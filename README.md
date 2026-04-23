# springboot-hello

## 项目简介

这是一个基于 Spring Boot 的后端练习项目，用于系统学习并实现常见后端开发能力，包括接口开发、参数校验、统一返回结构、全局异常处理、MyBatis 持久层、事务管理、分页查询和多条件动态查询。

当前项目已经从最初的单接口练习，逐步扩展为包含 `users` 和 `tasks` 两个业务模块的多模块后端练习项目。

## 技术栈

- Java 17
- Spring Boot
- Maven
- MyBatis
- MySQL
- Redis
- RabbitMQ
- Jakarta Validation

## 已完成功能

### 用户模块

- 用户创建、查询、更新、删除
- 按 id 查询单个用户
- 按姓名模糊查询
- 分页查询
- 条件分页 + 排序查询
- 查询参数 DTO 收口
- 参数校验
- 统一返回结构
- 全局异常处理
- 查询当前登录用户信息
- 修改当前登录用户资料
- 查询当前登录用户自己的任务列表

### 任务模块

- 任务创建、查询、更新、删除
- 按 id 查询单个任务
- 按标题、状态、用户 id 条件分页查询
- 当前登录用户任务状态统计
- 任务状态变更记录
- 查询任务状态变更记录列表
- 参数校验
- 状态合法性校验
- 任务完成时间 `completedAt` 自动维护
- `TODO` / `DONE` 状态切换时自动更新完成时间
- 登录后只允许查看自己的任务列表
- 登录后只允许查看、更新、删除自己的任务
- 创建和更新任务时，归属信息不再由前端随意传入

### 模块关联

- 根据用户查询任务列表
- 根据任务查询所属用户

### 鉴权与权限

- `POST /auth/login` 使用 `userId` 模拟登录并签发 token
- 使用拦截器保护 `/tasks/**`
- 通过 `Authorization: Bearer <token>` 识别当前登录用户
- 使用 `UserContext` 保存当前请求用户 id
- 任务模块实现了基础数据权限控制
- 已升级为用户名密码登录 + BCrypt + JWT
- logout 通过 Redis 黑名单让 JWT 真正失效

### 电商模块

- 分类列表、商品列表、商品详情
- 购物车增删改查
- 下单、订单列表、订单详情
- 取消订单并恢复库存
- 支付订单状态流转
- `order_items` 保存商品快照

### Redis 与消息队列

- 商品详情缓存
- 热门商品列表缓存
- 商品更新后主动删除缓存
- 下单防重复提交
- 下单成功后发送 `order.created` 消息
- RabbitMQ 消费端处理订单创建后的异步逻辑

### 基础能力

- `@ConfigurationProperties` 配置绑定
- `@Valid` 参数校验
- `BusinessException` 业务异常封装
- `ErrorCode` 错误码统一管理
- `Result<T>` / `PageResult<T>` 统一响应结构
- `@Transactional` 事务回滚实验
- MyBatis XML 动态 SQL
- 多条件动态 `where`
- 排序白名单控制

## 项目结构

```text
src/main/java/com/huanzichen/springboothello
├── common
│   ├── ErrorCode.java
│   ├── PageResult.java
│   └── Result.java
├── config
│   └── ProjectProperties.java
├── controller
│   ├── AuthController.java
│   ├── DemoController.java
│   ├── HelloController.java
│   ├── TaskController.java
│   └── UserController.java
├── dto
│   ├── LoginDTO.java
│   ├── TaskCreateDTO.java
│   ├── TaskQueryDTO.java
│   ├── TaskUpdateDTO.java
│   ├── UserCreateDTO.java
│   ├── UserQueryDTO.java
│   └── UserUpdateDTO.java
├── exception
│   ├── BusinessException.java
│   ├── CheckedBusinessException.java
│   └── GlobalExceptionHandler.java
├── interceptor
│   └── LoginInterceptor.java
├── mapper
│   ├── TaskMapper.java
│   └── UserMapper.java
├── model
│   ├── TaskInfo.java
│   └── UserInfo.java
└── service
    ├── AuthService.java
    ├── TaskService.java
    └── UserService.java
```

## 核心接口示例

### 用户模块

- `POST /users`
- `GET /users`
- `GET /users/{id}`
- `PUT /users/{id}`
- `DELETE /users/{id}`
- `GET /users/search-page?name=huan&minAge=18&maxAge=30&page=1&size=5&sort=id&order=asc`
- `GET /users/{id}/tasks`

### 任务模块

- `POST /tasks`
- `GET /tasks`
- `GET /tasks/status-summary`
- `GET /tasks/{id}/status-logs`
- `GET /tasks/{id}`
- `PUT /tasks/{id}`
- `DELETE /tasks/{id}`
- `GET /tasks/search-page?title=spring&status=TODO&userId=3&page=1&size=5`
- `GET /tasks/{id}/user`

### 鉴权模块

- `POST /auth/register`
- `POST /auth/login`
- `GET /auth/me`
- `POST /auth/logout`
- `PUT /users/me`
- `GET /users/me/tasks`

### 电商模块

- `GET /categories`
- `GET /products`
- `GET /products/hot`
- `GET /products/{id}`
- `PUT /products/{id}`
- `GET /cart`
- `POST /cart/items`
- `PUT /cart/items/{id}`
- `DELETE /cart/items/{id}`
- `POST /orders`
- `GET /orders`
- `GET /orders/{id}`
- `PUT /orders/{id}/cancel`
- `PUT /orders/{id}/pay`

## 统一返回示例

### 成功返回

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "id": 1,
    "title": "learn spring transaction",
    "description": "finish task module",
    "status": "TODO",
    "userId": 3,
    "completedAt": null
  }
}
```

### 分页返回

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "total": 1,
    "list": [
      {
        "id": 1,
        "title": "learn spring transaction",
        "description": "finish task module",
        "status": "TODO",
        "userId": 3
      }
    ],
    "page": 1,
    "size": 5,
    "totalPages": 1
  }
}
```

### 异常返回

```json
{
  "code": 404,
  "message": "task not found",
  "data": null
}
```

## 当前学习重点

当前重点不是继续堆零散接口，而是把项目逐步从“功能练习”推进成“可展示的小型后端项目”，包括：

- 模块结构整理
- README 和项目说明完善
- 用户模块与任务模块协同
- 登录鉴权与数据权限控制
- JWT 与 Redis 黑名单登出
- Redis 缓存与下单防重复提交
- RabbitMQ 最小异步链路
- 进一步向项目化改造推进

## 后续计划

- 继续完善任务模块
- 梳理 demo/实验接口与正式业务接口边界

## 电商系统改造计划

当前项目下一阶段不再继续围绕 `users + tasks` 单纯堆练习接口，而是准备升级成一个电商后端学习项目。

当前已整理的第一版文档：

- [ECOMMERCE_V1_DESIGN.md](/Users/huanzichen/interview/SpringBoot/springboot-hello/ECOMMERCE_V1_DESIGN.md)
- [ECOMMERCE_V1_STRUCTURE.md](/Users/huanzichen/interview/SpringBoot/springboot-hello/ECOMMERCE_V1_STRUCTURE.md)
- [ECOMMERCE_V1_SCHEMA.md](/Users/huanzichen/interview/SpringBoot/springboot-hello/ECOMMERCE_V1_SCHEMA.md)
- [ECOMMERCE_V1_API.md](/Users/huanzichen/interview/SpringBoot/springboot-hello/ECOMMERCE_V1_API.md)
- [ECOMMERCE_V1_IMPLEMENTATION_ORDER.md](/Users/huanzichen/interview/SpringBoot/springboot-hello/ECOMMERCE_V1_IMPLEMENTATION_ORDER.md)

当前改造原则：

- 第一版先做模块化单体，不直接上微服务
- 主线固定为：认证、商品、购物车、订单
- 在单体里提前保持未来 Spring Cloud 可拆分的上下文边界
- 增加项目说明和接口说明
- 准备作为暑期实习阶段的后端练习项目
