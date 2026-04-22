# Spring Boot 学习笔记 09

## 1. 本节目标

这一节学习事务 `@Transactional` 的最核心价值：

1. 多个数据库操作要么都成功
2. 要么出错时全部回滚
3. 避免数据库留下半成品数据

## 2. 为什么需要事务

如果一个业务方法里有多条数据库操作：

```text
插入用户 A
插入用户 B
更新订单
扣减库存
```

如果前面成功，后面失败，就可能产生脏数据或半成品数据。

事务的核心作用就是保证：

```text
要么全部成功，要么全部失败
```

## 3. 本次事务实验

本次写了一个专门用于测试事务的接口：

```http
POST /users/transaction-test
```

Controller：

- [UserController.java](/Users/huanzichen/interview/SpringBoot/springboot-hello/src/main/java/com/huanzichen/springboothello/controller/UserController.java)

```java
@PostMapping("/transaction-test")
public Result<String> transactionTest() {
    userService.createTwoUsersForTest();
    return Result.success("success");
}
```

Service：

- [UserService.java](/Users/huanzichen/interview/SpringBoot/springboot-hello/src/main/java/com/huanzichen/springboothello/service/UserService.java)

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

这个方法故意做两件事：

1. 先插入一条用户数据
2. 再主动抛异常

目的就是观察第一条插入是否会回滚。

## 4. 没有事务时的现象

没有 `@Transactional` 时：

1. `userMapper.insert(user1)` 执行成功
2. 后面抛出异常
3. 接口返回错误
4. 但数据库里留下了 `transaction-user-1`

查询：

```http
GET /users/search?name=transaction-user-1
```

能查到：

```json
{
  "id": 7,
  "name": "transaction-user-1",
  "age": 18
}
```

说明：前面的插入没有因为后面的异常而撤销。

## 5. 加上 `@Transactional` 后的现象

加上：

```java
@Transactional
```

后，再执行：

```http
POST /users/transaction-test
```

仍然会返回错误：

```json
{
  "code": 400,
  "message": "test transaction rollback",
  "data": null
}
```

但是再查：

```http
GET /users/search?name=transaction-user-1
```

返回：

```json
{
  "code": 200,
  "message": "success",
  "data": []
}
```

说明插入被回滚了。

## 6. `@Transactional` 应该加在哪里

当前阶段先记住：

```text
@Transactional 通常加在 Service 方法上
```

不要优先加在 Controller 上。

原因：

1. Controller 负责接请求
2. Service 负责业务逻辑
3. 事务保护的是业务方法中的一组数据库操作

所以事务边界应该放在 Service 层更合理。

## 7. 默认回滚规则

当前阶段先记住最常用规则：

```text
运行时异常会触发回滚
```

例如：

```java
throw new BusinessException(ErrorCode.BAD_REQUEST, "test transaction rollback");
```

因为 `BusinessException` 继承自 `RuntimeException`，所以默认会触发事务回滚。

## 8. 当前要掌握的事务直觉

事务不是为了“让 SQL 能执行”，而是为了保证数据一致性。

你可以这样理解：

```text
没有事务：做一步算一步，前面成功就留下
有事务：整个方法是一组操作，任何关键步骤失败就整体撤销
```

## 9. 一个重要提醒

这次只是先理解 `@Transactional` 的价值。

后面还需要继续学习事务常见坑，例如：

1. 同一个类内部方法互相调用，事务可能不生效
2. 捕获异常后不继续抛出，事务可能不回滚
3. 只有 public 方法通常更适合做事务入口
4. 多数据源和事务传播行为

这些后面再学，现在先掌握最核心主线。

## 10. 这一节真正要掌握什么

1. 事务保护一组数据库操作
2. `@Transactional` 通常加在 Service 方法上
3. 运行时异常默认会触发回滚
4. 没事务时可能留下半成品数据
5. 有事务时失败会回滚之前的数据库操作
