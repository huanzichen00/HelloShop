# Music Review Site 重写版第一版目录结构设计说明

## 1. 目标

这份文档用于明确 `music-review-site` 重写版第一版后端项目的目录结构、命名方式和第一批核心类的大致分布。

当前设计目标不是追求“最复杂、最抽象”的架构，而是：

- 结构清楚
- 容易扩展
- 符合你当前已经掌握的 Spring Boot 分层思路
- 后面写代码时不容易越写越乱

## 2. 第一版总体结构

建议项目主包先固定成：

```text
com.huanzichen.musicreview
```

推荐第一版目录结构：

```text
src/main/java/com/huanzichen/musicreview
├── common
├── config
├── controller
├── dto
│   ├── album
│   ├── artist
│   ├── auth
│   ├── review
│   └── user
├── exception
├── mapper
├── model
├── security
├── service
└── MusicReviewApplication.java
```

这套结构的核心思想是：

- 总体继续按分层组织
- DTO 再按模块细分
- 鉴权相关内容单独抽到 `security`

## 3. 为什么当前不建议按模块整套拆分

比如一开始不要用这种结构：

```text
user/controller
user/service
user/mapper
album/controller
album/service
album/mapper
```

这类按 feature 完整拆分的结构当然能用，但对当前阶段不够友好。

原因是：

- 你现在已经熟悉的是分层结构
- 第一版模块虽然已经有 4 个，但体量还不至于复杂到必须按 feature-first 拆
- 当前最重要的是稳定推进，不是为抽象而抽象

所以第一版更合适的是：

**总体分层 + DTO 按模块分目录**

## 4. 各目录职责

### `common`

放全项目通用基础对象，例如：

- `Result`
- `PageResult`
- `ErrorCode`

如果后面有分页参数工具类或统一常量，也可以放这里。

### `config`

放配置相关类，例如：

- `WebMvcConfig`
- Swagger 配置
- Redis 配置
- 跨域配置

### `controller`

放控制器类，第一版预计至少会有：

- `AuthController`
- `UserController`
- `ArtistController`
- `AlbumController`
- `ReviewController`

### `dto`

放请求和响应 DTO。

当前建议按模块再分子目录，例如：

```text
dto/auth
dto/user
dto/artist
dto/album
dto/review
```

这样做的好处是：

- 模块一多不会乱
- 接口相关对象更容易定位
- 后续继续扩展时不需要重构整个 DTO 包

### `exception`

放统一异常相关类，例如：

- `BusinessException`
- `GlobalExceptionHandler`

### `mapper`

放 MyBatis mapper 接口。

第一版预计会有：

- `UserMapper`
- `ArtistMapper`
- `AlbumMapper`
- `ReviewMapper`

### `model`

放数据库实体/模型对象，例如：

- `User`
- `Artist`
- `Album`
- `Review`

如果后面有中间表或日志表，也继续放这里。

### `security`

放鉴权和登录态相关内容，例如：

- `JwtUtil`
- `LoginInterceptor`
- `UserContext`

如果后面还有认证相关辅助类，也优先放这里。

### `service`

放业务逻辑类，例如：

- `AuthService`
- `UserService`
- `ArtistService`
- `AlbumService`
- `ReviewService`

## 5. DTO 包为什么要按模块拆

因为第一版开始以后，很快就会出现：

- 注册 DTO
- 登录 DTO
- 专辑创建 DTO
- 专辑响应 DTO
- 评论创建 DTO
- 评论响应 DTO

如果全部平铺在一个 `dto` 目录下，文件一多会很乱。

所以建议一开始就拆成：

- `dto/auth`
- `dto/user`
- `dto/artist`
- `dto/album`
- `dto/review`

这是当前阶段“成本很低但收益很高”的结构优化。

## 6. 第一版每个模块大致会有哪些类

### `auth`

预计会有：

- `AuthController`
- `AuthService`
- `dto/auth/RegisterDTO`
- `dto/auth/LoginDTO`

### `user`

预计会有：

- `UserController`
- `UserService`
- `UserMapper`
- `model/User`
- `dto/user/UserProfileUpdateDTO`
- 可能还有当前用户返回对象

### `artist`

预计会有：

- `ArtistController`
- `ArtistService`
- `ArtistMapper`
- `model/Artist`
- `dto/artist/ArtistResponse`

### `album`

预计会有：

- `AlbumController`
- `AlbumService`
- `AlbumMapper`
- `model/Album`
- `dto/album/AlbumResponse`

### `review`

预计会有：

- `ReviewController`
- `ReviewService`
- `ReviewMapper`
- `model/Review`
- `dto/review/ReviewCreateDTO`
- `dto/review/ReviewResponse`

## 7. 当前命名建议

### controller

- 一个模块一个 `XxxController`

### service

- 一个模块一个 `XxxService`

### mapper

- 一个表一个 `XxxMapper`

### model

- 用业务名词单数形式：
  - `User`
  - `Artist`
  - `Album`
  - `Review`

### DTO

请求和响应对象命名要体现用途，例如：

- `RegisterDTO`
- `LoginDTO`
- `UserProfileUpdateDTO`
- `ReviewCreateDTO`
- `AlbumResponse`

## 8. 第一版为什么暂时不单独拆 `vo` 包

技术上当然可以拆：

- `dto`
- `vo`
- `query`
- `command`

但当前阶段没有必要。

因为：

- 你现在正在建立的是稳定的项目开发主线
- 模块数量还可控
- 过早拆太多概念，反而会增加理解成本

所以第一版统一先用 `dto` 承担请求/响应对象，是更务实的方案。

## 9. 当前结构设计原则

这份结构说明背后最核心的原则只有一句：

**结构要服务于稳定推进，而不是服务于抽象上的“最优雅”。**

所以第一版优先选择的是：

- 你已经熟悉的分层结构
- 局部做一点升级，例如 DTO 按模块拆
- 把鉴权相关内容单独抽到 `security`

这会比一开始就追求大而全的架构更适合当前重写任务。

## 10. 当前结论

后面真正开始重写 `music-review-site` 第一版后端时，可以直接按这份目录结构作为基线。

当前最合理的方案是：

- 后端继续使用 Spring Boot + MyBatis
- 结构总体按分层组织
- DTO 按模块分目录
- 安全相关内容单独放在 `security`

这套结构足够支撑第一版最小闭环，也足够为后面继续扩展预留空间。
