# Study Note 24

## 主题

查询任务状态变更记录：`GET /tasks/{id}/status-logs`

## 1. 这节课解决什么问题

前一节已经实现了：

- 任务状态变化时自动写日志

但如果只能写，不能查，这条功能链路还是不完整。

所以这节课新增的是：

```text
GET /tasks/{id}/status-logs
```

目标是：

- 查看某条任务的状态变化历史
- 同时继续遵守任务归属权限

这一步完成后，任务状态日志就从：

```text
只会写
```

变成：

```text
能写 + 能查
```

## 2. 为什么这一步有价值

因为“状态变更记录”如果只能写进数据库，项目价值还没有完全体现出来。

只有当你真的能把它查出来，接口能力才更像真实业务系统，比如：

- 查看任务历史
- 看某条任务怎么一步步从 `TODO` 变成 `DONE`
- 追踪是谁做过状态修改

所以这一步会让状态日志从“内部技术实现”变成“对外可用能力”。

## 3. 这一节怎么实现

### 1. Mapper 增加查询方法

[TaskStatusLogMapper.java](/Users/huanzichen/interview/SpringBoot/springboot-hello/src/main/java/com/huanzichen/springboothello/mapper/TaskStatusLogMapper.java)

新增：

```java
findByTaskId(Long taskId)
```

SQL 做了几件事：

- 按 `task_id` 查询
- 把数据库下划线字段映射成 Java 驼峰字段
- 按 `created_at desc` 倒序排列

也就是说，前端拿到的会是“最新日志在最前面”的列表。

### 2. Service 层先做任务归属校验

[TaskService.java](/Users/huanzichen/interview/SpringBoot/springboot-hello/src/main/java/com/huanzichen/springboothello/service/TaskService.java)

新增：

```java
listTaskStatusLogs(Long taskId)
```

它不是直接查日志，而是先调用：

```java
getOwnedTask(taskId)
```

先确认：

- 任务存在
- 任务属于当前登录用户

只有通过以后，才真正去查日志。

## 4. 为什么这里要复用 `getOwnedTask`

因为前面项目已经把任务权限收得比较清楚了：

- 只能看自己的任务
- 只能改自己的任务
- 只能删自己的任务

那现在查状态日志，也应该遵守同一套规则。

所以最合理的方式不是重新写一遍权限判断，而是直接复用：

```java
getOwnedTask(taskId)
```

这体现的是：

**权限规则应该尽量复用，而不是散落在不同接口里重复实现。**

## 5. 这一步完成后的行为

### 当前登录用户查看自己的任务日志

```http
GET /tasks/{id}/status-logs
```

会返回类似：

```json
[
  {
    "id": 2,
    "taskId": 3,
    "oldStatus": "TODO",
    "newStatus": "DONE",
    "operatorUserId": 23,
    "createdAt": "2026-04-14T21:53:35"
  },
  {
    "id": 1,
    "taskId": 3,
    "oldStatus": "DONE",
    "newStatus": "TODO",
    "operatorUserId": 23,
    "createdAt": "2026-04-14T21:53:30"
  }
]
```

这说明：

- 日志能按任务查询
- 能按时间倒序返回
- 也能看出状态变化顺序

### 查看别人的任务日志

如果任务不属于当前用户，应该返回：

```json
{
  "code": 403,
  "message": "no permission",
  "data": null
}
```

也就是说，日志本身也属于受保护资源，不是随便能看的。

## 6. 当前任务日志主线已经闭环

现在围绕任务状态日志，你已经有了：

### 1. 写日志

任务状态真正变化时自动插入 `task_status_logs`

### 2. 查日志

可以通过：

```text
/tasks/{id}/status-logs
```

拿到某条任务的状态变更历史

### 3. 权限控制

只有任务拥有者自己能查看这条历史

这三件事合在一起，说明状态日志已经从“数据库记录”升级成了真正的业务能力。

## 7. 这节课的价值

这节课之后，项目里的 `tasks` 模块已经不仅有：

- 当前状态
- 完成时间
- 状态统计

还多了：

- 状态历史记录
- 状态历史查询

这意味着你现在做的已经不是简单任务表，而是一个更完整的任务管理模块。

在面试里你现在可以讲：

- 我给任务模块做了状态流转规则
- 状态变化会影响完成时间
- 状态变化还会自动记录日志
- 这些日志可以按任务查询，并且同样受权限保护

这类表达会明显比“我做了 CRUD”更有说服力。
