# Spring Boot 学习笔记 07

## 1. 本节目标

这一节的目标是整理 Controller 拆分和路径设计：

1. 把练习接口和用户业务接口分开
2. 理解类级别 `@RequestMapping`
3. 避免路径重复
4. 记住结尾斜杠带来的路径匹配问题

## 2. 为什么要拆 Controller

之前所有接口都放在 `HelloController` 里：

- `/hello`
- `/greet`
- `/project-info`
- `/users`
- `/users/{id}`
- `/users/search`
- `/users/page`

这样能跑，但职责混乱。

现在拆成两个 Controller：

### `HelloController`

文件：

- [HelloController.java](/Users/huanzichen/interview/SpringBoot/springboot-hello/src/main/java/com/huanzichen/springboothello/controller/HelloController.java)

负责：

1. 练习接口
2. 配置读取接口

例如：

```java
@GetMapping("/hello")
public String hello() {
    return "hello spring boot";
}
```

### `UserController`

文件：

- [UserController.java](/Users/huanzichen/interview/SpringBoot/springboot-hello/src/main/java/com/huanzichen/springboothello/controller/UserController.java)

负责用户业务接口：

1. 创建用户
2. 查询用户
3. 更新用户
4. 删除用户
5. 条件查询
6. 分页查询

## 3. 类级别 `@RequestMapping`

当前 `UserController` 写法：

```java
@RestController
@RequestMapping("/users")
public class UserController {
}
```

含义：

1. 这个类下面的所有接口都以 `/users` 开头。
2. 方法上的路径只写剩余部分。

例如：

```java
@GetMapping
public Result<List<UserInfo>> listUsers() {
    return Result.success(userService.listUsers());
}
```

最终路径是：

```http
GET /users
```

再比如：

```java
@GetMapping("/search")
public Result<List<UserInfo>> searchUsers(String name) {
    return Result.success(userService.searchUsers(name));
}
```

最终路径是：

```http
GET /users/search
```

## 4. 路径重复问题

如果类上已经写了：

```java
@RequestMapping("/users")
```

方法上就不要再写：

```java
@GetMapping("/users")
```

否则最终路径会变成：

```http
GET /users/users
```

这是这次踩到的一个重点坑。

正确写法：

```java
@RequestMapping("/users")
public class UserController {

    @GetMapping
    public Result<List<UserInfo>> listUsers() {
        return Result.success(userService.listUsers());
    }
}
```

## 5. 当前用户接口路径

当前 `UserController` 的接口应该这样访问：

### 创建用户

```http
POST /users
```

### 查询列表

```http
GET /users
```

### 条件查询

```http
GET /users/search?name=huan
```

### 分页查询

```http
GET /users/page?page=1&size=2
```

### 根据 id 查询

```http
GET /users/2
```

### 更新用户

```http
PUT /users/2
```

### 删除用户

```http
DELETE /users/2
```

## 6. 固定路径和动态路径

当前有这些路径：

```java
@GetMapping("/search")
@GetMapping("/page")
@GetMapping("/{id}")
```

其中：

- `/search` 是固定路径
- `/page` 是固定路径
- `/{id}` 是动态路径

为了更清晰，建议固定路径写在动态路径前面：

```java
@GetMapping("/search")
@GetMapping("/page")
@GetMapping("/{id}")
```

这样阅读时更不容易混乱。

## 7. 结尾斜杠问题

当前接口是：

```http
GET /users
```

不要写成：

```http
GET /users/
```

在当前项目配置下，结尾多一个 `/` 可能导致 `404`。

这次反复出现的问题就是：

```http
/users/
```

和：

```http
/users
```

不是同一个请求路径。

## 8. 404 和 405 的区别

### 404 Not Found

常见原因：

1. 路径写错
2. 多了或少了 `/`
3. 类级别路径和方法路径叠加错了
4. 项目没重启，运行的还是旧版本

### 405 Method Not Allowed

常见原因：

1. 路径对了
2. HTTP 方法错了

例如接口是：

```java
@DeleteMapping("/users/{id}")
```

但你发了：

```http
GET /users/2
```

就可能出现 405。

## 9. 这一节真正要掌握什么

1. 一个 Controller 不应该无限堆接口。
2. 业务接口应该按模块拆分到对应 Controller。
3. `@RequestMapping("/users")` 是公共前缀。
4. 方法路径不要重复写公共前缀。
5. 请求路径要严格匹配，不要随手多加结尾 `/`。
6. 看到 404，先查路径；看到 405，先查 HTTP 方法。

## 10. 下一步学习方向

下一步建议继续优化异常体系：

1. 给业务异常增加错误码
2. 区分参数错误、资源不存在、系统错误
3. 让 `Result` 的 `code` 不再永远只有 `200` 和 `400`
