# Study Note 11

## 主题

`@Transactional` 的默认回滚规则与 `rollbackFor`

## 1. 这节课解决什么问题

很多初学者知道：

```java
@Transactional
```

能让方法发生异常时回滚。

但这句话不完整。

更准确的说法是：

事务是否回滚，不只看“有没有异常”，还要看“异常是什么类型”。

## 2. Spring 默认回滚规则

Spring 事务的默认规则可以先记成两条：

### 规则 1

遇到 `RuntimeException` 或 `Error`，默认回滚。

### 规则 2

遇到普通的 checked exception，也就是继承 `Exception` 但不是 `RuntimeException` 的异常，默认不回滚。

## 3. 当前项目里的两类异常

当前项目已经有一个业务异常：

[BusinessException.java](/Users/huanzichen/interview/SpringBoot/springboot-hello/src/main/java/com/huanzichen/springboothello/exception/BusinessException.java)

```java
public class BusinessException extends RuntimeException
```

它属于运行时异常，所以事务默认会回滚。

后面又新建了一个异常：

[CheckedBusinessException.java](/Users/huanzichen/interview/SpringBoot/springboot-hello/src/main/java/com/huanzichen/springboothello/exception/CheckedBusinessException.java)

```java
public class CheckedBusinessException extends Exception
```

它属于 checked exception。

## 4. 为什么 checked exception 要写 `throws`

因为它继承的是：

```java
Exception
```

这类异常是 Java 语言层面要求显式处理的异常。

所以方法里如果抛出它，要么：

1. `try-catch`
2. 要么在方法签名上写：

```java
throws CheckedBusinessException
```

这和事务本身无关，是 Java 编译器对 checked exception 的要求。

## 5. 第一轮实验：只有 `@Transactional`

当前项目在 [UserService.java](/Users/huanzichen/interview/SpringBoot/springboot-hello/src/main/java/com/huanzichen/springboothello/service/UserService.java) 里做了一个方法：

```java
@Transactional
public void createUserAndThrowCheckedBusinessException() throws CheckedBusinessException {
    UserInfo userInfo = new UserInfo();
    userInfo.setName("checked-exception-user");
    userInfo.setAge(20);
    userMapper.insert(userInfo);

    throw new CheckedBusinessException("checked exception test");
}
```

这个方法里：

1. 先插入一条数据库记录
2. 再抛出 checked exception

## 6. 为什么一开始容易误判

第一次只看接口结果时，会看到：

- 接口返回 `500`
- 查询接口结果有时和预期不一致

这时候很容易误判“是不是已经回滚了”。

但仅靠接口返回，不足以下结论。

原因：

- `500` 只说明异常没有被你的全局异常处理器接住
- 不能说明事务最终是提交还是回滚

## 7. 更可靠的判断方式：看事务日志

在 [application.yml](/Users/huanzichen/interview/SpringBoot/springboot-hello/src/main/resources/application.yml) 里打开事务日志后，可以直接看到事务管理器最后到底做了什么。

关键日志有两种：

### 提交

```text
Initiating transaction commit
```

### 回滚

```text
Initiating transaction rollback
```

这比只看接口返回更可靠。

## 8. 实验结论 1：checked exception 默认提交

当前项目的日志明确出现了：

```text
Initiating transaction commit
```

这说明：

- 虽然方法抛出了 `CheckedBusinessException`
- 但事务没有回滚
- 最终还是提交了

这和 Spring 的默认规则一致。

所以这里真正要记住的是：

**checked exception 默认不会触发事务回滚。**

## 9. 第二轮实验：加 `rollbackFor`

为了让 checked exception 也触发回滚，把方法改成：

```java
@Transactional(rollbackFor = Exception.class)
public void createUserAndThrowCheckedBusinessException() throws CheckedBusinessException
```

这里的意思是：

“只要抛出的异常类型属于 `Exception.class` 及其子类，就回滚。”

因为 `CheckedBusinessException extends Exception`，所以这次会触发回滚。

## 10. 实验结论 2：加上 `rollbackFor` 后回滚

修改后再次测试，日志会变成：

```text
Initiating transaction rollback
```

这说明：

- 同样是 checked exception
- 默认规则本来不会回滚
- 但你显式写了 `rollbackFor = Exception.class`
- 所以事务最终回滚了

## 11. 这节课真正要记住什么

### 结论 1

`RuntimeException` 默认回滚。

### 结论 2

checked exception 默认不回滚。

### 结论 3

如果希望 checked exception 也回滚，需要显式写：

```java
@Transactional(rollbackFor = Exception.class)
```

### 结论 4

判断事务最终是否提交或回滚，最可靠的证据是事务管理器日志，不是只看接口 `500`。

## 12. 当前阶段的实践建议

现阶段可以先用这条简单规则：

1. 日常业务异常优先继承 `RuntimeException`
2. 如果确实要抛 checked exception，就明确考虑要不要写 `rollbackFor`
3. 做事务实验时，优先看日志里的 `commit` / `rollback`

## 13. 这一节和前一节的关系

前一节学的是：

- 事务是不是生效
- 为什么同类内部调用会绕过代理

这一节学的是：

- 事务生效以后，到底按什么规则决定提交还是回滚

两节合起来，事务主线就更完整了：

1. 先看事务有没有真正生效
2. 再看生效后遇到什么异常会回滚
