# Crydis

![Maven Central](https://img.shields.io/maven-central/v/cn.cikian/crydis?style=flat-square)
![Java](https://img.shields.io/badge/Java-8%2B-green?style=flat-square)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-2.7%2B-brightgreen?style=flat-square)
![License](https://img.shields.io/badge/license-MIT-blue?style=flat-square)

## 简介

Crydis 是一个轻量级、高效的 Redis 工具库，**基于 Jedis 7.5.2** 构建，完美支持 Java 8+ 和 Spring Boot 2.7+，同时支持非 Spring 项目。提供流畅的静态方法调用方式，开箱即用。

## ✨ 核心特性

- 🚀 **高性能**: 基于 Jedis 7.5.2，采用现代化 API 设计
- 📌 **静态方法调用**: 直接使用 `Crydis.xxx()` 方法，无需注入
- 🎯 **自动配置**: Spring Boot 2.7+ 完美支持自动配置
- 🔧 **非 Spring 友好**: 手动初始化，灵活配置，适合微服务和工具类项目
- 📦 **完整功能**: String、Hash、List、Set、计数器、对象序列化等
- 🎨 **简洁设计**: API 直观易用，代码量小

## 📦 Maven 依赖

### Spring Boot 项目

```xml
<dependency>
    <groupId>cn.cikian</groupId>
    <artifactId>crydis</artifactId>
    <version>0.1.3</version>
</dependency>
```

### 非 Spring 项目

```xml
<dependency>
    <groupId>cn.cikian</groupId>
    <artifactId>crydis</artifactId>
    <version>0.1.3</version>
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

## 📊 项目结构

```
cn.cikian.crydis
├── CrydisManager.java                    # 非Spring项目管理器
├── model/
│   └── CrydisConfiguration.java          # 配置类
├── service/
│   ├── Crydis.java                       # 静态方法入口
│   └── RedisClient.java                  # Redis客户端核心实现
├── autoconfigure/
│   └── CrydisAutoConfiguration.java      # Spring Boot自动配置
└── exception/
    └── CikException.java                 # 自定义异常
```

## 🔧 配置说明

| 配置项 | 类型 | 默认值 | 说明 |
|-------|------|-------|------|
| enable | boolean | false | 是否启用Crydis |
| host | String | - | Redis服务器地址 |
| port | int | 6379 | Redis服务器端口 |
| password | String | - | Redis密码 |
| database | int | 0 | Redis数据库索引 |
| timeout | int | 3000 | 连接超时时间(ms) |
| max-active | int | 50 | 最大连接数 |
| max-idle | int | 10 | 最大空闲连接数 |
| min-idle | int | 5 | 最小空闲连接数 |
| max-wait | long | 3000 | 最大等待时间(ms) |

## ⚠️ 注意事项

1. **初始化顺序**：非 Spring 项目必须先初始化才能使用
2. **资源释放**：非 Spring 项目结束时建议调用 `CrydisManager.destroy()`
3. **线程安全**：所有静态方法调用均为线程安全
4. **依赖冲突**：项目已内置 Jedis 7.5.2，无需额外引入

## 🤝 贡献

欢迎提交 Issue 和 Pull Request！

## 📄 License

MIT License

## 🔗 相关资源

- **GitHub**: https://github.com/Cikian/crydis
- **作者网站**: https://www.cikian.cn
- **Issue 跟踪**: https://github.com/Cikian/crydis/issues

---

**最后更新**: 2026-06-17  
**当前版本**: 0.1.3  
**维护者**: [Cikian Chen](https://www.cikian.cn)