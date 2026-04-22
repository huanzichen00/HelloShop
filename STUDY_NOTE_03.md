# Spring Boot 学习笔记 03

## 1. 本节目标

这一节的重点不是新增很多接口，而是把项目逐步写得更像真实后端：

1. 成功返回和失败返回统一格式
2. 非法参数不再只看到默认 `500`
3. 配置不再只靠零散的 `@Value`
4. 开始建立 `DTO / VO` 的直觉

## 2. 统一返回结构

当前项目已经有了统一返回类：

- [Result.java](/Users/huanzichen/interview/SpringBoot/springboot-hello/src/main/java/com/huanzichen/springboothello/common/Result.java)

核心结构是：

```java
public class Result<T> {
    private Integer code;
    private String message;
    private T data;
}
```

当前理解：

1. `code`：状态码语义
2. `message`：提示信息
3. `data`：真正的数据

这样做的好处：

1. 前端更容易统一处理接口结果
2. 成功和失败的格式更一致
3. 后面扩展字段会更方便

### 当前项目中的成功返回

例如创建用户：

```java
@PostMapping("/users")
public Result<UserInfo> createUser(@RequestBody UserInfo userInfo) {
    return Result.success(userService.createUser(userInfo));
}
```

成功返回示例：

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "name": "huan",
    "age": 10
  }
}
```

## 3. 参数校验的第一版

当前项目里，最初的参数校验是直接写在 `UserService` 里：

- [UserService.java](/Users/huanzichen/interview/SpringBoot/springboot-hello/src/main/java/com/huanzichen/springboothello/service/UserService.java)

核心逻辑：

```java
if (userInfo.getName() == null || userInfo.getName().trim().isEmpty()) {
    throw new RuntimeException("name cannot be blank");
}

if (userInfo.getAge() == null || userInfo.getAge() < 0) {
    throw new RuntimeException("age cannot be negative");
}
```

当前阶段这样写是合理的，因为重点是先学会：

1. 发现非法输入
2. 主动阻止非法数据继续进入业务逻辑

你现在要记住：

- 参数校验不是“高级优化”，而是接口开发的基本动作。

## 4. 全局异常处理

当前项目已经有全局异常处理类：

- [GlobalExceptionHandler.java](/Users/huanzichen/interview/SpringBoot/springboot-hello/src/main/java/com/huanzichen/springboothello/exception/GlobalExceptionHandler.java)

核心注解：

```java
@RestControllerAdvice
```

作用：

1. 统一捕获异常
2. 统一组织错误返回
3. 避免每个接口都手写一套 try-catch

当前处理的是：

```java
@ExceptionHandler(RuntimeException.class)
```

也就是说，当业务里抛出 `RuntimeException` 时，会走这里。

当前失败返回示例：

```json
{
  "code": 400,
  "message": "age cannot be negative",
  "data": null
}
```

这比默认的：

```json
{
  "timestamp": "...",
  "status": 500,
  "error": "Internal Server Error",
  "path": "/users"
}
```

更适合前后端协作。

## 5. `properties` 到 `yml`

当前项目已经从 `application.properties` 切换到了：

- [application.yml](/Users/huanzichen/interview/SpringBoot/springboot-hello/src/main/resources/application.yml)

当前内容类似：

```yml
server:
  port: 8081

project:
  name: springboot-hello
  version: 0.0.1
  author: huanzichen
```

理解：

1. `.properties` 更像一行一行的 `key=value`
2. `.yml` 更适合表示层级结构
3. 配置一多时，`yml` 一般更清晰

要注意：

- `yml` 对缩进敏感
- 少空格、多缩进都会出问题

## 6. `@Value` 和 `@ConfigurationProperties`

### `@Value`

之前已经学过：

```java
@Value("${project.name}")
private String projectName;
```

它适合：

- 读取单个简单配置

### `@ConfigurationProperties`

当前项目进一步用了配置类：

- [ProjectProperties.java](/Users/huanzichen/interview/SpringBoot/springboot-hello/src/main/java/com/huanzichen/springboothello/config/ProjectProperties.java)

```java
@Component
@ConfigurationProperties(prefix = "project")
public class ProjectProperties {
    private String name;
    private String version;
    private String author;
}
```

理解：

1. 它适合读取一组相关配置
2. 不用在 Controller 里写多个 `@Value`
3. 配置类本身更清晰、更容易维护

当前 Controller 已经这样使用：

- [HelloController.java](/Users/huanzichen/interview/SpringBoot/springboot-hello/src/main/java/com/huanzichen/springboothello/controller/HelloController.java)

```java
private final ProjectProperties projectProperties;
```

并通过接口返回：

```java
@GetMapping("/project-info")
public ProjectProperties getProjectInfo() {
    return projectProperties;
}
```

## 7. Controller 和 Service 的职责

当前项目已经有基本分层：

- `controller`
- `service`

你现在要进一步强化这个认识：

### Controller

负责：

1. 接收请求
2. 接收参数
3. 返回响应

### Service

负责：

1. 处理业务逻辑
2. 做最初步的参数合法性判断
3. 组织内部处理流程

当前链路已经可以这样理解：

`请求 -> Controller -> Service -> 返回统一结果`

后面接数据库后会继续变成：

`请求 -> Controller -> Service -> Mapper -> 数据库`

## 8. DTO / VO 的最初直觉

当前项目里 `UserInfo` 同时承担了：

1. 接收前端传参
2. 作为内部数据结构
3. 返回给前端

小项目里这样做可以，但你已经要开始建立一个更真实的意识：

### DTO

更偏“接收前端传来的数据”

例如：

- `UserCreateDTO`

### VO

更偏“返回给前端看的数据”

例如：

- `UserVO`

为什么以后要拆：

1. 请求字段和返回字段常常不一样
2. 内部对象不一定适合直接暴露给前端
3. 不同接口可能需要不同的返回结构

当前阶段你不用马上大规模重构，但要先建立这个直觉。

## 9. 当前代码里仍然比较粗糙的点

这些不是错误，但后面会继续优化：

1. `UserInfo` 还同时承担多种角色
2. `UserService` 里现在还是内存 `List`
3. 校验还是手写 `if`
4. `GlobalExceptionHandler` 还比较简陋
5. `GET /users` 目前还没统一包成 `Result<List<UserInfo>>`

这很正常，因为当前阶段是在先打主线。

## 10. 到目前为止你已经掌握了什么

你已经能独立完成这些基础动作：

1. 创建 Spring Boot 项目
2. 理解 `pom.xml`
3. 编写 `GET` / `POST` 接口
4. 接收查询参数、路径参数、JSON 请求体
5. 返回字符串、对象、列表、统一结果
6. 使用 `controller + service` 分层
7. 实现简单参数校验
8. 实现全局异常处理
9. 使用 `application.yml`
10. 读取单个配置和一组配置

## 11. 下一步学习方向

接下来最自然的方向有两个：

1. 引入 `DTO`，把 `POST /users` 的入参从 `UserInfo` 拆开
2. 开始接数据库，进入 `MySQL + MyBatis`

当前更建议先做第 1 个，让结构意识更稳，再进入数据库。
