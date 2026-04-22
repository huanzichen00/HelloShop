# Study Note 21

## 主题

当前用户主线：`/auth/me`、`PUT /users/me`、`GET /users/me/tasks`

## 1. 这节课解决什么问题

前面的项目已经有了：

- 注册
- 用户名密码登录
- JWT 鉴权
- Redis 黑名单 logout
- 任务权限控制

但这还更偏“认证系统”，不够像真实业务系统里的“当前用户中心”。

所以这节课重点做的是：

- 查询当前登录用户
- 修改当前登录用户资料
- 查询当前登录用户自己的任务列表

也就是把“当前用户是谁”和“当前用户能操作什么数据”真正串起来。

## 2. 为什么 `/users/me` 比 `/users/{id}` 更像真实项目

如果让前端每次都传：

```text
/users/{id}
```

再去改资料，那么本质上还是“按 id 操作任意用户”。

但很多真实系统里更常见的是：

- 查看当前登录用户信息
- 修改当前登录用户资料
- 查看当前登录用户自己的资源

这类接口不应该依赖前端传 id，而应该由后端根据登录态直接决定“当前用户是谁”。

所以：

```text
PUT /users/me
```

和：

```text
GET /users/me/tasks
```

会比单纯的 `/users/{id}` 更接近真实业务语义。

## 3. `/users/me` 是怎么做的

### 1. 接口受登录保护

在 [WebMvcConfig.java](/Users/huanzichen/interview/SpringBoot/springboot-hello/src/main/java/com/huanzichen/springboothello/config/WebMvcConfig.java) 里，把：

```text
/users/me
```

加入了拦截范围。

所以未登录访问会直接返回：

```json
{
  "code": 401,
  "message": "please login first",
  "data": null
}
```

### 2. 当前用户 id 不再从路径拿

`UserService` 里是通过：

```java
UserContext.getCurrentUserId()
```

拿当前登录用户 id，而不是靠前端传路径变量。

### 3. 只允许修改自己的资料

这样接口天然就是“当前登录用户修改自己”，不会出现“前端传别人的 id 去改别人资料”的问题。

## 4. 为什么 `PUT /users/me` 不应该返回 `UserInfo`

一开始最容易犯的错误是：

直接把 `UserInfo` 返回出去。

这样会把：

```text
password
```

也一起暴露给前端。

虽然现在数据库里存的已经不是明文，而是 BCrypt hash，但这仍然不应该出现在接口响应里。

所以这一步复用了：

[CurrentUserVO.java](/Users/huanzichen/interview/SpringBoot/springboot-hello/src/main/java/com/huanzichen/springboothello/model/CurrentUserVO.java)

它只保留：

- `id`
- `username`
- `name`
- `age`

这体现的是一个很重要的接口设计原则：

**实体对象不应该直接对外暴露，尤其是包含敏感字段时。**

## 5. `/users/me/tasks` 为什么有价值

前面项目里已经做过：

```text
GET /users/{id}/tasks
```

这条接口更多体现的是“模块关联”。

而：

```text
GET /users/me/tasks
```

体现的是“当前用户视角”。

它不再要求前端传 user id，而是直接根据当前登录用户返回自己的任务列表。

这和：

- `/auth/me`
- `PUT /users/me`

放在一起，就形成了比较完整的当前用户主线。

## 6. 这节课踩到的典型问题

### 1. `/users/me` 被 `/users/{id}` 吃掉

如果 controller 里写了：

```java
@GetMapping("/{id}")
```

或者：

```java
@PutMapping("/{id}")
```

而没有加数字约束，那么：

```text
/users/me
```

就可能被 Spring 当成：

```text
id = "me"
```

然后因为转 `Long` 失败，返回 `400`。

所以这里把动态路径收成了：

```java
/{id:\\d+}
```

这也是更稳的写法。

### 2. `/users/me/tasks` 不该再接收 `id` 参数

既然是“当前用户自己的任务列表”，就不应该再写：

```java
@RequestParam("id") Long id
```

否则会变成“接口语义是 me，但实现上还在等前端传 id”，这会把设计搞乱。

## 7. 当前用户主线现在已经成型

现在项目里这几条接口已经能连成一条完整链路：

### 认证

- `POST /auth/register`
- `POST /auth/login`
- `GET /auth/me`
- `POST /auth/logout`

### 当前用户能力

- `PUT /users/me`
- `GET /users/me/tasks`

### 当前用户任务能力

- `POST /tasks`
- `GET /tasks`
- `GET /tasks/{id}`
- `PUT /tasks/{id}`
- `DELETE /tasks/{id}`

这些接口共同体现的是：

- 当前用户是谁
- 当前用户能改什么
- 当前用户能看到什么

## 8. 这一节的价值

这节课的价值不在于多写了两个接口，而在于：

### 1. 项目开始更像“用户中心”

而不只是“数据库实体的 CRUD”。

### 2. 权限模型更统一

系统开始稳定地围绕：

```text
当前登录用户
```

来组织接口，而不是一会儿靠 id，一会儿靠 token。

### 3. 对实习项目更有帮助

因为这类“当前用户资料维护”和“当前用户资源列表”比单纯 CRUD 更接近真实业务系统，也更适合在面试里讲项目设计思路。
