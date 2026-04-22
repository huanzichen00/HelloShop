# Spring Boot 学习笔记 06

## 1. 本节目标

这一节在基础 CRUD 之上继续补两个常见查询能力：

1. 条件查询
2. 分页查询

这两个能力在真实项目里非常常见，例如：

- 按用户名搜索
- 分页展示用户列表
- 后台管理列表筛选

## 2. 条件查询：按名称模糊搜索

当前接口：

```http
GET /users/search?name=huan
```

Controller：

- [HelloController.java](/Users/huanzichen/interview/SpringBoot/springboot-hello/src/main/java/com/huanzichen/springboothello/controller/HelloController.java)

```java
@GetMapping("/users/search")
public Result<List<UserInfo>> searchUsers(String name) {
    return Result.success(userService.searchUsers(name));
}
```

理解：

1. `name` 来自查询参数
2. 例如 `/users/search?name=huan`
3. Spring 会把 `name=huan` 绑定到方法参数 `name`

## 3. Service 中的条件校验

Service：

- [UserService.java](/Users/huanzichen/interview/SpringBoot/springboot-hello/src/main/java/com/huanzichen/springboothello/service/UserService.java)

```java
public List<UserInfo> searchUsers(String name) {
    if (name == null || name.trim().isEmpty()) {
        throw new RuntimeException("name cannot be blank");
    }

    return userMapper.searchByName(name);
}
```

理解：

1. 没传 `name` 时，参数是 `null`
2. 传 `name=` 或纯空格时，也是不合法查询条件
3. 这种查询条件校验可以先放在 Service

## 4. Mapper 中的模糊查询

Mapper：

- [UserMapper.java](/Users/huanzichen/interview/SpringBoot/springboot-hello/src/main/java/com/huanzichen/springboothello/mapper/UserMapper.java)

```java
@Select("select id, name, age from users where name like concat('%', #{name}, '%')")
List<UserInfo> searchByName(String name);
```

核心是：

```sql
where name like concat('%', #{name}, '%')
```

如果 `name = huan`，大致等价于：

```sql
where name like '%huan%'
```

意思是：只要名字中包含 `huan`，就能查出来。

## 5. 条件查询请求示例

请求：

```http
GET http://localhost:8081/users/search?name=huan
```

返回示例：

```json
{
  "code": 200,
  "message": "success",
  "data": [
    {
      "id": 2,
      "name": "sunxiaochuan",
      "age": 40
    },
    {
      "id": 3,
      "name": "huanzichen",
      "age": 18
    }
  ]
}
```

非法请求：

```http
GET http://localhost:8081/users/search
```

返回：

```json
{
  "code": 400,
  "message": "name cannot be blank",
  "data": null
}
```

## 6. 分页查询的核心概念

分页里最重要的是三个参数：

1. `page`：第几页，从 1 开始
2. `size`：每页多少条
3. `offset`：跳过多少条

核心公式：

```text
offset = (page - 1) * size
```

例子：

- `page=1&size=2` -> `offset=0`
- `page=2&size=2` -> `offset=2`
- `page=3&size=2` -> `offset=4`

## 7. 分页接口

当前接口：

```http
GET /users/page?page=1&size=2
```

Controller：

```java
@GetMapping("/users/page")
public Result<List<UserInfo>> findUsersByPage(Integer page, Integer size) {
    return Result.success(userService.listUsersByPage(page, size));
}
```

理解：

1. `page` 和 `size` 都来自查询参数
2. Controller 不计算分页
3. 分页规则放到 Service 中处理

## 8. 分页参数校验

Service：

```java
public List<UserInfo> listUsersByPage(Integer page, Integer size) {
    if (page == null || page <= 0) {
        throw new RuntimeException("page must be greater than zero");
    }
    if (size == null || size <= 0) {
        throw new RuntimeException("size must be greater than zero");
    }

    int offset = (page - 1) * size;
    return userMapper.findPage(offset, size);
}
```

理解：

1. `page` 必须大于 0
2. `size` 必须大于 0
3. `offset` 由 Service 计算
4. Mapper 只负责执行 SQL

## 9. Mapper 中的分页查询

Mapper：

```java
@Select("select id, name, age from users limit #{offset}, #{size}")
List<UserInfo> findPage(int offset, int size);
```

MySQL 分页语法：

```sql
limit offset, size
```

注意：

逗号不能少。

错误写法：

```sql
limit #{offset} #{size}
```

会变成：

```sql
limit ? ?
```

这是 SQL 语法错误。

## 10. 分页请求示例

请求第一页：

```http
GET http://localhost:8081/users/page?page=1&size=2
```

返回：

```json
{
  "code": 200,
  "message": "success",
  "data": [
    {
      "id": 2,
      "name": "sunxiaochuan",
      "age": 40
    },
    {
      "id": 3,
      "name": "huanzichen",
      "age": 18
    }
  ]
}
```

请求第二页：

```http
GET http://localhost:8081/users/page?page=2&size=2
```

返回：

```json
{
  "code": 200,
  "message": "success",
  "data": [
    {
      "id": 4,
      "name": "huanzichen",
      "age": 19
    },
    {
      "id": 5,
      "name": "cainong",
      "age": 35
    }
  ]
}
```

非法请求：

```http
GET http://localhost:8081/users/page?page=0&size=2
```

返回：

```json
{
  "code": 400,
  "message": "page must be greater than zero",
  "data": null
}
```

## 11. 这一节真正要掌握什么

1. 查询参数可以用来做筛选条件
2. 模糊查询常用 `like`
3. MySQL 分页核心是 `limit offset, size`
4. `offset = (page - 1) * size`
5. Controller 不应该堆分页计算逻辑
6. Service 负责参数校验和分页计算
7. Mapper 负责把 SQL 执行出来

## 12. 当前用户模块能力

到现在，用户模块已经具备：

1. 创建用户
2. 查询用户列表
3. 根据 id 查询用户
4. 更新用户
5. 删除用户
6. 按名称模糊查询
7. 分页查询

这已经是一个小型后端业务模块的基础形态。

## 13. 下一步学习方向

下一步建议进入：

1. 自定义业务异常 `BusinessException`
2. 更清晰的错误码
3. 拆分 Controller：把练习接口和用户接口分开
