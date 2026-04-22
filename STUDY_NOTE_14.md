# Study Note 14

## 主题

MyBatis XML 动态 SQL：排序分页与条件分页排序

## 1. 这节课解决什么问题

前面已经完成了：

- 注解版 MyBatis CRUD
- 条件查询
- 分页查询
- 条件分页查询

但当需求进一步变成：

```http
GET /users/page?page=1&size=2&sort=id&order=desc
```

或者：

```http
GET /users/search-page?name=huan&page=1&size=2&sort=id&order=desc
```

时，注解版 SQL 开始变得不顺手。

原因是：

- 排序字段是动态的
- 排序方向是动态的
- 还要同时处理分页和条件

这时继续硬写注解版 SQL，会越来越难看，也更容易写出不安全代码。

所以这节课的重点是：

**从注解版切到 XML 版 MyBatis，处理动态排序和更复杂的 SQL。**

## 2. 为什么这里要用 XML

注解版适合：

- 固定 SQL
- 简单增删改查
- 结构清晰、变化少的查询

XML 更适合：

- 动态 SQL
- 多条件判断
- 复杂 `where`
- 动态排序
- 分页和排序组合

当前这节排序分页，就是一个非常典型的 XML 适用场景。

## 3. XML 文件应该放在哪里

当前项目踩到的第一个坑是：

一开始把 `UserMapper.xml` 放到了 `java` 源码目录。

这是不对的。

MyBatis 读取 mapper XML 时，应该从资源目录加载。

当前项目正确位置是：

[UserMapper.xml](/Users/huanzichen/interview/SpringBoot/springboot-hello/src/main/resources/mapper/UserMapper.xml)

对应配置在：

[application.yml](/Users/huanzichen/interview/SpringBoot/springboot-hello/src/main/resources/application.yml)

```yml
mybatis:
  mapper-locations: classpath:mapper/*.xml
```

这条配置找的是 `resources/mapper/*.xml`，不是 `java` 目录。

## 4. 为什么会报 `Invalid bound statement`

当前项目出现过报错：

```text
Invalid bound statement (not found): com.huanzichen.springboothello.mapper.UserMapper.findPageWithSort
```

它的含义是：

- `UserMapper` 接口里声明了 `findPageWithSort`
- 但 MyBatis 没有找到与之对应的 XML `select`

本次实验里，根因不是方法名拼错，而是：

**XML 文件根本没在 MyBatis 扫描路径里。**

所以这类错误要先查：

1. XML 文件路径对不对
2. `namespace` 对不对
3. `select id` 对不对

## 5. XML 的 `namespace` 必须和接口一致

当前项目的 `UserMapper.xml` 里，最关键的一行是：

```xml
<mapper namespace="com.huanzichen.springboothello.mapper.UserMapper">
```

它必须和接口全限定名完全一致。

否则即使 XML 被加载了，也无法和接口方法绑定。

## 6. 排序为什么不能直接拼字符串

如果把用户传来的：

- `sort`
- `order`

直接拼到 SQL 里，很容易形成 SQL 注入风险。

比如用户理论上可以传：

```text
sort=id desc; drop table users
```

所以排序这类动态参数不能直接无脑拼接。

当前阶段正确思路是：

**做白名单。**

也就是只允许：

- `sort = id`
- `sort = age`
- `order = asc`
- `order = desc`

其他情况都走默认排序。

## 7. 当前项目里的动态排序 SQL

当前项目在 [UserMapper.xml](/Users/huanzichen/interview/SpringBoot/springboot-hello/src/main/resources/mapper/UserMapper.xml) 里使用了：

- `<choose>`
- `<when>`
- `<otherwise>`

去控制排序分支。

可以把它粗略理解成 Java 里的：

- `if`
- `else if`
- `else`

例如：

```xml
<choose>
    <when test="sort == 'id' and order == 'desc'">
        order by id desc
    </when>
    <when test="sort == 'id' and order == 'asc'">
        order by id asc
    </when>
    <when test="sort == 'age' and order == 'desc'">
        order by age desc
    </when>
    <when test="sort == 'age' and order == 'asc'">
        order by age asc
    </when>
    <otherwise>
        order by id asc
    </otherwise>
</choose>
```

这样既满足动态排序，又避免直接拼接任意字符串。

## 8. 参数为什么要加 `@Param`

当前项目在 `UserMapper` 接口里，XML 方法参数写成了类似：

```java
List<UserInfo> findPageWithSort(@Param("offset") Integer offset,
                                @Param("size") Integer size,
                                @Param("sort") String sort,
                                @Param("order") String order);
```

原因是：

XML 里要通过：

```xml
#{offset}
#{size}
#{sort}
#{order}
```

来引用方法参数。

如果参数名无法稳定映射，MyBatis 就拿不到这些值。

所以当前阶段对 XML 方法参数加 `@Param` 是更稳妥的写法。

## 9. 普通排序分页

当前项目把普通分页升级成了可排序的版本：

```http
GET /users/page?page=1&size=2&sort=id&order=desc
```

这条接口现在已经可以：

- 分页
- 排序
- 返回统一的 `PageResult<T>`

## 10. 条件分页排序

后面进一步把三条线合成了一个更完整的接口：

```http
GET /users/search-page?name=huan&page=1&size=2&sort=id&order=desc
```

这个接口同时完成：

1. `name` 模糊查询
2. 分页
3. 排序
4. 返回总条数和总页数

这已经是一个非常接近真实业务列表接口的形态。

## 11. Service 层的小重构

实现过程中还发现了一个重复逻辑：

- `sort` 为空时默认 `id`
- `order` 为空时默认 `asc`

这类逻辑在多个方法里会重复。

当前阶段合适的做法是：

在 `UserService` 里抽私有方法，例如：

- `normalizeSort`
- `normalizeOrder`

这样可以：

- 减少重复
- 保持 service 主流程更清晰

## 12. 这节课真正要记住什么

### 结论 1

注解版 MyBatis 适合简单固定 SQL，复杂动态 SQL 更适合 XML。

### 结论 2

MyBatis XML 必须放在 `resources` 目录下，并和 `mapper-locations` 配置对上。

### 结论 3

动态排序不能直接拼用户输入，应该做白名单。

### 结论 4

`namespace` 必须和 mapper 接口全限定名一致。

### 结论 5

当一个接口同时需要条件、分页、排序时，XML 的优势会很明显。

## 13. 当前进度定位

到这一步，你已经不只是会：

- 写固定 SQL
- 做普通 CRUD

你已经开始接触：

- MyBatis XML
- 动态 SQL
- 排序白名单
- 更接近真实项目的列表接口设计

这说明你的 Spring Boot + MyBatis 学习已经明显超出“最基础入门”阶段，开始进入更真实的业务开发层面。
