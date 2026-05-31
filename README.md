# Crydis

## 简介

Crydis 是一个简洁的 Redis 工具库，**完美支持 Spring Boot 2.x/3.x 和非 Spring 项目**，提供静态方法调用方式，开箱即用。

## 特性

- **静态方法调用**：直接使用 `Crydis.xxx()` 方法，无需注入
- **Spring Boot 自动配置**：在 2.x 和 3.x 中均完美支持
- **非 Spring 项目支持**：手动初始化，灵活配置
- **版本无关设计**：兼容 Jedis 3.x 和 4.x
- **完整功能**：支持 String、Hash、List、Set 等常用数据类型
- **对象序列化**：内置 JSON 序列化支持
- **连接池管理**：内置 Jedis 连接池

## 兼容性表格

### Spring Boot 项目

| Spring Boot 版本 | Java 版本 | 推荐 Jedis | Crydis 支持 |
|------------------|-----------|------------|-------------|
| **1.5.x - 2.7.x** | 8 - 17    | 3.x (自带) | ✅ 完全支持 |
| **3.0.x - 3.3.x** | 17 - 25   | 4.x (自带) | ✅ 完全支持 |

> 💡 **Spring Boot 项目无需手动管理 Jedis 版本**，Spring Boot 会自动管理！

### 非 Spring 项目

| 你的 Java 版本 | 推荐 Jedis 版本 | 理由 |
|---------------|----------------|------|
| **Java 8** | **3.9.0** | Jedis 3.x 最后的稳定版，完全兼容 Java 8 |
| **Java 9-11** | **3.9.0** 或 **4.4.3** | 可选择 4.x 获取新特性，或使用 3.x 保持稳定 |
| **Java 12-16** | **4.4.3** | Jedis 4.x 性能更好 |
| **Java 17+** | **4.4.3** | Spring Boot 3.x 标配版本 |

> 📖 **详细版本选择指南**：[JEDIS-VERSION-GUIDE.md](JEDIS-VERSION-GUIDE.md)

## Maven 依赖

### Spring Boot 项目

只需引入 Crydis，Jedis 版本由 Spring Boot 自动管理！

```xml
<dependency>
    <groupId>cn.cikian</groupId>
    <artifactId>crydis</artifactId>
    <version>0.0.1</version>
</dependency>
```

### 非 Spring 项目

需手动引入 Jedis，选择与你的 Java 版本匹配的版本：

```xml
<!-- Java 8 项目 -->
<dependency>
    <groupId>cn.cikian</groupId>
    <artifactId>crydis</artifactId>
    <version>0.0.1</version>
</dependency>

<dependency>
    <groupId>redis.clients</groupId>
    <artifactId>jedis</artifactId>
    <version>3.9.0</version>  <!-- Java 8 推荐 -->
</dependency>
```

```xml
<!-- Java 17+ 项目 -->
<dependency>
    <groupId>cn.cikian</groupId>
    <artifactId>crydis</artifactId>
    <version>0.0.1</version>
</dependency>

<dependency>
    <groupId>redis.clients</groupId>
    <artifactId>jedis</artifactId>
    <version>4.4.3</version>  <!-- Java 17+ 推荐 -->
</dependency>
```

## 快速开始

### Spring Boot 项目

#### 1. 添加配置（application.yml）

```yaml
crydis:
  enable: true
  host: 127.0.0.1
  port: 6379
  password:
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

#### 1. 引入依赖

根据你的 Java 版本选择合适的 Jedis 版本（见上方兼容性表格）

#### 2. 初始化

```java
public class Main {
    public static void main(String[] args) {
        CrydisManager.builder()
            .host("127.0.0.1")
            .port(6379)
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

## API 文档

### 基本操作

#### String 类型

```java
// 设置值
Crydis.set("key", "value");
Crydis.set("key", "value", 30, TimeUnit.SECONDS);

// 获取值
String value = Crydis.get("key");

// 判断存在
boolean exists = Crydis.exists("key");

// 删除
Crydis.delete("key");
Crydis.delete("key1", "key2");

// 设置过期
Crydis.expire("key", 60, TimeUnit.SECONDS);

// 获取 TTL
long ttl = Crydis.ttl("key");

// 分布式锁
Crydis.setNX("lock:key", "1");
```

#### Hash 类型

```java
// 设置 Hash 字段
Crydis.hset("user:1", "name", "Cikian");

// 批量设置
Map<String, String> map = new HashMap<>();
map.put("name", "Cikian");
map.put("email", "cikian@cikian.com");
Crydis.hmset("user:1", map);

// 获取 Hash 字段
String name = Crydis.hget("user:1", "name");

// 获取所有字段
Map<String, String> user = Crydis.hgetAll("user:1");

// 删除 Hash 字段
Crydis.hdel("user:1", "email");
```

#### List 类型

```java
// 左插入
Crydis.lpush("list", "a", "b", "c");

// 右插入
Crydis.rpush("list", "d", "e", "f");

// 左弹出
String left = Crydis.lpop("list");

// 右弹出
String right = Crydis.rpop("list");

// 范围查询
List<String> items = Crydis.lrange("list", 0, -1);
```

#### Set 类型

```java
// 添加成员
Crydis.sadd("tags", "java", "redis", "spring");

// 获取所有成员
Set<String> members = Crydis.smembers("tags");

// 判断成员存在
boolean isMember = Crydis.sismember("tags", "java");

// 删除成员
Crydis.srem("tags", "spring");
```

#### 计数器

```java
Crydis.set("counter", "0");
Crydis.incr("counter");       // 1
Crydis.incrBy("counter", 5); // 6
Crydis.decr("counter");       // 5
Crydis.decrBy("counter", 2); // 3
```

#### 对象操作

```java
// 存储对象（自动 JSON 序列化）
User user = new User(1, "Cikian", "cikian@cikian.com");
Crydis.setObject("user:1", user);
Crydis.setObject("user:2", user, 60, TimeUnit.MINUTES);

// 获取对象
User retrievedUser = Crydis.getObject("user:1", User.class);
```

## 注意事项

1. **初始化顺序**：非 Spring 项目必须先初始化才能使用
2. **资源释放**：非 Spring 项目结束时建议调用 `Crydis.destroy()`
3. **Jedis 版本**：Spring Boot 项目自动管理，非 Spring 项目需手动指定（见兼容性表格）
4. **线程安全**：静态方法调用是线程安全的

---

## 项目结构

```
cn.cikian.crydis
├── Crydis.java                 # 静态方法入口
├── CrydisManager.java          # 非Spring项目管理器
├── model
│   └── CrydisConfiguration.java # 配置类
├── service
│   └── RedisClient.java        # Redis客户端封装
└── autoconfigure
    └── CrydisAutoConfiguration.java # Spring自动配置
```

---

## License

MIT License
