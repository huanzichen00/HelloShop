# Study Note 17

## 主题

查询参数 DTO：`UserQueryDTO` 与默认值收口

## 1. 这节课解决什么问题

随着查询功能越来越完整，`search-page` 接口的参数已经变成了这种样子：

```java
searchUsersByPage(String name,
                  Integer minAge,
                  Integer maxAge,
                  Integer page,
                  Integer size,
                  String sort,
                  String order)
```

这在功能上能跑，但继续这样长下去会有两个问题：

1. Controller 方法签名越来越长
2. 查询参数越来越难管理

所以这节课的重点是：

**把查询参数收进一个专门的 DTO。**

## 2. 为什么查询也要单独 DTO

前面已经有：

- `UserCreateDTO`
- `UserUpdateDTO`

它们分别负责：

- 创建请求
- 更新请求

同样地，查询请求也有自己的参数结构，所以也应该有自己的 DTO。

于是当前项目新增了：

[UserQueryDTO.java](/Users/huanzichen/interview/SpringBoot/springboot-hello/src/main/java/com/huanzichen/springboothello/dto/UserQueryDTO.java)

## 3. 当前 `UserQueryDTO` 包含哪些字段

当前项目的查询 DTO 主要包含：

- `name`
- `minAge`
- `maxAge`
- `page`
- `size`
- `sort`
- `order`

这正好覆盖了当前 `search-page` 接口的查询参数。

## 4. Controller 为什么会更清爽

有了 DTO 以后，controller 方法不需要再写一长串散参数。

可以从这种风格：

```java
public Result<PageResult<UserInfo>> searchUsersByPage(String name,
                                                      Integer minAge,
                                                      Integer maxAge,
                                                      Integer page,
                                                      Integer size,
                                                      String sort,
                                                      String order)
```

收成更清楚的写法：

```java
public Result<PageResult<UserInfo>> searchUsersByPage(UserQueryDTO queryDTO)
```

这样一来：

- 接口签名更短
- 参数语义更集中
- 后面加新查询条件也更容易维护

## 5. 为什么 `GET` 请求也能绑定 DTO

很多初学者容易误以为：

- `DTO` 只用于 `POST` / `PUT` 请求体

其实不是。

对 Spring MVC 来说：

- `POST/PUT` 常用 `@RequestBody` 绑定 JSON 到 DTO
- `GET` 的 query parameter 也可以自动绑定到普通对象

例如：

```http
GET /users/search-page?name=huan&page=1&size=5
```

Spring 会把这些 query parameter 自动填到 `UserQueryDTO` 对应字段里。

所以 `GET` 请求同样可以使用 DTO 来收参数。

## 6. DTO 默认值为什么有用

当前项目进一步给 `UserQueryDTO` 设置了默认值，例如：

- `page = 1`
- `size = 10`
- `sort = "id"`
- `order = "asc"`

这样当请求里没有传这些参数时，DTO 自己就有一个合理默认值。

例如：

```http
GET /users/search-page?name=huan
```

即使没传：

- `page`
- `size`
- `sort`
- `order`

接口依然可以跑出一个合理结果。

## 7. 为什么有了默认值还不能删 `normalize`

这是这节课里非常容易误解的一点。

当前项目里，`UserQueryDTO` 已经给了一些字段默认值，但 `UserService` 里的：

- `normalizeSort(...)`
- `normalizeOrder(...)`

仍然不能直接删。

原因是：

### 场景 1：参数根本没传

例如：

```http
/users/search-page?name=huan
```

这时 DTO 默认值会生效。

### 场景 2：参数传了，但传的是空字符串

例如：

```http
/users/search-page?name=huan&sort=&order=
```

这时 Spring 绑定出来的不是 `null`，而是空字符串 `""`。

DTO 默认值已经不会再顶上来。

所以 service 仍然需要做最后一步规范化。

## 8. `normalize` 现在的真正职责

在当前阶段，`normalizeSort` 和 `normalizeOrder` 不只是“补默认值”，更像：

**最终参数规范化和白名单兜底。**

例如可以做这些事：

- 处理 `null`
- 处理空字符串
- 处理非法值
- 统一大小写

这样最后传给 mapper 的参数才更稳定。

## 9. 这节课真正要记住什么

### 结论 1

查询参数多起来以后，应该收成专门的 Query DTO。

### 结论 2

`GET` 请求同样可以绑定到 DTO，不是只有 `POST` / `PUT` 才能用 DTO。

### 结论 3

DTO 默认值能解决“没传参数”的情况。

### 结论 4

`normalize` 方法还能继续保留，用于处理空字符串和非法输入。

### 结论 5

DTO 负责收参数，service 负责做最终规范化，两者不是互相替代，而是分工协作。

## 10. 当前进度定位

到这一步，你已经开始形成比较清晰的接口参数分层：

- 创建请求：`UserCreateDTO`
- 更新请求：`UserUpdateDTO`
- 查询请求：`UserQueryDTO`

这比“所有请求都堆散参数”要成熟很多，也更接近真实项目中的写法。
