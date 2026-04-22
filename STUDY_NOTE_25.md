# Study Note 25

## 主题

阶段复盘：`music-review-site` 现有项目里你还没系统学过的技术栈

## 1. 这节课解决什么问题

你已经明确说了，后面想重写：

`music-review-site`

而且不是直接硬上，而是希望先看清楚：

- 这个项目用了什么技术栈
- 哪些是你现在已经学过的
- 哪些是你现在还没系统学过的

这一步很重要，因为它决定了后面重写时到底是：

- 直接照着重写
- 还是先拆技术点、逐个补齐再重写

## 2. 项目整体结构

当前仓库是一个前后端分离项目，主要分成三块：

- `backend`
- `frontend`
- `database`

说明它已经不是一个单纯的 Spring Boot 单体后端练习仓库，而是一个完整网站项目。

## 3. 后端主要技术栈

从当前结构和代码看，后端主要用了这些：

### 1. Spring Boot

这个你已经在当前学习项目里系统练过了。

### 2. Spring Data JPA

项目里有大量：

```text
repository
```

而不是你现在一直在用的 `MyBatis Mapper`。

这说明这个项目的数据库访问主线是：

**Spring Data JPA / Hibernate**

这块你现在还没有系统学过。

### 3. Spring Security

项目里有：

- `SecurityConfig`
- `JwtAuthenticationFilter`
- `UserDetailsServiceImpl`
- `UserDetailsImpl`

这说明它不是你现在这种“自写拦截器 + JWT”的最小鉴权，而是已经用上了：

**Spring Security 的用户认证体系**

这块你目前也还没有系统学过。

### 4. JWT

这个你现在已经学过，而且在当前项目里已经自己接过一版。

### 5. 文件上传

后端有：

- `FileController`
- `uploads/`

这说明项目里有文件上传或头像上传能力。

这块你现在还没有在当前学习项目里系统做过。

### 6. 复杂业务模块

后端模块已经明显超过简单用户和任务：

- artist
- album
- genre
- review
- favorite
- notification
- blog
- question bank
- guess band online

这说明它是一个业务模块很多、实体关系更复杂的项目。

## 4. 前端主要技术栈

### 1. React

前端目录里是：

- `src/pages`
- `src/components`
- `src/context`

说明前端主线是 React。

### 2. Vite

有：

- `vite.config.js`

说明前端是用 Vite 启动和构建的。

### 3. Axios

有：

- `src/api/axios.js`

说明前端通过 Axios 调后端接口。

### 4. Context

有：

- `AuthContext`
- `ThemeContext`

说明它用了 React Context 管理登录状态和主题状态。

### 5. Playwright

有：

- `playwright.config.js`
- `tests/guess-band.spec.js`

说明项目里有前端端到端测试。

这块你当前完全还没系统学。

## 5. 数据库和脚本层

`database` 目录里有很多：

- 初始化 SQL
- 补字段 SQL
- 数据同步脚本
- 数据导出脚本

包括：

- `init.sql`
- `seed_public.sql`
- Python 脚本
- MusicBrainz 数据同步脚本

这说明这个项目不只是“建表”，还涉及：

- 数据种子
- 数据迁移
- 外部数据同步

这部分你现在也还没系统碰。

## 6. 你现在已经学过、能迁移过去的部分

这些是你现在已经打过底的：

- Spring Boot 基础分层
- 参数校验
- 统一返回
- 全局异常处理
- JWT 基础
- Redis 基础接入
- 当前用户主线
- 数据权限控制
- Swagger
- 一些基础测试思路

也就是说：

`music-review-site`

不是完全陌生，你已经有一部分底座了。

## 7. 你现在还没系统学过、重写前必须正视的部分

如果按“还没系统学过”的角度，最关键的是：

### 后端

1. Spring Data JPA / Hibernate
2. Spring Security
3. 文件上传
4. 更复杂的实体关系设计
5. 多模块业务拆分

### 前端

1. React 项目结构
2. Axios + 前后端联调
3. Context 状态管理
4. Playwright 测试

### 数据与工程化

1. SQL 初始化与 seed 数据
2. 数据同步脚本
3. 更完整的数据库演进方式

## 8. 对“从零重写”的现实判断

如果你现在立刻完全照着它重写，风险很高。

因为这不是单一技术点问题，而是：

- 后端栈切换到 JPA / Spring Security
- 前端栈还是完整 React 网站
- 业务模块远多于你当前学习项目

所以更合理的重写方式不应该是：

```text
一步到位全量重写
```

而应该是：

```text
先拆出一个最小可重写版本，再逐层补
```

## 9. 更合理的后续路线

如果后面真的要我带你从零重写这个项目，最合理的顺序应该是：

### 第一阶段

只重写后端最小版：

- 用户注册 / 登录
- 艺术家 / 专辑 / 乐评基础模块
- 收藏
- 基础鉴权

### 第二阶段

再补：

- 文件上传
- 通知
- 博客 / 回复

### 第三阶段

最后再碰：

- Guess Band Online
- Question Bank
- 更复杂实时或游戏逻辑

## 10. 当前结论

`music-review-site` 里真正属于“你现在还没系统学过”的核心技术，主要是：

- Spring Data JPA
- Spring Security
- 文件上传
- React 前端结构
- Playwright
- 更复杂的数据脚本和 seed 体系

而你现在已经具备的，是一套比较扎实的 Spring Boot 后端主线底子。

所以后面如果要重写，最优方案不是硬抄现有项目，而是：

**基于你现在已经会的后端能力，先做一个缩小版、可控版的重写路线。**
