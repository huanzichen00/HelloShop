# Spring Boot 学习笔记 05

## 1. 本节目标

这一节把手写参数校验升级成 Spring Boot 常用的标准校验方式：

1. 引入 validation 依赖
2. 在 DTO 字段上声明校验规则
3. 在 Controller 入参处使用 `@Valid`
4. 用全局异常处理器统一返回校验错误
5. 清理 Service 里重复的手写校验

## 2. 为什么要从手写 if 切到标准校验

昨天的校验写在 `UserService` 里：

```java
if (name == null || name.trim().isEmpty()) {
    throw new RuntimeException("name cannot be blank");
}

if (age == null || age < 0) {
    throw new RuntimeException("age cannot be negative");
}
```

这种方式能用，但有两个问题：

1. 创建和更新都要写类似逻辑，容易重复。
2. 字段规则分散在 Service 里，DTO 本身看不出要求。

现在改成标准校验后：

- DTO 描述“入参应该长什么样”
- Controller 负责触发校验
- 全局异常处理器负责统一错误返回
- Service 更专注业务逻辑

## 3. validation 依赖

在 `pom.xml` 中加入：

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-validation</artifactId>
</dependency>
```

加入后才可以使用：

- `@Valid`
- `@NotBlank`
- `@NotNull`
- `@Min`

## 4. DTO 上的校验规则

当前创建用户 DTO：

- [UserCreateDTO.java](/Users/huanzichen/interview/SpringBoot/springboot-hello/src/main/java/com/huanzichen/springboothello/dto/UserCreateDTO.java)

核心写法：

```java
@NotBlank(message = "name cannot be blank")
private String name;

@NotNull(message = "age cannot be null")
@Min(value = 0, message = "age cannot be negative")
private Integer age;
```

含义：

1. `@NotBlank`：字符串不能是 `null`、空字符串、纯空格
2. `@NotNull`：字段不能是 `null`
3. `@Min(0)`：数字不能小于 0

为什么 `age` 要同时写 `@NotNull` 和 `@Min`：

- `@Min` 只负责最小值判断
- `age` 不传时要用 `@NotNull` 单独判断

## 5. Controller 里的 `@Valid`

当前 Controller 写法：

- [HelloController.java](/Users/huanzichen/interview/SpringBoot/springboot-hello/src/main/java/com/huanzichen/springboothello/controller/HelloController.java)

```java
@PostMapping("/users")
public Result<UserInfo> createUser(@RequestBody @Valid UserCreateDTO userCreateDTO) {
    return Result.success(userService.createUser(userCreateDTO));
}
```

```java
@PutMapping("/users/{id}")
public Result<UserInfo> updateUserById(@PathVariable Long id, @RequestBody @Valid UserUpdateDTO userUpdateDTO) {
    return Result.success(userService.updateUser(id, userUpdateDTO));
}
```

理解：

1. `@RequestBody`：把请求体 JSON 转成 Java 对象
2. `@Valid`：触发 DTO 上的校验规则
3. 如果校验失败，Controller 方法不会继续执行

也就是说，如果 `age = -1`，不会进入 `userService.updateUser(...)`。

## 6. 校验失败时的异常

`@Valid` 校验失败时，Spring 会抛出：

```java
MethodArgumentNotValidException
```

如果不处理它，Spring 会返回默认错误 JSON：

```json
{
  "timestamp": "...",
  "status": 400,
  "error": "Bad Request",
  "path": "/users/2"
}
```

这不符合我们项目里的统一返回结构。

## 7. 全局处理校验异常

当前全局异常处理器：

- [GlobalExceptionHandler.java](/Users/huanzichen/interview/SpringBoot/springboot-hello/src/main/java/com/huanzichen/springboothello/exception/GlobalExceptionHandler.java)

新增处理方法：

```java
@ExceptionHandler(MethodArgumentNotValidException.class)
public Result<String> handleValidationException(MethodArgumentNotValidException e) {
    String message = e.getBindingResult()
            .getFieldErrors()
            .get(0)
            .getDefaultMessage();

    return Result.error(message);
}
```

理解：

1. `getBindingResult()` 拿到校验结果
2. `getFieldErrors()` 拿到字段错误列表
3. `get(0)` 暂时只取第一个错误
4. `getDefaultMessage()` 取 DTO 注解里写的 `message`

最终返回：

```json
{
  "code": 400,
  "message": "age cannot be negative",
  "data": null
}
```

## 8. Service 清理后的职责

当前 `UserService` 不再手写参数格式校验。

- [UserService.java](/Users/huanzichen/interview/SpringBoot/springboot-hello/src/main/java/com/huanzichen/springboothello/service/UserService.java)

它现在更专注：

1. 组装 `UserInfo`
2. 调用 `UserMapper`
3. 处理业务判断，比如 `user not found`

注意：

- `name` 不能为空、`age` 不能为负数：这是参数格式校验，放 DTO
- 用户不存在：这是业务判断，放 Service

## 9. 当前请求示例

### 创建用户校验失败

```http
POST http://localhost:8081/users
Content-Type: application/json

{
  "name": "",
  "age": 18
}
```

返回：

```json
{
  "code": 400,
  "message": "name cannot be blank",
  "data": null
}
```

### 更新用户校验失败

```http
PUT http://localhost:8081/users/2
Content-Type: application/json

{
  "name": "test",
  "age": -1
}
```

返回：

```json
{
  "code": 400,
  "message": "age cannot be negative",
  "data": null
}
```

## 10. 这一节真正要掌握什么

1. DTO 不只是“装数据”，还可以声明入参规则。
2. `@Valid` 是触发校验的开关。
3. 校验失败不会进入 Service。
4. 校验异常也应该纳入统一返回结构。
5. Service 应该保留业务判断，不应该堆重复的字段格式校验。

## 11. 下一步学习方向

下一步建议继续学习：

1. 分页查询
2. 条件查询
3. 更规范的异常类，比如自定义 `BusinessException`
