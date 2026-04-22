# Study Note 13

## 主题

完善分页返回对象：增加 `totalPages`

## 1. 这节课解决什么问题

前一节已经把分页接口统一成了：

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "total": 2,
    "page": 1,
    "size": 2,
    "list": [...]
  }
}
```

这已经比只返回 `List<T>` 好很多了。

但前端如果想渲染分页组件，通常还会需要一个额外信息：

- 总页数 `totalPages`

否则前端还要自己再根据 `total` 和 `size` 计算一遍。

## 2. 为什么要把 `totalPages` 直接返回

如果后端只返回：

- `total`
- `size`

前端当然也能自己算总页数。

但把它直接放进响应里有几个好处：

1. 分页信息更完整
2. 前端不需要重复写同一套计算逻辑
3. 后端统一定义分页语义，接口更稳定

当前阶段虽然项目还小，但这种返回结构已经更接近真实项目。

## 3. 当前项目里的分页结构

当前项目里的 [PageResult.java](/Users/huanzichen/interview/SpringBoot/springboot-hello/src/main/java/com/huanzichen/springboothello/common/PageResult.java) 现在已经包含：

- `total`
- `list`
- `page`
- `size`
- `totalPages`

这样分页结果就更完整了。

## 4. 总页数怎么计算

计算公式是：

```java
int totalPages = (int) ((total + size - 1) / size);
```

它的本质是“向上取整”。

### 例子 1

如果：

- `total = 5`
- `size = 2`

那么：

```text
totalPages = 3
```

因为 5 条数据每页 2 条，需要 3 页。

### 例子 2

如果：

- `total = 4`
- `size = 2`

那么：

```text
totalPages = 2
```

因为刚好分满两页。

## 5. 这个公式为什么能向上取整

这个公式常见但不容易一眼看懂。

```java
(total + size - 1) / size
```

可以这样理解：

- 如果正好整除，加上 `size - 1` 以后再整除，结果不会多出一页
- 如果不能整除，它会把余数那一页补出来

现阶段不需要死抠数学证明，只要会用并知道它是“向上取整”就够了。

## 6. 命名统一的重要性

实现过程中一度出现过字段名：

```json
"totalPage": 1
```

后来统一改成了：

```json
"totalPages": 1
```

这个修改虽然不影响功能，但很重要。

原因：

- `totalPages` 表意更准确
- 和 `page` 的关系更清楚
- 能减少接口命名不一致带来的沟通成本

当前阶段就要养成这个习惯：

**功能做对之后，要继续把命名收干净。**

## 7. 当前分页返回结果

现在分页接口的响应已经稳定为：

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "total": 2,
    "list": [
      {"id": 3, "name": "huanzichen", "age": 18},
      {"id": 4, "name": "huanzichen", "age": 19}
    ],
    "page": 1,
    "size": 2,
    "totalPages": 1
  }
}
```

这已经是一个比较像样的分页返回结构。

## 8. 这节课真正要记住什么

### 结论 1

分页响应不仅要返回 `list`，还应该返回完整的分页信息。

### 结论 2

`totalPages` 的常用计算公式是：

```java
(total + size - 1) / size
```

### 结论 3

当字段命名不统一时，要尽早收口。

### 结论 4

分页对象一旦稳定下来，后面所有分页接口都应该复用同一套结构。

## 9. 当前进度定位

到这一步，你已经不只是“会写分页 SQL”。

你已经开始具备：

- 设计分页响应对象
- 组织分页元数据
- 控制接口命名一致性

这说明你正在从“功能能跑”进入“接口设计更像真实项目”的阶段。
