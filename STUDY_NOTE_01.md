# Spring Boot 学习笔记 01

## 1. 本节目标

这一节的目标不是写业务，而是先建立对一个最小 Spring Boot 项目的基本认识：

1. 这个项目是怎么创建出来的。
2. `pom.xml` 在做什么。
3. 启动类在做什么。
4. 项目目录结构大概怎么理解。

## 2. 当前项目信息

- 项目名：`springboot-hello`
- 包名：`com.huanzichen.springboothello`
- 构建工具：`Maven`
- 当前 JDK：`25`
- 当前 Spring Boot 版本：`3.5.13`
- 项目源码版本：`17`
- 当前依赖：`Spring Web MVC`

说明：

- 当前机器先继续用 `JDK 25`，不让环境问题阻塞主线学习。
- `pom.xml` 中的 `<java.version>17</java.version>` 表示项目按 Java 17 目标版本构建。
- 这意味着项目代码应尽量按 Java 17 的常见写法学习，不要依赖 25 的新特性。
- 这次把 Spring Boot 从 `4.0.5` 切回 `3.5.13`，原因是入门阶段更稳定、资料更多、测试和依赖更好对齐。

## 3. 当前项目结构理解

当前项目里最重要的几个文件是：

### `pom.xml`

这是 Maven 项目的核心配置文件。

它主要负责：

1. 说明这是一个什么项目。
2. 指定项目依赖哪些库。
3. 指定项目使用什么 Java 版本。
4. 指定构建时要用哪些插件。

### `src/main/java`

这里放正式业务代码，也就是项目运行时真正会用到的代码。

### `src/main/resources`

这里放配置文件、静态资源等内容。

当前已有：

- `application.properties`

### `src/test/java`

这里放测试代码。

当前已有：

- `SpringbootHelloApplicationTests`

## 4. `pom.xml` 逐项理解

当前 `pom.xml` 的核心部分如下：

```xml
<parent>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-parent</artifactId>
    <version>3.5.13</version>
    <relativePath/>
</parent>
```

理解：

1. 这个项目继承了 Spring Boot 提供的父工程。
2. 父工程帮我们统一管理很多依赖版本和默认构建配置。
3. 这也是为什么很多 Spring Boot 项目不需要自己写一堆依赖版本号。

---

```xml
<groupId>com.huanzichen</groupId>
<artifactId>springboot-hello</artifactId>
<version>0.0.1-SNAPSHOT</version>
```

理解：

- `groupId`：项目归属，一般写公司或个人域名反转风格。
- `artifactId`：项目名称。
- `version`：当前项目版本。
- `SNAPSHOT`：通常表示开发中的未正式发布版本。

---

```xml
<properties>
    <java.version>17</java.version>
</properties>
```

理解：

1. 这里指定项目使用的 Java 目标版本。
2. 这是项目配置，不等于你本机安装的 Java 版本本身。
3. 当前你机器用 `JDK 25`，项目目标版本是 `17`，暂时可以继续学习。

---

```xml
<dependencies>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>

    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-test</artifactId>
        <scope>test</scope>
    </dependency>
</dependencies>
```

理解：

1. `dependencies` 表示当前项目依赖的外部库。
2. `spring-boot-starter-web` 让项目具备开发 Web 接口的基础能力。
3. `spring-boot-starter-test` 是测试相关依赖。
4. `<scope>test</scope>` 表示这个依赖只在测试阶段使用。

进一步理解：

- 现在你可以先把它粗略理解成“加了这个依赖，项目才有能力写接口”。
- 至于它内部到底又依赖了什么，现在不需要深究。

---

```xml
<build>
    <plugins>
        <plugin>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-maven-plugin</artifactId>
        </plugin>
    </plugins>
</build>
```

理解：

1. `build` 是构建相关配置。
2. 这里配置了 Spring Boot 的 Maven 插件。
3. 这个插件帮助我们完成打包、运行等构建工作。

## 5. 启动类理解

当前启动类内容是：

```java
@SpringBootApplication
public class SpringbootHelloApplication {

    public static void main(String[] args) {
        SpringApplication.run(SpringbootHelloApplication.class, args);
    }
}
```

先建立最直观的理解：

1. 这是整个 Spring Boot 项目的启动入口。
2. 运行 `main` 方法，就相当于启动这个后端应用。
3. `SpringApplication.run(...)` 会启动 Spring Boot 并初始化项目需要的运行环境。

当前阶段你只需要先记住：

- 普通 Java 程序靠 `main` 方法启动。
- Spring Boot 项目本质上也是从 `main` 方法启动。
- 只是它在 `run(...)` 里帮你做了大量框架初始化工作。

## 6. 配置文件理解

当前配置文件内容：

```properties
spring.application.name=springboot-hello
```

理解：

1. 这是应用名称配置。
2. `application.properties` 是 Spring Boot 默认会读取的配置文件之一。
3. 后面端口号、数据库连接、日志等配置，也常写在这里或 `application.yml` 里。

## 7. 测试类理解

当前测试类：

```java
@SpringBootTest
class SpringbootHelloApplicationTests {

    @Test
    void contextLoads() {
    }
}
```

当前阶段的理解就够了：

1. 这是项目自动生成的基础测试类。
2. `contextLoads()` 可以粗略理解成“测试 Spring 容器能不能正常启动”。
3. 现在不用深究测试细节，先知道项目里为什么会有它。

## 8. 这一节你应该真正掌握什么

不是背 XML，而是掌握下面这些事实：

1. `pom.xml` 是 Maven 项目的核心配置文件。
2. Spring Boot 项目启动入口是启动类里的 `main` 方法。
3. `src/main/java` 放正式代码，`src/main/resources` 放配置，`src/test/java` 放测试。
4. 加 Web starter 之后，项目才具备开发接口的基础能力。
5. 配置文件是后面管理端口、数据库、日志等设置的地方。
6. 同一个接口如果在测试里通过，而浏览器/IDEA 里还报错，优先怀疑运行中的旧进程或旧依赖版本。

## 9. 当前还不需要深究的点

这些词先知道名字就行，不要现在钻进去：

1. 自动配置底层原理
2. 依赖传递的细节
3. Maven 生命周期完整细节
4. 注解底层实现
5. Spring 容器源码

## 10. 下一步学习任务

下一步会进入第一个最小接口：

1. 新建一个 `controller` 包。
2. 写一个最简单的 `HelloController`。
3. 学 `@RestController`
4. 学 `@GetMapping`
5. 在浏览器里访问第一个接口。

## 11. 参数接收初步理解

在当前项目中，已经接触到了两种最基础的参数传递方式。

### 11.1 查询参数

示例：

```java
@GetMapping("/greet")
public String greet(String name) {
    return "hello " + name;
}
```

访问方式：

```text
/greet?name=huanzichen
```

理解：

1. `?name=huanzichen` 是查询参数。
2. Spring 可以把请求里的 `name` 绑定到方法参数 `name`。
3. 如果请求里没有传 `name`，那么这个参数就可能是 `null`。

补充：

- 写成 `String name` 在这个例子里可以工作。
- 写成 `@RequestParam String name` 会更清晰，更容易让别人看懂这是从请求参数取值。

### 11.2 路径参数

示例：

```java
@GetMapping("/users/{name}")
public String getUser(@PathVariable String name) {
    return "user: " + name;
}
```

访问方式：

```text
/users/huanzichen
```

理解：

1. `{name}` 是路径中的一个变量位置。
2. `@PathVariable` 表示从路径里把值取出来。
3. 当访问 `/users/huanzichen` 时，方法参数 `name` 的值就是 `huanzichen`。

### 11.3 两者区别

- `@RequestParam` 更适合接收查询参数。
- `@PathVariable` 更适合接收路径中的资源标识。

直观例子：

- `/users?id=1`：更像“通过条件查询”
- `/users/1`：更像“访问 id 为 1 的用户资源”

当前阶段先形成这个直觉，不要急着背 REST 规范。

## 12. JSON 返回初步理解

当前项目已经验证了“返回对象 -> 自动转 JSON”的行为。

示例：

```java
@GetMapping("/user-info")
public UserInfo userInfo() {
    return new UserInfo("huanzichen", 18);
}
```

返回结果：

```json
{"name":"huanzichen","age":18}
```

理解：

1. Controller 返回普通 Java 对象时，Spring Boot 会帮你把对象序列化成 JSON。
2. 这是 Web 接口最常见的返回方式之一。
3. `@RestController` 的核心意义之一，就是让这种返回更自然。

## 13. `POST /users` 排障记录

我在这一步遇到过 `415 Unsupported Media Type`，最终验证结果是：

1. 接口代码本身是对的。
2. 单元测试里 `POST /users` 可以通过。
3. 说明问题更可能出在 IDEA 里运行的旧进程、旧依赖或未重新导入 Maven 版本。

排障结论：

- `POST /users` 接收的是 JSON 请求体。
- 请求头必须是 `Content-Type: application/json`。
- 代码层面要保证 `UserInfo` 有无参构造器和 `setter`。
- 如果测试通过但 IDEA 里还报错，优先重启并重新导入 Maven。

## 14. 我当前的总结

到这一步，我对这个项目的理解应该是：

“这是一个用 Maven 构建的 Spring Boot Web 项目。`pom.xml` 负责声明项目信息和依赖；启动类负责启动应用；配置文件负责项目配置；我已经能在 `controller` 包中编写基础接口，并初步理解查询参数和路径参数的接收方式。”
