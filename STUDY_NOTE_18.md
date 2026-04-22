# Study Note 18

## 主题

认证系统第二版：注册、用户名密码登录、BCrypt、最小 token 鉴权与任务权限控制

## 1. 这节课解决什么问题

前面项目已经做过一个最小登录链路：

- 通过 `userId` 模拟登录
- 登录成功后返回 token
- 通过拦截器保护 `/tasks/**`

但这种方案只能算“鉴权主链路演示”，还不够像真实项目。

所以这节课的重点是把认证系统往前推进一层，至少补齐这些能力：

1. 用户注册
2. 用户名密码登录
3. 密码加密存储
4. 当前用户接口
5. 任务接口登录保护
6. 基础数据权限控制

## 2. 为什么要先升级认证系统

在项目能力继续往上走时，单纯继续堆 CRUD 的收益已经开始下降。

真正更有分量的能力，是：

- 谁能登录系统
- 登录后怎么识别当前用户
- 当前用户能操作哪些数据

所以认证系统是项目从“练习项目”走向“更像真实后端项目”的关键分水岭。

## 3. 数据库层的变化

前面的 `users` 表只有：

- `id`
- `name`
- `age`

为了支持真实登录，这一节给 `users` 表补了：

- `username`
- `password`

同时：

- `username` 要唯一
- `password` 作为登录密码存储字段

这意味着用户模块开始承担认证系统的数据基础。

## 4. 注册接口

当前项目新增了：

```http
POST /auth/register
```

并引入了：

[RegisterDTO.java](/Users/huanzichen/interview/SpringBoot/springboot-hello/src/main/java/com/huanzichen/springboothello/dto/RegisterDTO.java)

字段包括：

- `username`
- `password`
- `name`
- `age`

同时在 DTO 层做了基本参数校验。

注册时的核心逻辑是：

1. 先按用户名查询是否已存在
2. 如果存在，返回：

```json
{
  "code": 400,
  "message": "username already exists",
  "data": null
}
```

3. 如果不存在，创建新用户并写入数据库

## 5. 用户名密码登录

当前项目把原来的“按 `userId` 模拟登录”升级成了：

```http
POST /auth/login
```

请求体使用：

[LoginDTO.java](/Users/huanzichen/interview/SpringBoot/springboot-hello/src/main/java/com/huanzichen/springboothello/dto/LoginDTO.java)

字段包括：

- `username`
- `password`

登录逻辑变成：

1. 根据 `username` 查用户
2. 校验密码
3. 成功后生成 token
4. 在内存 `tokenStore` 中保存：

```text
token -> userId
```

5. 返回 token 给客户端

## 6. 为什么密码不能明文存储

如果注册时直接把密码明文写入数据库，例如：

```text
123456
```

这在真实项目里是非常危险的。

因为：

- 一旦数据库泄露，用户密码直接暴露
- 也不符合基本的安全规范

所以这节课进一步引入了 BCrypt。

## 7. BCrypt 是怎么接进来的

项目里新增了依赖：

```xml
org.springframework.security:spring-security-crypto
```

这里没有直接引入整套 Spring Security 登录体系，只是先使用它提供的密码加密能力。

在 [AuthService.java](/Users/huanzichen/interview/SpringBoot/springboot-hello/src/main/java/com/huanzichen/springboothello/service/AuthService.java) 中，通过：

```java
private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
```

来完成密码处理。

### 注册时

不是直接存明文密码，而是：

```java
passwordEncoder.encode(registerDTO.getPassword())
```

### 登录时

不是直接用 `equals` 比较，而是：

```java
passwordEncoder.matches(loginDTO.getPassword(), userInfo.getPassword())
```

也就是：

- 前端永远传明文密码
- 数据库存加密后的 hash
- 后端负责比对

## 8. 一个很重要的排错点

这节课里还踩到了一个非常典型的坑：

登录失败时，一开始误以为是代码错了，但真正原因是：

**把数据库里的加密后密码直接当成登录密码传给了接口。**

这当然不会通过。

正确方式是：

- 注册时传明文密码
- 数据库存 hash
- 登录时仍然传明文密码
- 后端自己做 `matches(...)`

这一步非常值得记住，因为它能说明你已经开始理解“密码加密存储”和“密码校验”的区别。

## 9. 当前用户接口

当前项目新增了：

```http
GET /auth/me
```

它的作用是：

- 根据当前请求携带的 token
- 找到当前登录用户
- 返回当前用户信息

这一步把认证链路真正闭环了：

注册 -> 登录 -> 拿 token -> 带 token 请求 -> 识别当前用户

## 10. 为什么 `/auth/me` 不能直接返回 `UserInfo`

一开始如果直接返回 `UserInfo`，就会把：

- `username`
- `password`

都原样返回给前端。

其中 `password` 明显不应该暴露。

所以当前项目新增了：

[CurrentUserVO.java](/Users/huanzichen/interview/SpringBoot/springboot-hello/src/main/java/com/huanzichen/springboothello/model/CurrentUserVO.java)

只保留：

- `id`
- `username`
- `name`
- `age`

也就是说：

**即使 service 层拿到的是完整用户对象，返回给前端时也要做字段隔离。**

## 11. 拦截器和 token 鉴权

当前项目通过：

- `LoginInterceptor`
- `WebMvcConfig`
- `UserContext`

实现了一个最小 token 鉴权链路。

### 基本流程

1. 登录成功后生成 token
2. 客户端在请求头里带：

```text
Authorization: Bearer <token>
```

3. `LoginInterceptor` 校验 token
4. token 合法时，把当前用户 id 放进 `UserContext`
5. controller / service 可以从 `UserContext` 里拿当前用户

## 12. 任务接口的登录保护

当前项目把：

```text
/tasks/**
```

以及：

```text
/auth/me
```

纳入了拦截范围。

这意味着：

- 不带 token 访问任务接口，会返回：

```json
{
  "code": 401,
  "message": "please login first",
  "data": null
}
```

- 带合法 token 才能继续访问

## 13. 数据权限控制

这一节最有价值的不只是“能登录”，而是继续往前做了数据权限控制。

当前任务模块已经实现了：

- 登录用户只能查看自己的任务列表
- 登录用户只能查看自己的任务详情
- 登录用户只能更新自己的任务
- 登录用户只能删除自己的任务

如果访问别人的任务，会返回：

```json
{
  "code": 403,
  "message": "no permission",
  "data": null
}
```

这说明项目已经从“有没有登录”进入到了“登录后能操作什么数据”的层次。

## 14. 创建任务为什么不能再传 `userId`

一开始创建任务时，前端还需要在请求体里传：

```json
"userId": 3
```

这不够真实，因为前端不应该决定任务属于谁。

后面改造成了：

- `TaskCreateDTO` 不再接收 `userId`
- 后端从 `UserContext` 中直接读取当前登录用户 id
- 自动把任务归属到当前用户

这一步是权限模型真正收紧的重要标志。

## 15. 这节课真正要记住什么

### 结论 1

认证系统比单纯 CRUD 更能体现项目含金量。

### 结论 2

注册和登录是两条不同链路：

- 注册负责创建用户
- 登录负责校验身份并签发 token

### 结论 3

密码不能明文存储，应该用 BCrypt 这类方式加密后再写入数据库。

### 结论 4

登录校验只是第一层，数据权限控制才是更像真实业务系统的能力。

### 结论 5

创建任务时不再让前端传 `userId`，而是由后端根据当前登录用户自动决定数据归属，这样的权限模型更合理。

## 16. 当前进度定位

到这一步，你这个项目已经明显超过了“基础 Spring Boot 练习项目”的层级。

因为你现在不只是会：

- 写接口
- 连数据库
- 做分页

你已经开始具备：

- 认证系统
- token 鉴权
- 当前用户识别
- 数据权限控制
- 敏感字段隔离

这已经是更接近“实习项目”而不是“入门练习”的能力结构了。
