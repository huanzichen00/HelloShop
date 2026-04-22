# SpringBoot Hello 电商系统 V1 目录结构设计说明

## 1. 目标

这份文档用于明确电商系统第一版的目录结构、命名方式和第一批核心类的大致分布。

当前设计目标不是追求“最复杂”的架构，而是：

- 结构清楚
- 符合你当前已经掌握的 Spring Boot 分层思路
- 先适合单体开发
- 后面便于演进到 Spring Cloud 微服务

## 2. 第一版总体结构

当前主包建议仍保留在现有项目下，但业务包逐步向电商领域过渡。

建议结构：

```text
src/main/java/com/huanzichen/springboothello
├── common
├── config
├── controller
├── dto
│   ├── auth
│   ├── cart
│   ├── category
│   ├── order
│   ├── product
│   └── user
├── exception
├── interceptor
├── mapper
├── model
├── service
└── SpringbootHelloApplication.java
```

## 3. 为什么当前不建议直接按微服务拆目录

比如一开始不要急着把当前仓库拆成：

```text
user-service
product-service
order-service
gateway-service
```

因为当前阶段最重要的是先立住业务主线，而不是先承担多服务调试和治理成本。

所以第一版更合适的是：

**单体分层结构 + 清晰的业务上下文边界**

## 4. 各目录职责

### `common`

放通用对象，例如：

- `Result`
- `PageResult`
- `ErrorCode`

### `config`

放配置类，例如：

- `WebMvcConfig`
- Swagger 配置
- 配置属性类

### `controller`

第一版预计至少会有：

- `AuthController`
- `CategoryController`
- `ProductController`
- `CartController`
- `OrderController`

### `dto`

DTO 建议按模块拆分子目录：

- `dto/auth`
- `dto/category`
- `dto/product`
- `dto/cart`
- `dto/order`
- `dto/user`

### `exception`

放统一异常相关类。

### `interceptor`

当前鉴权仍沿用拦截器方案，继续放：

- `LoginInterceptor`

### `mapper`

第一版预计至少会有：

- `UserMapper`
- `CategoryMapper`
- `ProductMapper`
- `CartItemMapper`
- `OrderMapper`
- `OrderItemMapper`

### `model`

第一版预计至少会有：

- `UserInfo`
- `Category`
- `Product`
- `CartItem`
- `Order`
- `OrderItem`

### `service`

第一版预计至少会有：

- `AuthService`
- `CategoryService`
- `ProductService`
- `CartService`
- `OrderService`

## 5. 为什么 DTO 要按模块拆

因为电商第一版很快就会出现很多 DTO：

- 登录 DTO
- 注册 DTO
- 商品查询 DTO
- 购物车新增 DTO
- 购物车修改 DTO
- 订单创建 DTO

如果全部平铺在一个目录下，后面会很乱。

## 6. 面向未来微服务的上下文边界

虽然当前还是单体，但建议从命名和职责上就保持边界清楚：

### 认证用户上下文

- `AuthController`
- `AuthService`
- `UserMapper`

### 商品目录上下文

- `CategoryController`
- `ProductController`
- `CategoryService`
- `ProductService`

### 购物车上下文

- `CartController`
- `CartService`

### 订单上下文

- `OrderController`
- `OrderService`
- `OrderItemMapper`

这样后面拆 Spring Cloud 时，会更自然。

## 7. Mapper 设计建议

### 简单固定 SQL

继续用注解版 Mapper。

### 复杂分页和条件查询

继续用 XML 动态 SQL。

例如：

- 商品列表分页筛选
- 订单列表分页筛选

## 8. 命名建议

### controller

- 一个模块一个 `XxxController`

### service

- 一个模块一个 `XxxService`

### mapper

- 一个表或一个核心资源一个 `XxxMapper`

### DTO

尽量用动作或用途命名，例如：

- `ProductQueryDTO`
- `CartItemAddDTO`
- `CartItemUpdateDTO`
- `OrderCreateDTO`

## 9. 当前阶段最推荐的代码组织原则

当前先不要为抽象而抽象。

更重要的是：

1. 让类职责清楚
2. 让模块边界稳定
3. 让后续扩展不需要大改目录

如果后面真的进入微服务阶段，再从这些边界清楚的单体模块中拆服务，成本会低很多。
