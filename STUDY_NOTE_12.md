# Study Note 12

## 主题

统一分页返回结构 `PageResult<T>` 与条件分页查询

## 1. 这节课解决什么问题

前面已经完成了：

- 普通列表查询
- 条件查询
- 基础分页查询

但如果分页接口只返回一个 `List<T>`，前端通常还会缺少一些重要信息：

- 总条数 `total`
- 当前页 `page`
- 每页大小 `size`

所以这节课的重点是：

1. 设计统一的分页响应结构
2. 把条件查询和分页查询合成一个更像真实项目的接口

## 2. 为什么 `List<T>` 不够

如果分页接口只返回：

```json
[
  {"id": 1, "name": "a", "age": 18},
  {"id": 2, "name": "b", "age": 19}
]
```

前端只能拿到“当前页的数据”，但不知道：

- 一共有多少条
- 一共有多少页
- 现在是不是最后一页

所以分页接口通常不会只返回一个列表。

## 3. 当前项目里的分页对象

当前项目新建了：

[PageResult.java](/Users/huanzichen/interview/SpringBoot/springboot-hello/src/main/java/com/huanzichen/springboothello/common/PageResult.java)

当前统一字段为：

- `total`
- `page`
- `size`
- `list`

也就是说，分页接口最终返回的是：

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "total": 2,
    "page": 1,
    "size": 2,
    "list": [
      {"id": 3, "name": "huanzichen", "age": 18},
      {"id": 4, "name": "huanzichen", "age": 19}
    ]
  }
}
```

## 4. 为什么统一成 `list`

实现过程中一度出现过：

- `rows`
- `list`

两种字段名并存。

这不是功能错误，但会让接口风格不一致。

最终统一成 `list`，原因是：

- 语义直观
- 更符合很多 Java 项目的命名习惯
- 后续所有分页接口都能保持一致

当前阶段最重要的是：

**同一类接口的返回结构要统一。**

## 5. 分页 Mapper 需要做两件事

分页不是只查“当前页数据”就结束了，还要查“总条数”。

所以当前项目里，分页相关通常需要两类 SQL：

### 当前页数据

```sql
select id, name, age from users limit #{offset}, #{size}
```

### 总条数

```sql
select count(*) from users
```

如果是条件分页，还需要：

### 条件分页数据

```sql
select id, name, age
from users
where name like concat('%', #{name}, '%')
limit #{offset}, #{size}
```

### 条件总条数

```sql
select count(*)
from users
where name like concat('%', #{name}, '%')
```

## 6. 条件分页接口

当前项目增加了一个更接近真实项目的接口：

```http
GET /users/search-page?name=huan&page=1&size=1
```

它同时做两件事：

1. 先按 `name` 模糊查询
2. 再按 `page` 和 `size` 做分页

这比单独的：

- `/users/search`
- `/users/page`

更接近真实业务接口设计。

## 7. Service 层职责

当前阶段，`UserService` 在分页场景里主要负责：

1. 参数校验
2. 计算 `offset`
3. 调用 mapper 查询当前页数据
4. 调用 mapper 查询总条数
5. 组装 `PageResult<UserInfo>`

也就是说，分页逻辑不是 controller 去拼，而是 service 负责组织业务结果。

## 8. 为什么要抽分页参数校验

前面已经至少有两个地方会校验：

- 普通分页
- 条件分页

如果每个方法都重复写：

```java
if (page == null || page <= 0) ...
if (size == null || size <= 0) ...
```

后面会越来越乱。

所以当前项目把它抽成了 `UserService` 内部的私有方法，例如：

```java
private void validatePageParams(Integer page, Integer size)
```

这类重构的意义是：

- 降低重复代码
- 让分页方法主流程更清楚
- 后续修改提示文案时只改一个地方

## 9. `int` 和 `Integer` 的选择

这节课里还碰到了一个常见问题：

Mapper 或 Service 方法参数写 `int` 还是 `Integer`？

当前阶段的直觉规则：

### 用 `Integer`

适合：

- 请求参数
- DTO 字段
- 可能为空的值

因为它可以表示 `null`。

### 用 `int`

适合：

- 内部纯计算的局部变量

例如：

```java
int offset = (page - 1) * size;
```

所以像 `page`、`size` 这类请求输入，用 `Integer` 更自然。

## 10. 这节课真正要记住什么

### 结论 1

分页接口通常不应该只返回 `List<T>`。

### 结论 2

分页返回结构至少应该包含：

- `list`
- `total`
- `page`
- `size`

### 结论 3

分页通常需要两次查询：

1. 查当前页数据
2. 查总条数

### 结论 4

条件查询和分页查询可以组合成更真实的业务接口。

### 结论 5

当分页参数校验开始重复时，就应该做小重构。

## 11. 当前进度定位

到这一步，你已经不只是会写基础 CRUD。

你已经开始掌握真实项目里常见的接口能力：

- 条件查询
- 分页查询
- 条件分页查询
- 统一分页返回结构

这已经明显超出了“Spring Boot 最基础入门”的层级，开始接近实习项目里的常见后端模块写法。
