# Study Note 22

## 主题

任务状态统计接口：`GET /tasks/status-summary`

## 1. 这节课解决什么问题

前面的 `tasks` 模块已经有了：

- 列表
- 详情
- 创建
- 更新
- 删除
- 分页查询
- 状态流转
- 权限控制

这些已经比普通 CRUD 完整很多了，但它们仍然主要是“资源操作接口”。

这节课新增的：

```text
GET /tasks/status-summary
```

解决的是另一个问题：

**当前登录用户的任务概览应该怎么返回。**

也就是：

- 我现在有多少 `TODO`
- 我现在有多少 `DONE`

这类接口不再是查某条记录，而是给前端提供“概览数据”。

## 2. 为什么这个接口比普通 CRUD 更像项目接口

因为真实项目里通常不只有：

- 表单提交
- 列表分页
- 单条详情

还会有很多：

- 首页统计
- 仪表盘概览
- 状态汇总
- 模块概览

`/tasks/status-summary` 就属于这类接口。

所以这节课的价值不在于代码量，而在于：

**项目开始出现“面向业务视图”的接口，而不只是面向表的 CRUD。**

## 3. 当前实现方式

这一步没有直接上复杂 SQL，而是先用最直观、最容易理解的方式实现。

在 [TaskMapper.java](/Users/huanzichen/interview/SpringBoot/springboot-hello/src/main/java/com/huanzichen/springboothello/mapper/TaskMapper.java) 里新增了两个方法：

```java
countTodoByUserId(Long userId)
countDoneByUserId(Long userId)
```

分别统计当前用户：

- `TODO` 任务数量
- `DONE` 任务数量

然后在 `TaskService` 里组装成一个返回对象。

## 4. 为什么这里先不用 `group by`

技术上当然可以直接写成：

```sql
select status, count(*) ...
group by status
```

但当前阶段先拆成两个简单 count 更合适，原因是：

1. 逻辑更直观
2. 更符合当前学习阶段
3. 先把“统计接口”这类接口类型建立起来
4. 后面如果再扩展成更多状态，再考虑 `group by`

这也是一个很重要的学习策略：

**先做清楚，再做抽象。**

## 5. 返回对象为什么单独建 VO

这一步没有直接返回 `Map<String, Long>`，而是新建了：

[TaskStatusSummaryVO.java](/Users/huanzichen/interview/SpringBoot/springboot-hello/src/main/java/com/huanzichen/springboothello/model/TaskStatusSummaryVO.java)

字段是：

- `todoCount`
- `doneCount`

这样做的好处是：

- 结构稳定
- 语义明确
- 前后端约定更清晰
- 后面如果要再扩展字段更方便

## 6. 这节课踩到的典型错误

### 1. 把用户 id 当成任务 id 去查

一开始 `TaskService#getTaskStatusSummary()` 里错误写成了：

```java
getOwnedTask(userId)
```

但 `getOwnedTask(...)` 的参数语义是：

```text
任务 id
```

不是用户 id。

所以它会直接抛：

```json
{
  "code": 404,
  "message": "task not found",
  "data": null
}
```

这个错误本质上是：

**把“当前用户统计”误写成了“根据当前用户 id 查某条任务”。**

正确做法是：

- 直接拿当前用户 id
- 分别 count
- 再组装 summary

### 2. 路由冲突导致固定路径被动态路径吃掉

一开始：

```java
@GetMapping("/{id}")
```

没有加数字约束。

所以：

```text
/tasks/status-summary
```

有机会被当成：

```text
id = "status-summary"
```

然后走错接口。

后面通过把动态路由改成：

```java
/{id:\\d+}
```

才把固定路径和动态路径彻底分开。

这类问题前面在 `/users/me`、`/users/me/tasks` 上也踩过，说明：

**固定路径和动态路径混用时，数字约束非常重要。**

## 7. 当前最终效果

现在：

```http
GET /tasks/status-summary
Authorization: Bearer <token>
```

可以返回类似：

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "todoCount": 1,
    "doneCount": 0
  }
}
```

而未登录访问则会被拦截成：

```json
{
  "code": 401,
  "message": "please login first",
  "data": null
}
```

## 8. 这一节的价值

这节课完成后，项目里又多了一类更像真实系统的接口：

- 不只是资源 CRUD
- 不只是分页列表
- 而是“当前用户视角的概览统计”

这对项目质量的提升很明显，因为它开始让项目具备：

- 用户中心
- 任务中心
- 任务概览

这类更接近真实业务系统的结构。
