# Study Note 10

## 主题

`@Transactional` 的生效条件与同类内部调用失效

## 1. 这节课解决什么问题

前一节已经验证过：

- 给 `UserService#createTwoUsersForTest()` 加上 `@Transactional` 以后
- 方法中先插入一条用户，再抛出 `BusinessException`
- 数据库中的第一条插入会被回滚

这说明事务已经生效。

但很多初学者会进一步误以为：

“只要一个方法上写了 `@Transactional`，无论怎么调用，它都会生效。”

这不对。

Spring 事务通常是通过 **代理对象** 生效的，不是直接改写你原来的方法体。

## 2. 生效模型

先记住这一条链路：

外部调用 -> Spring 代理对象 -> 事务逻辑 -> 目标方法

也就是说：

- `@Transactional` 生效的前提之一，是这次方法调用经过了 Spring 代理
- 如果没有经过代理，事务逻辑就不会被织入

## 3. 当前项目里的事务方法

当前项目在 [UserService.java](/Users/huanzichen/interview/SpringBoot/springboot-hello/src/main/java/com/huanzichen/springboothello/service/UserService.java) 里有这样一个方法：

```java
@Transactional
public void createTwoUsersForTest() {
    UserInfo user1 = new UserInfo();
    user1.setName("transaction-user-1");
    user1.setAge(18);
    userMapper.insert(user1);

    throw new BusinessException(ErrorCode.BAD_REQUEST, "test transaction rollback");
}
```

这个方法的行为是：

1. 先插入一条用户
2. 再抛出一个运行时异常

如果事务生效，插入应该回滚。

## 4. 外部调用时为什么能回滚

当前项目在 [UserController.java](/Users/huanzichen/interview/SpringBoot/springboot-hello/src/main/java/com/huanzichen/springboothello/controller/UserController.java) 里有测试接口：

```java
@PostMapping("/transaction-test")
public Result<String> transactionTest() {
    userService.createTwoUsersForTest();
    return Result.success("success");
}
```

当请求：

```http
POST /users/transaction-test
```

时，调用路径是：

Controller -> Spring 容器中的 `userService` Bean -> 代理 -> `createTwoUsersForTest()`

因为经过了 Spring 管理的代理对象，所以事务生效，抛异常后数据库会回滚。

## 5. 同类内部调用为什么会失效

当前项目又加了一个方法：

```java
public void callTransactionMethodInsideTheSameClass() {
    createTwoUsersForTest();
}
```

以及对应接口：

```java
@PostMapping("/transaction-self-call-test")
public Result<String> transactionSelfCallTest() {
    userService.callTransactionMethodInsideTheSameClass();
    return Result.success("success");
}
```

注意关键点：

- Controller 调用的是 `callTransactionMethodInsideTheSameClass()`
- 这个方法本身没有事务
- 它在 `UserService` 内部又直接调用了同类里的 `createTwoUsersForTest()`

这一步是“类内部直接方法调用”，本质上等价于：

```java
this.createTwoUsersForTest();
```

它没有再经过 Spring 代理。

所以执行路径变成了：

Controller -> Spring 代理对象 -> `callTransactionMethodInsideTheSameClass()` -> 直接调用目标方法

后半段绕过了代理，`@Transactional` 失效。

## 6. 实验结果

请求：

```http
POST /users/transaction-self-call-test
```

返回：

```json
{
  "code": 400,
  "message": "test transaction rollback",
  "data": null
}
```

再查询：

```http
GET /users/search?name=transaction-user-1
```

返回中仍然查到了数据库记录。

这说明：

- 异常确实抛了
- 但插入没有回滚
- 原因不是 SQL 写错
- 原因是事务没有生效

## 7. 这节课真正要记住什么

### 规则 1

`@Transactional` 依赖 Spring 代理生效。

### 规则 2

外部通过 Spring Bean 调用事务方法，事务通常能生效。

### 规则 3

同一个类内部直接调用另一个带 `@Transactional` 的方法，事务可能失效。

### 规则 4

不要把“方法上写了注解”和“运行时一定生效”混为一谈。

## 8. 当前阶段的实践建议

现阶段先记住这条工程习惯：

- 事务方法通常写在 `Service` 层
- 由 `Controller` 或其他 Bean 从外部调用
- 不要在同一个类里靠内部直接调用去触发事务

后面如果需要拆更复杂的事务边界，再继续学习更深入的代理机制和 AOP。

## 9. 额外排错点

这次实验里还踩到一个非事务问题：

如果直接在浏览器地址栏访问：

```text
/users/transaction-self-call-test
```

浏览器默认发的是 `GET` 请求，不是 `POST`。

而当前接口定义是：

```java
@PostMapping("/transaction-self-call-test")
```

所以必须用 HTTP Client 或 Postman 发 `POST`，不能直接用地址栏测试。

否则 Spring 可能会去匹配：

```java
@GetMapping("/{id}")
```

再把 `"transaction-self-call-test"` 当作 `id` 尝试转换成 `Long`，从而报参数类型转换错误。

## 10. 当前进度定位

到这一步，你已经不只是会“给方法加一个 `@Transactional`”。

你已经开始理解：

- 事务为什么能生效
- 事务为什么会失效
- Spring 注解很多时候依赖运行时机制，而不是只看代码表面

这类理解对后面学习 `AOP`、`IOC`、事务传播行为都会有帮助。
