# Study Note 16

## 主题

MyBatis XML 动态 `where`：多条件查询与条件分页

## 1. 这节课解决什么问题

前面已经完成了：

- 单条件查询
- 分页
- 排序
- 条件分页排序

但之前条件还是比较单一，例如只按 `name` 查。

真实项目里，列表查询通常不会只有一个条件，而是像这样：

```http
GET /users/search-page?name=huan&minAge=18&maxAge=30&page=1&size=5&sort=id&order=desc
```

这意味着 SQL 里的 `where` 条件是动态的：

- 有时只传 `name`
- 有时只传年龄范围
- 有时两者都传
- 有时三者都不传

这就是 MyBatis XML 动态 `where` 的典型使用场景。

## 2. 为什么这一步不能继续写死 SQL

如果把每种情况都单独写一个方法，例如：

- 按名字查
- 按最小年龄查
- 按最大年龄查
- 按名字 + 最小年龄查
- 按名字 + 最大年龄查
- 按名字 + 年龄范围查

方法数量会迅速膨胀。

这类场景更合理的做法是：

**用一条动态 SQL，根据传入参数决定拼哪些条件。**

## 3. XML 里的两个关键标签

这节最关键的是：

- `<where>`
- `<if>`

### `<if>`

作用：

只有当条件成立时，才把这段 SQL 拼进去。

例如：

```xml
<if test="minAge != null">
    and age &gt;= #{minAge}
</if>
```

意思是：

- 只有 `minAge` 不为空时，才拼 `age >= ...`

### `<where>`

作用：

自动帮你处理：

- 是否需要加 `where`
- 开头多余的 `and`

也就是说，你只管写条件片段，MyBatis 帮你把 `where` 拼干净。

## 4. 当前项目里的动态条件

当前项目支持的可选查询条件有：

- `name`
- `minAge`
- `maxAge`

XML 里大致结构是：

```xml
<where>
    <if test="name != null and name != ''">
        name like concat('%', #{name}, '%')
    </if>
    <if test="minAge != null">
        and age &gt;= #{minAge}
    </if>
    <if test="maxAge != null">
        and age &lt;= #{maxAge}
    </if>
</where>
```

这样就能根据不同参数自动拼出不同 `where`。

## 5. 为什么 XML 里要写 `&gt;=` 和 `&lt;=`

XML 不是普通字符串文件，它有自己的语法规则。

所以在 XML 里：

- `>=` 不能直接写成普通文本条件的一部分时，通常写成 `&gt;=`
- `<=` 通常写成 `&lt;=`

这是 XML 转义，不是 MyBatis 特性。

当前阶段你先记住：

**在 MyBatis XML 里写比较运算符时，要注意 XML 转义。**

## 6. 条件分页为什么仍然要两条 SQL

即使已经是多条件查询，分页接口仍然需要两条 SQL：

### 1. 查当前页数据

例如：

- `searchWithConditionsAndPage(...)`

### 2. 查满足条件的总数

例如：

- `countWithConditions(...)`

原因很简单：

前端不仅要知道当前页数据，还要知道：

- 一共有多少条
- 一共有多少页

所以不能只查当前页。

## 7. Service 层做了什么

当前项目的 `searchUsersByPage(...)` 已经升级为支持：

- `name`
- `minAge`
- `maxAge`
- `page`
- `size`
- `sort`
- `order`

Service 层主要做：

1. 校验分页参数
2. 规范化排序参数
3. 校验年龄范围是否合法
4. 计算 `offset`
5. 调用 XML 查 list
6. 调用 XML 查 total
7. 组装 `PageResult<UserInfo>`

这说明：

**Controller 负责接参数，Service 负责组织查询逻辑。**

## 8. 年龄范围为什么要在 Service 校验

当前项目增加了一个业务校验：

如果：

```text
minAge > maxAge
```

就返回：

```json
{
  "code": 400,
  "message": "minAge cannot be greater than maxAge",
  "data": null
}
```

这类规则不属于 SQL 层，也不该交给数据库去报错，而应该在 Service 层提前判断。

这也是为什么 Service 不只是“转调 mapper”，它还要承担业务规则校验。

## 9. 这节课踩到的 XML 错误

这次还遇到了几类很典型的 XML 错误：

### 错误 1：`resultType` 写到标签外面

例如错误写法：

```xml
<select id="xxx">
resultType="..."
```

正确写法应该是：

```xml
<select id="xxx" resultType="...">
```

### 错误 2：SQL 关键字拼错

例如曾经写出：

```sql
order age id desc
```

正确应该是：

```sql
order by age desc
```

### 错误 3：XML 文件路径不对

MyBatis XML 必须放在：

```text
src/main/resources/mapper
```

不能放在 `java` 源码目录。

## 10. 当前接口形态

现在的条件分页接口已经能支持：

### 只按年龄范围查

```http
GET /users/search-page?minAge=18&maxAge=30&page=1&size=5&sort=id&order=asc
```

### 按名字 + 年龄范围查

```http
GET /users/search-page?name=huan&minAge=18&maxAge=30&page=1&size=5&sort=id&order=asc
```

### 非法范围

```http
GET /users/search-page?minAge=30&maxAge=18&page=1&size=5
```

这已经是一个明显更接近真实后台列表接口的形态。

## 11. 这节课真正要记住什么

### 结论 1

多条件查询不要通过“为每种组合都写一个方法”去解决。

### 结论 2

MyBatis XML 的 `<where>` 和 `<if>` 是动态条件拼接的核心工具。

### 结论 3

条件分页仍然需要：

1. 当前页数据查询
2. 条件总数查询

### 结论 4

SQL 负责查数据，Service 负责补业务校验。

### 结论 5

当查询条件越来越多时，XML 动态 SQL 的价值会越来越明显。

## 12. 当前进度定位

到这一步，你已经不只是会做“固定字段查询”。

你已经开始掌握：

- 多条件动态查询
- 条件分页
- 条件总数统计
- 业务层范围校验

这已经非常接近真实项目里常见的后台列表查询实现方式。
