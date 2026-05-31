# Crydis

![Maven Central](https://img.shields.io/maven-central/v/cn.cikian/crydis?style=flat-square)
![Java](https://img.shields.io/badge/Java-8%2B-green?style=flat-square)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-2.6%2B-brightgreen?style=flat-square)
![License](https://img.shields.io/badge/license-MIT-blue?style=flat-square)

## 简介

Crydis 是一个轻量级、高效的 Redis 工具库，**完美支持 Java 8-21+ 和 Spring Boot 2.6.6-4.x**，同时支持非 Spring 项目。提供流畅的静态方法调用方式，开箱即用。

## ✨ 核心特性

- 🚀 **多版本支持**：Java 8-21+、Spring Boot 2.6.6-4.x 完整兼容
- 📌 **静态方法调用**：直接使用 `Crydis.xxx()` 方法，无需注入
- 🎯 **自动配置**：Spring Boot 2.x/3.x 均完美支持
- 🔧 **非 Spring 友好**：手动初始化，灵活配置，适合微服务和工具类项目
- 🔌 **版本适配**：自动适配 Jedis 3.x 和 4.x
- 📦 **完整功能**：String、Hash、List、Set、计数器、对象序列化等
- 🎨 **简洁设计**：API ���观易用，代码量小

## 🔄 版本支持矩阵

### 快速查询

| Java 版本 | Spring Boot 版本 | 编译命令 | 支持度 |
|----------|-----------------|--------|-------|
| 8-11     | 2.6.6-2.7.x    | `默认` 或 `-P spring-boot-2.6` | ✅ |
| 8        | 2.6.6          | `-P spring-boot-2.6` | ✅ 最低版本 |
| 17+      | 3.0-3.1.x      | `-P spring-boot-3.0` | ✅ |
| 17+      | 3.2.x+         | `-P spring-boot-3.2` | ✅ |
| 21+      | 4.x            | `-P spring-boot-4.0` | ⚠️ 预览 |
| 8+       | 非 Spring 项目  | 使用 CrydisManager | ✅ |

**📚 详细版本对应表和使用指南：**
- [**快速参考**](QUICK_REFERENCE.md) ⚡ - 一行命令、常用代码、FAQ
- [**完整兼容性指南**](VERSION_COMPATIBILITY.md) 📖 - 详细的配置和故障排除

### Spring Boot 项目

| Spring Boot 版本 | Java 版本 | Jedis 版本 | 支持状态 |
|-----------------|----------|-----------|--------|
| 2.6.6-2.6.x     | 8+       | 3.9.0     | ✅ 完全支持 |
| 2.7.x           | 8-17     | 3.9.0     | ✅ 推荐版本 |
| 3.0-3.1.x       | 17+      | 4.4.3     | ✅ 完全支持 |
| 3.2.x+          | 17+      | 4.4.3     | ✅ 完全支持 |
| 4.x             | 21+      | 5.0.0     | ⚠️ 预览版本 |

> 💡 **Spring Boot 项目无需手动管理 Jedis 版本**，项目会自动适配！

### 非 Spring 项目

| Java 版本 | 推荐 Jedis 版本 | 理由 |
|----------|---------------|------|
| 8-11     | 3.9.0         | 最后的稳定版，完全兼容 Java 8 |
| 12-16    | 4.4.3         | 更好的性能和特性 |
| 17+      | 4.4.3         | 标配版本 |
| 21+      | 5.0.0         | 最新版本 |

## 📦 Maven 依赖

### Spring Boot 项目

```xml
<dependency>
    <groupId>cn.cikian</groupId>
    <artifactId>crydis</artifactId>
    <version>0.1.0</version>
</dependency>
```

### 非 Spring 项目（Java 8）

```xml
<dependency>
    <groupId>cn.cikian</groupId>
    <artifactId>crydis</artifactId>
    <version>0.1.0</version>
</dependency>

<dependency>
    <groupId>redis.clients</groupId>
    <artifactId>jedis</artifactId>
    <version>3.9.0</version>
</dependency>
```

### 非 Spring 项目（Java 17+）

```xml
<dependency>
    <groupId>cn.cikian</groupId>
    <artifactId>crydis</artifactId>
    <version>0.1.0</version>
</dependency>

<dependency>
    <groupId>redis.clients</groupId>
    <artifactId>jedis</artifactId>
    <version>4.4.3</version>
</dependency>
```

## 🚀 快速开始

### Spring Boot 项目

#### 1. 配置（application.yml）

```yaml
crydis:
  enable: true
  host: localhost
  port: 6379
  password:                 # 无密码则留空
  database: 0
  timeout: 3000
  max-active: 50
  max-idle: 10
  min-idle: 5
  max-wait: 3000
```

#### 2. 直接使用

```java
@Service
public class UserService {

    public void saveUser(User user) {
        Crydis.set("user:" + user.getId(), user.getName());
    }

    public String getUserName(Long userId) {
        return Crydis.get("user:" + userId);
    }
}
```

---

### 非 Spring 项目

```java
import cn.cikian.crydis.CrydisManager;
import cn.cikian.crydis.service.Crydis;

public class Main {
    public static void main(String[] args) {
        // 初始化
        CrydisManager.builder()
                .host("localhost")
                .port(6379)
                .database(0)
                .timeout(3000)
                .maxActive(50)
                .maxIdle(10)
                .minIdle(5)
                .maxWait(3000L)
                .init();

        // 使用
        Crydis.set("key", "value");
        String value = Crydis.get("key");
        System.out.println("Value: " + value);

        // 程序结束时销毁
        CrydisManager.destroy();
    }
}
```

## 📖 完整 API 文档

### String 类型

```java
// 基本操作
Crydis.set("key", "value");
Crydis.set("key", "value", 30, TimeUnit.SECONDS);  // 带过期时间
Crydis.setNX("key", "value");                       // 仅当不存在时设置

String value = Crydis.get("key");
boolean exists = Crydis.exists("key");
Crydis.delete("key", "key2");

// 过期管理
Crydis.expire("key", 60, TimeUnit.SECONDS);
long ttl = Crydis.ttl("key");
```

### Hash 类型

```java
// 基本操作
Crydis.hset("user:1", "name", "Cikian");
Crydis.hmset("user:1", Map.of("name", "Cikian", "email", "cikian@cikian.com"));

// 查询
String name = Crydis.hget("user:1", "name");
Map<String, String> user = Crydis.hgetAll("user:1");
boolean exists = Crydis.hexists("user:1", "name");

// 删除
Crydis.hdel("user:1", "email");
```

### List 类型

```java
// 插入
Crydis.lpush("list", "a", "b", "c");      // 左插入
Crydis.rpush("list", "d", "e", "f");      // 右插入

// 弹出
String left = Crydis.lpop("list");
String right = Crydis.rpop("list");

// 查询
List<String> items = Crydis.lrange("list", 0, -1);
Long length = Crydis.llen("list");
```

### Set 类型

```java
// 添加
Crydis.sadd("tags", "java", "redis", "spring");

// 查询
Set<String> members = Crydis.smembers("tags");
boolean isMember = Crydis.sismember("tags", "java");

// 删除
Crydis.srem("tags", "spring");
```

### 计数器

```java
Long count = Crydis.incr("counter");           // +1
count = Crydis.incrBy("counter", 5);           // +5
count = Crydis.decr("counter");                // -1
count = Crydis.decrBy("counter", 2);           // -2
```

### 对象序列化

```java
// 存储对象（自动 JSON 序列化）
User user = new User(1, "Cikian", "cikian@cikian.com");
Crydis.setObject("user:1", user);
Crydis.setObject("user:2", user, 60, TimeUnit.MINUTES);

// 获取对象
User retrieved = Crydis.getObject("user:1", User.class);
```

## 💻 开发和编译

### 多版本编译

```bash
# 默认编译（Spring Boot 2.7.x, Java 8）
mvn clean install

# Spring Boot 2.6.6 编译（最低版本，Java 8）
mvn clean install -P spring-boot-2.6

# Spring Boot 3.x 编译（Java 17+）
mvn clean install -P spring-boot-3.0

# Spring Boot 3.2+ 编译（Java 17+）
mvn clean install -P spring-boot-3.2

# Spring Boot 4.x 编译（Java 21+，预览）
mvn clean install -P spring-boot-4.0
```

### 多版本测试

```bash
# Java 11 兼容性测试
mvn clean test -P java-11

# Java 17 兼容性测试
mvn clean test -P java-17

# Java 21 兼容性测试
mvn clean test -P java-21
```

### 完整的开发工作流

```bash
# 1. 清理编译
mvn clean install

# 2. 验证多 Spring Boot 版本兼容性
mvn clean test -P spring-boot-2.6
mvn clean test -P spring-boot-3.0
mvn clean test -P spring-boot-3.2

# 3. 验证多 Java 版本兼容性
mvn clean test -P java-11
mvn clean test -P java-17

# 4. 生成文档和源码
mvn clean source:jar javadoc:jar

# 5. 打包
mvn clean package -DskipTests
```

## ⚠️ 注意事项

1. **初始化顺序**：非 Spring 项目必须先初始化才能使用
2. **资源释放**：非 Spring 项目结束时建议调用 `Crydis.destroy()`
3. **线程安全**：所有静态方法调用均为线程安全
4. **Spring Boot 2.6.6**：推荐最低使用 2.6.6 版本以获得最佳稳定性
5. **Jedis 版本冲突**：使用 Spring Boot 3.x 时自动升级到 Jedis 4.4.3

## 📊 项目结构

```
cn.cikian.crydis
├── Crydis.java                 # 静态方法入口
├── CrydisManager.java          # 非Spring项目管理器
├── model/
│   └── CrydisConfiguration.java # Spring Boot 配置类
├── service/
│   ├── RedisClient.java        # Redis 客户端核心实现
│   └── Crydis.java             # 静态方法包装
├── autoconfigure/
│   └── CrydisAutoConfiguration.java # Spring Boot 自动配置
├── config/
│   ├── LogbackCustomHtmlLayout.java # HTML 日志样式
│   └── SmartLogbackConfigurator.java # 智能日志配置
└── exception/
    └── CikException.java       # 自定义异常
```

## 📚 文档导航

| 文档 | 用途 |
|-----|------|
| [QUICK_REFERENCE.md](QUICK_REFERENCE.md) | ⚡ 快速参考 - 常用命令和代码片段 |
| [VERSION_COMPATIBILITY.md](VERSION_COMPATIBILITY.md) | 📖 完整兼容性指南 - 详细配置、故障排除 |
| [本 README](README.md) | 📋 项目概览和使用指南 |

## 🔍 故障排除

### 常见问题

**Q: 我的 Spring Boot 是 2.6.6，能用吗？**
```bash
mvn clean install -P spring-boot-2.6
```
✅ 可以！这是最低支持版本

**Q: 我的 Spring Boot 是 3.2，我的 Java 是 17，怎么编译？**
```bash
mvn clean install -P spring-boot-3.2
```
✅ 已完全支持

**Q: 非 Spring 项目怎么用？**
```java
CrydisManager.builder()
    .host("localhost")
    .port(6379)
    .init();
Crydis.set("key", "value");
```
✅ 完全支持，见快速开始

更多常见问题和解决方案，请查看 [VERSION_COMPATIBILITY.md](VERSION_COMPATIBILITY.md)

## 🤝 贡献

欢迎提交 Issue 和 Pull Request！

## 📄 License

MIT License - 详见 [LICENSE](LICENSE) 文件

## 🔗 相关资源

- **GitHub**: https://github.com/Cikian/crydis
- **作者网站**: https://www.cikian.cn
- **Issue 跟踪**: https://github.com/Cikian/crydis/issues

---

**最后更新**: 2026-05-31  
**当前版本**: 0.1.0  
**维护者**: [Cikian Chen](https://www.cikian.cn)
