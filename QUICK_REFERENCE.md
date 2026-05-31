# 🚀 Crydis 快速参考

## 版本支持速查表

```
Java 8    ──→ Spring Boot 2.6.6-2.7.x    ✅
Java 11   ──→ Spring Boot 2.6.6-2.7.x    ✅
Java 17   ──→ Spring Boot 2.7.x/3.0-3.2  ✅
Java 21   ──→ Spring Boot 3.2.x/4.x      ✅
```

---

## 一行命令编译

```bash
# Spring Boot 2.6.6（最低版本）
mvn clean install -P spring-boot-2.6

# Spring Boot 2.7.x（推荐）
mvn clean install

# Spring Boot 3.x（Java 17+）
mvn clean install -P spring-boot-3.0

# Spring Boot 3.2+（Java 17+）
mvn clean install -P spring-boot-3.2

# Spring Boot 4.x（Java 21+，预览）
mvn clean install -P spring-boot-4.0
```

---

## Spring Boot 项目配置

### pom.xml 依赖
```xml
<dependency>
    <groupId>cn.cikian</groupId>
    <artifactId>crydis</artifactId>
    <version>0.1.0</version>
</dependency>
```

### application.yml
```yaml
crydis:
  enable: true
  host: localhost
  port: 6379
  password:           # 无密码则留空
  database: 0
  timeout: 3000
  max-active: 50
  max-idle: 10
  min-idle: 5
  max-wait: 3000
```

### 使用示例
```java
@Autowired
private RedisClient redisClient;

// 或使用静态方法
Crydis.set("key", "value");
Crydis.get("key");
Crydis.delete("key");
```

---

## 非 Spring 项目初始化

```java
CrydisManager.builder()
    .host("localhost")
    .port(6379)
    .database(0)
    .init();

// 直接使用静态方法
Crydis.set("key", "value");
String value = Crydis.get("key");

// 完成后销毁
CrydisManager.destroy();
```

---

## 常用操作

```java
// 字符串操作
Crydis.set("key", "value");
Crydis.set("key", "value", 30, TimeUnit.SECONDS);  // 带过期时间
Crydis.setNX("key", "value");                       // 仅当不存在时设置
String value = Crydis.get("key");
Crydis.delete("key");

// 哈希操作
Crydis.hset("hash", "field", "value");
Crydis.hmset("hash", Map.of("f1", "v1", "f2", "v2"));
String value = Crydis.hget("hash", "field");
Map<String, String> all = Crydis.hgetAll("hash");

// 计数器
Crydis.incr("counter");
Crydis.decr("counter");
Crydis.incrBy("counter", 10);

// 列表
Crydis.lpush("list", "a", "b", "c");
Crydis.rpush("list", "d", "e");
List<String> range = Crydis.lrange("list", 0, -1);

// 集合
Crydis.sadd("set", "member1", "member2");
Set<String> members = Crydis.smembers("set");

// 对象序列化
Crydis.setObject("user", user);
User user = Crydis.getObject("user", User.class);
```

---

## 多版本测试

```bash
# 测试 Java 编译兼容性
mvn clean test -P java-11    # Java 11
mvn clean test -P java-17    # Java 17
mvn clean test -P java-21    # Java 21

# 测试 Spring Boot 兼容性
mvn clean test -P spring-boot-2.6
mvn clean test -P spring-boot-3.0
mvn clean test -P spring-boot-3.2
```

---

## Profile 一览

| ID | Spring Boot | Java | 用途 |
|----|------------|------|------|
| `default` | 2.7.18 | 8 | 默认编译 |
| `spring-boot-2.6` | 2.6.15 | 8 | 最低版本支持 |
| `spring-boot-3.0` | 3.1.7 | 17 | Spring Boot 3.0-3.1 |
| `spring-boot-3.2` | 3.2.2 | 17 | Spring Boot 3.2+ |
| `spring-boot-4.0` | 4.0.0-SNAPSHOT | 21 | 预览版本 |
| `java-11` | 2.7.18 | 11 | Java 11 验证 |
| `java-17` | 2.7.18 | 17 | Java 17 验证 |
| `java-21` | 2.7.18 | 21 | Java 21 验证 |

---

## 依赖版本对应

| Profile | Jackson | Logback | Jedis |
|---------|---------|---------|-------|
| default | 2.15.4 | 1.2.13 | 3.9.0 |
| spring-boot-2.6 | 2.15.4 | 1.2.13 | 3.9.0 |
| spring-boot-3.0 | 2.15.4 | 1.2.13 | 4.4.3 |
| spring-boot-3.2 | 2.16.1 | 1.4.14 | 4.4.3 |
| spring-boot-4.0 | 2.17.0 | 1.5.0 | 5.0.0 |

---

## 常见问题

**Q: 我用的是 Spring Boot 2.6.6，能用吗？**
```bash
mvn clean install -P spring-boot-2.6
```
✅ 可以！这是最低支持版本

---

**Q: 我用的是 Spring Boot 3.2，用什么 Profile？**
```bash
mvn clean install -P spring-boot-3.2
```
✅ 需要 Java 17+

---

**Q: 非 Spring 项目怎么用？**
```java
CrydisManager.builder()
    .host("localhost")
    .port(6379)
    .init();
```
✅ 支持 Java 8+

---

**Q: Spring Boot 4.x 什么时候正式支持？**
```bash
mvn clean install -P spring-boot-4.0  # 目前是预览
```
⚠️ 需要 Java 21+，等待 Spring Boot 4.x 正式发布

---

**Q: 编译报错 "Unsupported class-file format"？**
```bash
java -version  # 检查 JDK 版本
# 确保 JDK 版本 ≥ Profile 要求的版本
```

---

## 进阶命令

```bash
# 完整的多版本测试
mvn clean test -P spring-boot-2.6
mvn clean test -P spring-boot-3.0  
mvn clean test -P spring-boot-3.2
mvn clean test -P java-11
mvn clean test -P java-17

# 生成文档和源码
mvn clean source:jar javadoc:jar

# 本地测试
mvn clean install -DskipTests

# 发布到 Maven Central
mvn clean deploy -P release
```

---

## 相关资源

- 📖 [完整兼容性指南](VERSION_COMPATIBILITY.md)
- 🐛 [提交 Issue](https://github.com/Cikian/crydis/issues)
- 💻 [项目主页](https://github.com/Cikian/crydis)
- 🌐 [作者网站](https://www.cikian.cn)
