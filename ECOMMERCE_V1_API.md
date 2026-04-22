# SpringBoot Hello 电商系统 V1 接口设计说明

## 1. 目标

这份文档用于明确电商系统第一版到底先做哪些接口、为什么先做这些接口，以及哪些能力先放到第二批。

核心原则仍然是：

**先把最小业务闭环做稳，不先追求接口数量。**

## 2. 接口分类

第一版接口分成 5 类：

### 1. 用户认证类

用于建立登录体系。

### 2. 当前用户类

用于识别当前登录用户。

### 3. 商品浏览类

用于让用户浏览分类和商品。

### 4. 购物车类

用于让用户维护待购买商品。

### 5. 订单类

用于完成一次购买行为和订单查看。

## 3. 第一批必须做的接口

### 1. `POST /auth/register`

作用：

- 创建用户

### 2. `POST /auth/login`

作用：

- 登录并返回 token

### 3. `GET /auth/me`

作用：

- 获取当前登录用户

### 4. `GET /categories`

作用：

- 查询分类列表

### 5. `GET /products`

作用：

- 查询商品列表
- 支持分页
- 支持按分类筛选

### 6. `GET /products/{id}`

作用：

- 查询商品详情

### 7. `GET /cart/items`

作用：

- 查询当前用户购物车

### 8. `POST /cart/items`

作用：

- 加入购物车

### 9. `PUT /cart/items/{id}`

作用：

- 修改购物车项数量或选中状态

### 10. `DELETE /cart/items/{id}`

作用：

- 删除购物车项

### 11. `POST /orders`

作用：

- 从当前用户购物车创建订单

### 12. `GET /orders`

作用：

- 查询我的订单列表

### 13. `GET /orders/{id}`

作用：

- 查询我的订单详情

## 4. 第二批再做的接口

### 1. `PUT /users/me`

作用：

- 修改当前用户资料

### 2. 商品后台管理接口

例如：

- `POST /admin/products`
- `PUT /admin/products/{id}`
- `DELETE /admin/products/{id}`

### 3. 分类后台管理接口

### 4. `POST /orders/{id}/cancel`

作用：

- 取消订单

### 5. 商品评价接口

### 6. 收藏商品接口

### 7. 地址和支付接口

## 5. 第一版接口顺序背后的原则

接口推进顺序遵守：

```text
先身份
再内容
再过程
最后结果
```

也就是：

### 第一步

系统先知道是谁在访问。

### 第二步

再让用户有内容可浏览。

### 第三步

再让用户把商品放进购物车。

### 第四步

最后才把购物车转成订单。

## 6. 第一版最小闭环接口顺序

推荐顺序：

1. `POST /auth/register`
2. `POST /auth/login`
3. `GET /auth/me`
4. `GET /categories`
5. `GET /products`
6. `GET /products/{id}`
7. `GET /cart/items`
8. `POST /cart/items`
9. `PUT /cart/items/{id}`
10. `DELETE /cart/items/{id}`
11. `POST /orders`
12. `GET /orders`
13. `GET /orders/{id}`

## 7. 关键请求对象建议

### `RegisterDTO`

- `username`
- `password`
- `nickname`

### `LoginDTO`

- `username`
- `password`

### `ProductQueryDTO`

- `categoryId`
- `keyword`
- `page`
- `size`
- `sort`
- `order`

### `CartItemAddDTO`

- `productId`
- `quantity`

### `CartItemUpdateDTO`

- `quantity`
- `selected`

### `OrderCreateDTO`

第一版可以有两种方案：

#### 方案 A

不接收请求体，默认将当前用户购物车中所有 `selected = true` 的商品下单。

#### 方案 B

接收购物车项 id 列表，只对指定项下单。

当前更推荐方案 A，因为更简单，适合第一版。

## 8. 第一版接口权限要求

### 公共接口

- `POST /auth/register`
- `POST /auth/login`
- `GET /categories`
- `GET /products`
- `GET /products/{id}`

### 需要登录

- `GET /auth/me`
- `GET /cart/items`
- `POST /cart/items`
- `PUT /cart/items/{id}`
- `DELETE /cart/items/{id}`
- `POST /orders`
- `GET /orders`
- `GET /orders/{id}`

### 数据权限

即使用户已经登录，也只能：

- 查看自己的购物车
- 修改自己的购物车项
- 查看自己的订单

## 9. 为什么订单创建接口很关键

`POST /orders`` 是第一版最值得讲的接口之一，因为它不再是简单 CRUD，而是一个真正的业务流程接口。

它至少会包含这些步骤：

1. 取当前用户
2. 读取已选中的购物车项
3. 校验购物车是否为空
4. 校验商品是否存在且上架
5. 校验库存
6. 生成订单主表
7. 生成订单项明细
8. 扣减库存
9. 删除或清理已下单购物车项

这条接口很适合后面练事务。

## 10. 面向微服务的接口边界意识

虽然第一版还是单体，但接口分组最好已经有未来边界意识：

- `auth/*` 更接近未来 `user-service`
- `categories/*`、`products/*` 更接近未来 `product-service`
- `cart/*` 更接近未来 `cart-service`
- `orders/*` 更接近未来 `order-service`

这样后面拆 Spring Cloud 时更自然。
