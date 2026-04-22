# Study Note 20

## 主题

任务状态流转与完成时间 `completedAt`

## 1. 这节课解决什么问题

前面的 `tasks` 模块已经有了：

- 任务创建、查询、更新、删除
- 登录保护
- 数据权限控制
- 创建任务自动绑定当前登录用户

但它本质上还是一个“表单 CRUD”模块。

这节课要解决的问题是：

**让任务模块开始具备明确业务规则，而不只是把字段写进数据库。**

当前新增的核心规则是：

- `TODO` 任务不应该有完成时间
- `DONE` 任务应该自动记录完成时间
- 任务从 `DONE` 改回 `TODO` 时，要清空完成时间

## 2. 为什么要加 `completedAt`

如果任务只有：

- `title`
- `description`
- `status`

那它虽然能跑，但还是偏“练接口”。

补上 `completedAt` 以后，任务就开始更像真实业务对象：

- 有状态
- 有状态变化的结果
- 有由业务规则自动维护的字段

这一步的意义不只是多加一个字段，而是：

**开始把任务模块从 CRUD 推向业务模块。**

## 3. 数据库层做了什么

给 `tasks` 表新增了：

```sql
completed_at datetime null
```

这里允许为空，因为：

- `TODO` 任务本来就不该有完成时间
- 只有完成态任务才需要这个字段

## 4. 模型层怎么变化

[TaskInfo.java](/Users/huanzichen/interview/SpringBoot/springboot-hello/src/main/java/com/huanzichen/springboothello/model/TaskInfo.java) 新增了：

```java
private LocalDateTime completedAt;
```

这样任务对象本身就能表达：

- 当前状态
- 是否已完成
- 完成时间是什么时候

## 5. Mapper 层怎么变化

[TaskMapper.java](/Users/huanzichen/interview/SpringBoot/springboot-hello/src/main/java/com/huanzichen/springboothello/mapper/TaskMapper.java) 和 [TaskMapper.xml](/Users/huanzichen/interview/SpringBoot/springboot-hello/src/main/resources/mapper/TaskMapper.xml) 都补上了：

```sql
completed_at as completedAt
```

并且：

- `insert` 会写入 `completed_at`
- `update` 也会更新 `completed_at`

这一步的关键是让数据库字段和 Java 对象保持一致。

## 6. 为什么更新任务时不再允许改 `userId`

前面项目已经把任务权限收成了：

- 创建任务时，`userId` 由后端从 `UserContext` 里拿
- 查询、更新、删除都要校验是否属于当前用户

在这个前提下，如果更新任务接口还允许前端传 `userId`，那就不合理了。

所以这节课顺手把 [TaskUpdateDTO.java](/Users/huanzichen/interview/SpringBoot/springboot-hello/src/main/java/com/huanzichen/springboothello/dto/TaskUpdateDTO.java) 里的 `userId` 去掉了。

这样任务归属不再通过更新接口随意修改。

## 7. Service 层的核心规则

当前 `TaskService` 里开始承担“状态变化带来的业务后果”。

核心规则是：

### 1. 创建任务

- 创建 `TODO` 任务时：
  - `completedAt = null`
- 创建 `DONE` 任务时：
  - `completedAt = 当前时间`

### 2. 更新任务

- `TODO -> DONE`
  - 自动补 `completedAt`
- `DONE -> TODO`
  - 清空 `completedAt`
- `DONE -> DONE`
  - 保留原有完成时间

这说明：

**任务的完成时间不是前端随便传的，而是后端根据状态自动维护的。**

## 8. 这一步为什么有价值

因为它体现了一个很重要的工程意识：

### 不是所有字段都应该由前端控制

像：

- `userId`
- `completedAt`

这类字段都更适合由后端根据登录态或业务规则自动决定。

这会让系统更安全，也更接近真实项目。

## 9. 测试结果说明了什么

当前已经验证过这几种情况：

1. 创建 `TODO` 任务时，`completedAt = null`
2. 创建 `DONE` 任务时，`completedAt` 自动写入
3. 把任务改成 `DONE` 时，`completedAt` 自动补上
4. 把任务改回 `TODO` 时，`completedAt` 被清空

这说明任务模块现在已经不是单纯的“数据库字段回填”，而是有状态规则的。

## 10. 当前阶段结论

这节课完成后，`tasks` 模块相比之前提升了一层：

- 不再只是 CRUD
- 有状态流转影响
- 有自动维护的业务字段
- 更新接口的权限边界也更清晰

这类改造对实习项目很重要，因为它比继续机械堆接口更能体现：

- 你理解业务规则
- 你知道哪些字段该由后端控制
- 你能把模块从“练习型”往“项目型”推进
