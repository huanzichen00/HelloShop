# Spring Boot 学习笔记 04

## 1. 本节目标

这一节的目标是把用户模块从“内存 List”切换到真实数据库：

1. 接入 `MyBatis`
2. 连接 `MySQL`
3. 用注解版 Mapper 完成基础 CRUD
4. 理解 `Controller -> Service -> Mapper -> MySQL` 链路

## 2. 当前数据库表

当前使用的数据库是：

```text
springboot_study
```

当前使用的表是：

```sql
CREATE TABLE users (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(50) NOT NULL,
    age INT NOT NULL
);
```

字段理解：

1. `id`：主键，由数据库自动生成
2. `name`：用户名
3. `age`：年龄

创建用户时不需要传 `id`，因为 `id` 是自增字段。

## 3. 数据库连接配置

当前数据库配置在：

- [application.yml](/Users/huanzichen/interview/SpringBoot/springboot-hello/src/main/resources/application.yml)

核心配置：

```yml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/springboot_study?useSSL=false&serverTimezone=Asia/Shanghai&characterEncoding=utf8
    username: root
    password: your-password
    driver-class-name: com.mysql.cj.jdbc.Driver
```

理解：

1. `url` 表示连接哪个 MySQL 实例、哪个数据库
2. `username` / `password` 是数据库账号密码
3. `driver-class-name` 指定 MySQL JDBC 驱动

注意：

- IDEA Database 面板和 Spring Boot 的 `application.yml` 是两套配置。
- IDEA 里能看到数据库，不代表 Spring Boot 一定配置正确。
- Spring Boot 能查到数据，IDEA 里看不到时，优先检查是否看错 schema 或未刷新。

## 4. MyBatis 依赖

项目在 `pom.xml` 中加入了：

```xml
<dependency>
    <groupId>org.mybatis.spring.boot</groupId>
    <artifactId>mybatis-spring-boot-starter</artifactId>
    <version>3.0.4</version>
</dependency>

<dependency>
    <groupId>com.mysql</groupId>
    <artifactId>mysql-connector-j</artifactId>
    <scope>runtime</scope>
</dependency>
```

理解：

1. `mybatis-spring-boot-starter` 负责 Spring Boot 和 MyBatis 整合
2. `mysql-connector-j` 是 Java 连接 MySQL 的驱动

## 5. Mapper 是什么

当前 Mapper 文件：

- [UserMapper.java](/Users/huanzichen/interview/SpringBoot/springboot-hello/src/main/java/com/huanzichen/springboothello/mapper/UserMapper.java)

当前先使用注解版 MyBatis。

### `@Mapper`

```java
@Mapper
public interface UserMapper {
}
```

作用：

1. 告诉 MyBatis 这是一个 Mapper 接口
2. 运行时 MyBatis 会为这个接口生成实现类
3. Service 可以直接注入它

## 6. 插入用户

Mapper 方法：

```java
@Insert("insert into users(name, age) values (#{name}, #{age})")
@Options(useGeneratedKeys = true, keyProperty = "id")
int insert(UserInfo userInfo);
```

理解：

1. `@Insert` 里写的是 SQL
2. `#{name}` 从 `userInfo.getName()` 取值
3. `#{age}` 从 `userInfo.getAge()` 取值
4. 返回值 `int` 表示影响了几行

### 主键回填

```java
@Options(useGeneratedKeys = true, keyProperty = "id")
```

作用：

1. 数据库生成自增 id
2. MyBatis 把生成的 id 回填到 `userInfo.id`
3. 所以创建成功后返回里可以看到 id

## 7. 查询用户列表

Mapper 方法：

```java
@Select("select id, name, age from users")
List<UserInfo> findAll();
```

理解：

1. 查询多行数据
2. MyBatis 把每一行映射成一个 `UserInfo`
3. 最终返回 `List<UserInfo>`

## 8. 根据 id 查询用户

Mapper 方法：

```java
@Select("select id, name, age from users where id = #{id}")
UserInfo findById(Long id);
```

理解：

1. `#{id}` 对应方法参数 `id`
2. 查询到数据时返回 `UserInfo`
3. 查询不到时返回 `null`

Service 里要处理查不到：

```java
UserInfo userInfo = userMapper.findById(id);

if (userInfo == null) {
    throw new RuntimeException("user not found");
}
```

## 9. 删除用户

Mapper 方法：

```java
@Delete("delete from users where id = #{id}")
int deleteById(Long id);
```

理解：

1. 删除成功返回影响行数
2. 删除存在的 id，通常返回 `1`
3. 删除不存在的 id，返回 `0`

Service 里要判断：

```java
int rows = userMapper.deleteById(id);
if (rows == 0) {
    throw new RuntimeException("user not found");
}
```

## 10. 更新用户

Mapper 方法：

```java
@Update("update users set name = #{name}, age = #{age} where id = #{id}")
int update(UserInfo userInfo);
```

理解：

1. `name` 和 `age` 是要更新的新值
2. `id` 是要更新哪一行
3. 所以 Service 里必须把路径里的 id 设置到 `UserInfo`

关键代码：

```java
userInfo.setId(id);
userInfo.setName(userUpdateDTO.getName());
userInfo.setAge(userUpdateDTO.getAge());
```

如果忘了 `setId(id)`，SQL 里的 `#{id}` 就拿不到正确值。

## 11. 当前完整请求

### 创建用户

```http
POST http://localhost:8081/users
Content-Type: application/json

{
  "name": "huan",
  "age": 18
}
```

### 查询用户列表

```http
GET http://localhost:8081/users
```

### 根据 id 查询用户

```http
GET http://localhost:8081/users/2
```

### 更新用户

```http
PUT http://localhost:8081/users/2
Content-Type: application/json

{
  "name": "updated",
  "age": 20
}
```

### 删除用户

```http
DELETE http://localhost:8081/users/2
```

## 12. 今天踩到的坑

### 路径结尾斜杠

当前接口是：

```text
/users
```

不是：

```text
/users/
```

在当前配置下，多一个结尾斜杠可能导致 `404`。

### 请求方法错误

如果接口是：

```java
@DeleteMapping("/users/{id}")
```

就必须发：

```http
DELETE /users/{id}
```

如果用 `GET` 请求删除接口，会出现方法不匹配。

### 路径冲突

不能同时写：

```java
@GetMapping("/users/{name}")
@GetMapping("/users/{id}")
```

因为它们对 Spring 来说路径结构一样，都会匹配 `/users/1`。

### 数据库工具和应用配置不是同一个东西

IDEA Database 面板不会自动读取 Spring Boot 的 `application.yml`。

所以：

- Spring Boot 连接数据库看 `application.yml`
- IDEA Database 面板要单独配置连接

## 13. 当前已完成能力

到这里，用户模块已经完成基础 CRUD：

1. 新增用户
2. 查询用户列表
3. 根据 id 查询用户
4. 更新用户
5. 删除用户

当前主链路已经是：

```text
HTTP 请求 -> Controller -> Service -> Mapper -> MySQL -> 返回 JSON
```

这条链路是 Spring Boot 后端开发的核心主线之一。

## 14. 下一步学习方向

下一步建议进入：

1. 把 `GET /users` 也改成统一 `Result<List<UserInfo>>`
2. 学标准参数校验：`@Valid`、`@NotBlank`、`@Min`
3. 学分页查询和条件查询

其中最建议先做第 1 个，因为它能让返回结构彻底统一。
