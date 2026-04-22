# Study Note 19

## 主题

JWT 鉴权第一版与 Redis 黑名单版 logout

## 1. 这节课解决什么问题

前一节已经做出了：

- 注册
- 用户名密码登录
- BCrypt 密码加密
- `POST /auth/login`
- `GET /auth/me`
- `/tasks/**` 登录保护
- 任务数据权限控制

但当认证系统从“内存 token 表”升级到 JWT 后，很快会遇到一个关键问题：

**logout 之后，token 依然还能继续使用。**

这不是 bug，而是 JWT 天然无状态带来的结果。

所以这节课重点解决两个问题：

1. 用 JWT 取代原来的随机 UUID + 内存 `tokenStore`
2. 用 Redis 黑名单让 logout 真正生效

## 2. 为什么要从 UUID + 内存 Map 升级到 JWT

最开始的最小方案是：

- 登录成功后生成一个 UUID
- 在服务端内存里保存：

```text
token -> userId
```

这种方式能快速把“登录 -> token -> 拦截器 -> 当前用户”这条链路跑通，但它有明显局限：

- token 完全依赖服务端内存
- 更像演示方案
- 不够接近真实后端项目常见做法

所以后面升级成了 JWT。

## 3. JWT 第一版做了什么

当前项目新增了：

[JwtUtil.java](/Users/huanzichen/interview/SpringBoot/springboot-hello/src/main/java/com/huanzichen/springboothello/common/JwtUtil.java)

它负责两个核心动作：

### 1. 生成 token

```java
generateToken(Long userId)
```

### 2. 从 token 里解析用户 id

```java
parseUserId(String token)
```

当前项目里把 `userId` 放在 JWT 的 `subject` 里。

也就是说，JWT 本身就携带了“当前登录用户是谁”的信息。

## 4. 登录逻辑怎么变化了

升级成 JWT 后，登录成功时不再：

- 生成 UUID
- 往 `tokenStore` 里写数据

而是直接：

```java
return JwtUtil.generateToken(userInfo.getId());
```

这样客户端拿到的就是标准 JWT，形态类似：

```text
xxxxx.yyyyy.zzzzz
```

这说明项目的鉴权方式已经从“服务端保存会话状态”转成了“客户端持有 JWT，服务端按 JWT 校验”。

## 5. 拦截器怎么变化了

升级成 JWT 后，[LoginInterceptor.java](/Users/huanzichen/interview/SpringBoot/springboot-hello/src/main/java/com/huanzichen/springboothello/interceptor/LoginInterceptor.java) 的核心逻辑也发生了变化。

原来是：

- 取 token
- 去 `tokenStore` 里查

现在变成：

1. 从请求头读取：

```text
Authorization: Bearer <token>
```

2. 取出 token
3. 用 `JwtUtil.parseUserId(token)` 解析用户 id
4. 把用户 id 放进 `UserContext`

也就是说：

**服务端不再依赖内存里的 token 映射，而是直接信任合法 JWT。**

## 6. 为什么 logout 失效了

升级到 JWT 后，一个现象会立刻出现：

- 调了 `POST /auth/logout`
- 但同一个 token 仍然还能访问受保护接口

这不是实现错了，而是 JWT 的天然特点：

### JWT 是无状态的

只要：

- 签名正确
- 没过期

服务端就能解析出用户 id。

所以：

即使你把原来的 `tokenStore` 记录删掉了，也没有意义，因为拦截器已经不再查这个 `Map`。

## 7. 为什么要引入 Redis

为了让“已经签发出去的 JWT”在 logout 后失效，需要额外状态。

这时候 Redis 就正好适合做：

**token 黑名单**

也就是：

- logout 时，把 token 放进 Redis
- 拦截器校验 token 时，先查 Redis
- 如果 token 在黑名单里，直接判定无效

这样就能在 JWT 无状态的前提下，实现“主动失效 token”。

## 8. Redis 黑名单怎么实现

当前项目在 [AuthService.java](/Users/huanzichen/interview/SpringBoot/springboot-hello/src/main/java/com/huanzichen/springboothello/service/AuthService.java) 中：

### 注入了

```java
StringRedisTemplate
```

### 定义了 key 前缀

```java
token:blacklist:
```

### logout 时执行

```java
stringRedisTemplate.opsForValue().set(
    TOKEN_BLACKLIST_PREFIX + token,
    "1",
    24,
    TimeUnit.HOURS
);
```

这表示：

- 把当前 token 放进 Redis
- 值随便写 `"1"` 就够
- 过期时间和 JWT 当前有效期保持一致

## 9. 为什么过期时间要和 JWT 一致

因为黑名单本身不需要永久保存。

只要 token 自己过期了，这条黑名单记录也就失去意义了。

所以：

- JWT 有效期 24 小时
- Redis 黑名单 TTL 也设 24 小时

这样 Redis 不会无限堆积无效 token。

## 10. 拦截器怎么接黑名单

当前项目在 [LoginInterceptor.java](/Users/huanzichen/interview/SpringBoot/springboot-hello/src/main/java/com/huanzichen/springboothello/interceptor/LoginInterceptor.java) 中先做：

```java
if (authService.isBlacklisted(token)) {
    throw new BusinessException(ErrorCode.UNAUTHORIZED, "invalid token");
}
```

然后才继续解析 JWT。

也就是说，校验顺序变成：

1. 请求头格式是否合法
2. token 是否在 Redis 黑名单里
3. token 本身能不能被 JWT 正常解析

这三步都通过，才会放行。

## 11. 当前 logout 链路终于闭环了

现在 logout 的行为已经不是“看起来登出了”，而是真的能使 token 失效：

1. 登录拿到 JWT
2. 访问 `/auth/me` 正常
3. 调 `POST /auth/logout`
4. token 被写入 Redis 黑名单
5. 再访问 `/auth/me`
6. 返回：

```json
{
  "code": 401,
  "message": "invalid token",
  "data": null
}
```

这说明 logout 已经真正生效。

## 12. 这节课真正要记住什么

### 结论 1

JWT 比“随机 UUID + 内存 Map”更接近真实项目的鉴权方式。

### 结论 2

JWT 是无状态的，所以单纯删除服务端内存记录，不能让已签发的 token 失效。

### 结论 3

如果希望 logout 后 JWT 立刻失效，需要额外状态，例如 Redis 黑名单。

### 结论 4

拦截器不只要校验 token 格式和签名，还可以接入 Redis 黑名单做更强控制。

## 13. 当前进度定位

到这一步，你的认证系统已经不是“最小演示版”了，而是明显更像真实项目的版本：

- 注册
- 用户名密码登录
- BCrypt
- JWT
- 当前用户接口
- 登出
- Redis 黑名单
- 数据权限控制

这已经是一个非常适合写进项目亮点和面试讲解中的部分。
