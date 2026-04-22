# SpringBoot Hello 电商系统 V1 表设计说明

## 1. 目标

这份文档聚焦电商系统第一版的数据库设计，不讨论支付、物流、优惠券这些第二阶段能力。

第一版当前只围绕 6 张核心表展开：

- `users`
- `categories`
- `products`
- `cart_items`
- `orders`
- `order_items`

它们要支撑的能力只有：

- 注册
- 登录
- 分类列表
- 商品列表
- 商品详情
- 购物车增删改查
- 提交订单
- 查看我的订单

## 2. 设计原则

### 1. 先做最小闭环

只保留当前业务真正需要的字段，不把地址、支付、发货、优惠券一次塞进来。

### 2. 区分内容、过程、结果三类资源

- `categories`、`products` 是内容资源
- `cart_items` 是过程资源
- `orders`、`order_items` 是结果资源

这个划分的作用是让表职责更清楚：

- 商品负责“卖什么”
- 购物车负责“准备买什么”
- 订单负责“已经买了什么”

### 3. 一对多关系里，外键放在“多”的一侧

所以：

- `products.category_id`
- `cart_items.user_id`
- `cart_items.product_id`
- `orders.user_id`
- `order_items.order_id`

### 4. 购物车表只保存当前状态，不保存历史

`cart_items` 是临时过程数据。下单完成后，对应购物车项可以删除，不需要把它当历史记录长期保留。

### 5. 订单表和订单项表必须拆开

一笔订单天然会包含多个商品，所以：

- `orders` 保存订单主信息
- `order_items` 保存订单明细

不能把多个商品硬塞进 `orders` 一张表里。

### 6. 订单项必须保留快照字段

`order_items` 里不只存 `product_id`，还要保存：

- `product_name`
- `product_price`
- `product_cover_url`

原因是订单是交易结果，不能被后续商品改名、改价影响。

### 7. 订单号和主键 id 分离

`orders.id` 适合数据库内部关联，`orders.order_no` 适合对外展示、搜索和后续支付链路关联。

## 3. 六张表各自解决什么问题

### `users`

解决“谁在下单、谁的购物车、谁的订单”。

### `categories`

解决商品如何分类和组织。

### `products`

解决用户到底能浏览和购买什么商品。

### `cart_items`

解决“当前用户准备买哪些商品、每件买多少、是否选中”。

### `orders`

解决“一次下单行为”的主记录。

### `order_items`

解决“一笔订单里具体买了哪些商品”的明细记录。

## 4. 各表设计

## 4.1 `users` 表

### 角色定位

用于承载注册、登录、当前用户识别，以及购物车和订单归属。

### 当前项目实际字段

- `id`
- `username`
- `password`
- `name`
- `age`
- `created_at`

### 字段说明

#### `id`

主键。

#### `username`

登录名，唯一。

#### `password`

数据库中存 BCrypt hash。

#### `name`

展示名称。

#### `age`

当前学习项目保留下来的基础资料字段。

#### `created_at`

用户创建时间。

## 4.2 `categories` 表

### 角色定位

用于承载商品分类，帮助商品组织和展示。

### 当前项目实际字段

- `id`
- `name`
- `sort`
- `created_at`

### 字段说明

#### `name`

分类名称。

#### `sort`

排序字段，列表展示时使用。

## 4.3 `products` 表

### 角色定位

商品是电商系统第一版的内容主资源。

### 当前项目实际字段

- `id`
- `category_id`
- `name`
- `description`
- `price`
- `stock`
- `status`
- `cover_url`
- `created_at`
- `updated_at`

### 字段说明

#### `category_id`

商品所属分类。

#### `name`

商品展示名称。

#### `description`

商品描述。

#### `price`

当前销售单价，使用 `DECIMAL(10,2)`。

#### `stock`

当前库存。

#### `status`

商品状态，第一版建议：

- `ON_SALE`
- `OFF_SALE`

#### `cover_url`

商品封面图地址，第一版先用字符串。

## 4.4 `cart_items` 表

### 角色定位

用于承载某个用户当前待购买的商品集合。

### 当前项目实际字段

- `id`
- `user_id`
- `product_id`
- `quantity`
- `selected`
- `created_at`
- `updated_at`

### 字段说明

#### `user_id`

购物车归属用户。

#### `product_id`

对应商品。

#### `quantity`

购买数量，必须大于 0。

#### `selected`

是否选中，便于支持“部分购物车项下单”。

### 约束建议

建议加唯一约束：

```text
(user_id, product_id)
```

避免同一用户购物车里重复出现同一商品多行。

## 4.5 `orders` 表

### 角色定位

用于承载一次下单行为的主记录。

### 第一版建议字段

- `id`
- `order_no`
- `user_id`
- `status`
- `total_amount`
- `total_quantity`
- `created_at`
- `updated_at`

### 字段说明

#### `id`

数据库内部主键，主要用于和 `order_items.order_id` 关联。

#### `order_no`

业务订单号。相比自增 id，更适合对外展示和后续支付链路扩展。

#### `user_id`

订单归属用户。

#### `status`

第一版建议的最小状态集：

- `PENDING_PAYMENT`
- `PAID`
- `CANCELED`

如果你当前版本还没接支付，也可以先只实际用到 `PENDING_PAYMENT` 和 `CANCELED`。

#### `total_amount`

订单总金额，由后端根据订单项计算。

#### `total_quantity`

订单总件数，由后端根据订单项数量合计。

## 4.6 `order_items` 表

### 角色定位

用于承载订单下的每个商品明细。

### 第一版建议字段

- `id`
- `order_id`
- `product_id`
- `product_name`
- `product_price`
- `product_cover_url`
- `quantity`
- `subtotal_amount`
- `created_at`

### 字段说明

#### `order_id`

所属订单 id。

#### `product_id`

来源商品 id，便于追溯商品来源。

这里我建议保留该字段，但允许为 `NULL`，并且第一版先不对它加外键约束。这样做有两个好处：

- 即使后面商品被删除，订单历史依然能完整保留
- 未来如果拆成商品服务和订单服务，订单侧对商品表的直接强依赖也会更容易松开

#### `product_name`

下单时商品名快照。

#### `product_price`

下单时单价快照。

#### `product_cover_url`

下单时商品封面图快照。

#### `quantity`

购买数量。

#### `subtotal_amount`

该订单项小计。

## 5. 核心表关系

### `categories` 对 `products`

- 一个分类对应多个商品
- 外键在 `products.category_id`

### `users` 对 `cart_items`

- 一个用户有多条购物车项
- 外键在 `cart_items.user_id`

### `products` 对 `cart_items`

- 一个商品可以出现在多个用户购物车中
- 外键在 `cart_items.product_id`

### `users` 对 `orders`

- 一个用户有多个订单
- 外键在 `orders.user_id`

### `orders` 对 `order_items`

- 一个订单有多个订单项
- 外键在 `order_items.order_id`

## 6. 为什么订单一定要做快照

这是第一版数据库设计里最值得记住的点之一。

如果 `order_items` 只存 `product_id`，不存 `product_name` 和 `product_price`，会出现两个问题：

### 1. 商品改名后，老订单会被“篡改”

用户买的时候商品名叫“iPhone 15 128G”，后来商品运营把名字改成“iPhone 15 新版”。如果订单详情总是实时查 `products.name`，老订单看到的就不是当时买的那个名字了。

### 2. 商品改价后，老订单金额解释不清

用户下单时单价是 `4999.00`，后来商品涨价到 `5499.00`。如果订单详情不保存下单时价格，历史订单就无法正确还原。

所以订单项快照的核心原则是：

**订单是交易结果，商品是当前内容。交易结果不能依赖当前内容实时回填。**

## 7. 索引设计

数据库设计不只是把字段和外键列出来，索引设计同样重要。第一版索引的原则是：

- 先围绕真实查询路径建索引
- 不为了“看起来专业”乱加索引
- 唯一约束优先解决数据正确性
- 普通索引优先服务最常见查询

## 7.1 `cart_items` 的索引

### 唯一索引 `uk_cart_user_product (user_id, product_id)`

作用有两个：

- 保证同一用户购物车里同一商品只能出现一行
- 支持“先查是否已存在，再决定新增还是累加”的业务逻辑

为什么不是只给 `user_id` 或 `product_id` 单独建唯一索引？

- `user_id` 单独唯一会导致一个用户只能有一条购物车记录，显然不对
- `product_id` 单独唯一会导致一个商品只能出现在一个人的购物车里，也不对

所以必须是联合唯一键 `(user_id, product_id)`。

## 7.2 `orders` 的索引

### 唯一索引 `uk_orders_order_no (order_no)`

作用：

- 约束订单号不能重复
- 支持按订单号查单

为什么要单独做 `order_no` 唯一索引，而不是只靠主键 `id`？

- `id` 更偏数据库内部关联
- `order_no` 更偏业务展示和外部链路，比如后续支付、查询、对账

### 普通索引 `idx_orders_user_id (user_id)`

作用：

- 支持按用户查订单

这个索引对应的典型 SQL 是：

```sql
select *
from orders
where user_id = ?
```

### 复合索引 `idx_orders_user_created_at (user_id, created_at)`

作用：

- 支持“查我的订单列表”这类高频场景
- 典型 SQL 往往是先按 `user_id` 过滤，再按 `created_at` 倒序分页

例如：

```sql
select *
from orders
where user_id = ?
order by created_at desc
limit ?, ?
```

为什么写成 `(user_id, created_at)`，而不是 `(created_at, user_id)`？

因为你的过滤条件是先按用户查，再在这个用户自己的订单里按时间排序。复合索引的最左匹配原则决定了，把 `user_id` 放前面更符合实际查询路径。

简单理解就是：

- 先找到“这个用户的订单集合”
- 再在这个集合里按时间取最近几条

## 7.3 `order_items` 的索引

### 普通索引 `idx_order_items_order_id (order_id)`

作用：

- 支持按订单 id 查明细

这是订单详情页最核心的一条查询路径：

```sql
select *
from order_items
where order_id = ?
```

如果没有这个索引，订单详情每次都要扫整张 `order_items` 表。

### 普通索引 `idx_order_items_product_id (product_id)`

作用：

- 支持按商品反查订单项
- 方便后续统计“某个商品出现在多少笔订单里”
- 方便做售卖统计、问题追溯、运营分析

这个索引不是当前下单主链路必须的，但它是一个成本不高、后续扩展价值较高的索引。

## 7.4 为什么没有给每个字段都建索引

这是数据库设计里很容易犯的一个错误。

索引不是越多越好，因为：

- 每多一个索引，插入和更新成本都会上升
- 购物车、订单这些表后面会频繁写入
- 很多字段并不是高频过滤条件

例如：

- `orders.status` 第一版不值得单独建索引，因为当前主要查询路径不是“按状态全表筛订单”
- `order_items.product_name`、`product_cover_url` 这种快照展示字段也不需要建索引
- `cart_items.selected` 第一版也不需要单独建索引，因为它几乎总是跟 `user_id` 一起使用，而且数据量还不大

所以第一版索引策略的核心不是“全都加”，而是：

**只给高频查询条件、排序路径和唯一性约束加索引。**

## 8. 建表顺序建议

推荐顺序：

1. `users`
2. `categories`
3. `products`
4. `cart_items`
5. `orders`
6. `order_items`

这个顺序符合依赖关系。

## 9. 当前版本 SQL 示例

```sql
CREATE TABLE users (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(100) NOT NULL,
    name VARCHAR(50) NOT NULL,
    age INT,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE categories (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(50) NOT NULL,
    sort INT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE products (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    category_id BIGINT NOT NULL,
    name VARCHAR(100) NOT NULL,
    description VARCHAR(500),
    price DECIMAL(10, 2) NOT NULL,
    stock INT NOT NULL,
    status VARCHAR(20) NOT NULL,
    cover_url VARCHAR(255),
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_products_category FOREIGN KEY (category_id) REFERENCES categories(id)
);

CREATE TABLE cart_items (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    quantity INT NOT NULL,
    selected TINYINT(1) NOT NULL DEFAULT 1,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT uk_cart_user_product UNIQUE (user_id, product_id),
    CONSTRAINT fk_cart_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT fk_cart_product FOREIGN KEY (product_id) REFERENCES products(id)
);

CREATE TABLE orders (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    order_no VARCHAR(32) NOT NULL,
    user_id BIGINT NOT NULL,
    status VARCHAR(20) NOT NULL,
    total_amount DECIMAL(10, 2) NOT NULL,
    total_quantity INT NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT uk_orders_order_no UNIQUE (order_no),
    CONSTRAINT fk_orders_user FOREIGN KEY (user_id) REFERENCES users(id),
    INDEX idx_orders_user_id (user_id),
    INDEX idx_orders_user_created_at (user_id, created_at)
);

CREATE TABLE order_items (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    order_id BIGINT NOT NULL,
    product_id BIGINT,
    product_name VARCHAR(100) NOT NULL,
    product_price DECIMAL(10, 2) NOT NULL,
    product_cover_url VARCHAR(255),
    quantity INT NOT NULL,
    subtotal_amount DECIMAL(10, 2) NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_order_items_order FOREIGN KEY (order_id) REFERENCES orders(id),
    INDEX idx_order_items_order_id (order_id),
    INDEX idx_order_items_product_id (product_id)
);
```

## 10. V7 / V8 Migration 示例

### `V7__create_orders_table.sql`

```sql
CREATE TABLE orders (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    order_no VARCHAR(32) NOT NULL,
    user_id BIGINT NOT NULL,
    status VARCHAR(20) NOT NULL,
    total_amount DECIMAL(10, 2) NOT NULL,
    total_quantity INT NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT uk_orders_order_no UNIQUE (order_no),
    CONSTRAINT fk_orders_user FOREIGN KEY (user_id) REFERENCES users(id),
    INDEX idx_orders_user_id (user_id),
    INDEX idx_orders_user_created_at (user_id, created_at)
);
```

### `V8__create_order_items_table.sql`

```sql
CREATE TABLE order_items (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    order_id BIGINT NOT NULL,
    product_id BIGINT,
    product_name VARCHAR(100) NOT NULL,
    product_price DECIMAL(10, 2) NOT NULL,
    product_cover_url VARCHAR(255),
    quantity INT NOT NULL,
    subtotal_amount DECIMAL(10, 2) NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_order_items_order FOREIGN KEY (order_id) REFERENCES orders(id),
    INDEX idx_order_items_order_id (order_id),
    INDEX idx_order_items_product_id (product_id)
);
```

## 11. 第一版刻意不加的表

当前先不加：

- `addresses`
- `payments`
- `shipments`
- `coupons`
- `product_reviews`
- `favorite_products`

这些都属于第二阶段增强，不进入第一版最小闭环。
