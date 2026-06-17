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

```xml
<dependency>
    <groupId>cn.cikian</groupId>
    <artifactId>crydis</artifactId>
    <version>0.1.5</version>
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

### 一、String 类型操作

#### 1.1 基本操作

| 方法 | 参数 | 返回值 | 说明 |
|------|------|--------|------|
| `set(key, value)` | key: String - 键名<br>value: String - 键值 | void | 设置字符串值 |
| `set(key, value, expireTime, timeUnit)` | key: String - 键名<br>value: String - 键值<br>expireTime: long - 过期时间<br>timeUnit: TimeUnit - 时间单位 | void | 设置字符串值并指定过期时间 |
| `get(key)` | key: String - 键名 | String | 获取字符串值，不存在返回 null |
| `exists(key)` | key: String - 键名 | boolean | 判断键是否存在 |
| `delete(keys...)` | keys: String... - 一个或多个键名 | void | 删除一个或多个键 |

**示例：**
```java
Crydis.set("username", "Cikian");
Crydis.set("session:abc123", "userData", 30, TimeUnit.SECONDS);
String username = Crydis.get("username");
boolean exists = Crydis.exists("username");
Crydis.delete("username", "session:abc123");
```

#### 1.2 过期管理

| 方法 | 参数 | 返回值 | 说明 |
|------|------|--------|------|
| `expire(key, expireTime, timeUnit)` | key: String - 键名<br>expireTime: long - 过期时间<br>timeUnit: TimeUnit - 时间单位 | boolean | 设置键的过期时间 |
| `ttl(key)` | key: String - 键名 | long | 获取键的剩余过期时间(秒) |

#### 1.3 进阶操作

| 方法 | 参数 | 返回值 | 说明 |
|------|------|--------|------|
| `append(key, value)` | key: String - 键名<br>value: String - 追加值 | long | 在字符串末尾追加内容 |
| `strlen(key)` | key: String - 键名 | long | 获取字符串长度 |
| `getSet(key, value)` | key: String - 键名<br>value: String - 新值 | String | 获取旧值并设置新值 |
| `mget(keys...)` | keys: String... - 多个键名 | List<String> | 批量获取多个键的值 |
| `mset(keyValuePairs...)` | keyValuePairs: String... - 键值对(偶数个) | void | 批量设置多个键值对 |

---

### 二、Hash 类型操作

#### 2.1 基本操作

| 方法 | 参数 | 返回值 | 说明 |
|------|------|--------|------|
| `hset(key, field, value)` | key: String - 哈希表名<br>field: String - 字段名<br>value: String - 字段值 | void | 设置哈希表字段值 |
| `hmset(key, hash)` | key: String - 哈希表名<br>hash: Map<String,String> - 字段值映射 | void | 批量设置哈希表字段 |
| `hget(key, field)` | key: String - 哈希表名<br>field: String - 字段名 | String | 获取哈希表字段值 |
| `hgetAll(key)` | key: String - 哈希表名 | Map<String,String> | 获取哈希表所有字段和值 |
| `hexists(key, field)` | key: String - 哈希表名<br>field: String - 字段名 | boolean | 判断字段是否存在 |
| `hdel(key, fields...)` | key: String - 哈希表名<br>fields: String... - 字段名 | void | 删除哈希表中的字段 |

**示例：**
```java
Crydis.hset("user:1", "name", "Cikian");
Crydis.hmset("user:1", Map.of("name", "Cikian", "email", "cikian@cikian.com"));
String name = Crydis.hget("user:1", "name");
Map<String, String> user = Crydis.hgetAll("user:1");
Crydis.hdel("user:1", "email");
```

#### 2.2 进阶操作

| 方法 | 参数 | 返回值 | 说明 |
|------|------|--------|------|
| `hkeys(key)` | key: String - 哈希表名 | Set<String> | 获取所有字段名 |
| `hvals(key)` | key: String - 哈希表名 | List<String> | 获取所有字段值 |
| `hlen(key)` | key: String - 哈希表名 | long | 获取字段数量 |
| `hincrBy(key, field, increment)` | key: String - 哈希表名<br>field: String - 字段名<br>increment: long - 增量 | long | 对字段值进行增量操作 |

---

### 三、List 类型操作

#### 3.1 基本操作

| 方法 | 参数 | 返回值 | 说明 |
|------|------|--------|------|
| `lpush(key, values...)` | key: String - 列表名<br>values: String... - 元素值 | void | 从列表左侧插入一个或多个元素 |
| `rpush(key, values...)` | key: String - 列表名<br>values: String... - 元素值 | void | 从列表右侧插入一个或多个元素 |
| `lpop(key)` | key: String - 列表名 | String | 移除并返回列表左侧第一个元素 |
| `rpop(key)` | key: String - 列表名 | String | 移除并返回列表右侧第一个元素 |
| `lrange(key, start, end)` | key: String - 列表名<br>start: long - 起始索引<br>end: long - 结束索引 | List<String> | 获取列表指定范围的元素 |
| `llen(key)` | key: String - 列表名 | long | 获取列表长度 |

**示例：**
```java
Crydis.lpush("queue:tasks", "task3", "task2", "task1");
Crydis.rpush("queue:tasks", "task4", "task5");
String task = Crydis.lpop("queue:tasks");
List<String> allTasks = Crydis.lrange("queue:tasks", 0, -1);
```

#### 3.2 进阶操作

| 方法 | 参数 | 返回值 | 说明 |
|------|------|--------|------|
| `lindex(key, index)` | key: String - 列表名<br>index: long - 索引位置 | String | 获取指定索引位置的元素 |
| `lset(key, index, value)` | key: String - 列表名<br>index: long - 索引位置<br>value: String - 新值 | void | 设置指定索引位置的元素值 |
| `linsert(key, before, pivot, value)` | key: String - 列表名<br>before: boolean - 是否在前面插入<br>pivot: String - 参考元素<br>value: String - 新元素 | long | 在指定元素前/后插入新元素 |
| `ltrim(key, start, end)` | key: String - 列表名<br>start: long - 起始索引<br>end: long - 结束索引 | void | 截取列表，保留指定范围的元素 |

---

### 四、Set 类型操作

#### 4.1 基本操作

| 方法 | 参数 | 返回值 | 说明 |
|------|------|--------|------|
| `sadd(key, members...)` | key: String - 集合名<br>members: String... - 成员值 | void | 向集合添加一个或多个成员 |
| `smembers(key)` | key: String - 集合名 | Set<String> | 获取集合所有成员 |
| `sismember(key, member)` | key: String - 集合名<br>member: String - 成员值 | boolean | 判断成员是否在集合中 |
| `srem(key, members...)` | key: String - 集合名<br>members: String... - 成员值 | void | 移除集合中的一个或多个成员 |
| `scard(key)` | key: String - 集合名 | long | 获取集合的大小 |

**示例：**
```java
Crydis.sadd("tags:java", "spring", "redis", "jpa");
Set<String> tags = Crydis.smembers("tags:java");
boolean hasRedis = Crydis.sismember("tags:java", "redis");
Crydis.srem("tags:java", "jpa");
```

#### 4.2 进阶操作

| 方法 | 参数 | 返回值 | 说明 |
|------|------|--------|------|
| `spop(key)` | key: String - 集合名 | String | 随机弹出一个成员 |
| `spop(key, count)` | key: String - 集合名<br>count: long - 弹出数量 | Set<String> | 随机弹出指定数量的成员 |
| `srandmember(key)` | key: String - 集合名 | String | 随机获取一个成员（不弹出） |
| `srandmember(key, count)` | key: String - 集合名<br>count: int - 获取数量 | List<String> | 随机获取指定数量的成员 |
| `sinterstore(dest, keys...)` | dest: String - 目标集合名<br>keys: String... - 源集合名 | long | 计算多个集合的交集并存储 |
| `sunionstore(dest, keys...)` | dest: String - 目标集合名<br>keys: String... - 源集合名 | long | 计算多个集合的并集并存储 |

---

### 五、ZSet (有序集合) 操作

#### 5.1 基本操作

| 方法 | 参数 | 返回值 | 说明 |
|------|------|--------|------|
| `zadd(key, score, member)` | key: String - 有序集合名<br>score: double - 分数<br>member: String - 成员值 | void | 添加一个成员及其分数 |
| `zadd(key, scoreMembers)` | key: String - 有序集合名<br>scoreMembers: Map<String,Double> - 成员分数映射 | void | 批量添加成员及其分数 |
| `zrange(key, start, end)` | key: String - 有序集合名<br>start: long - 起始索引<br>end: long - 结束索引 | List<String> | 获取指定范围的成员（升序） |
| `zrangeWithScores(key, start, end)` | key: String - 有序集合名<br>start: long - 起始索引<br>end: long - 结束索引 | List<Tuple> | 获取指定范围的成员及分数 |
| `zrank(key, member)` | key: String - 有序集合名<br>member: String - 成员值 | Long | 获取成员的排名（升序） |
| `zscore(key, member)` | key: String - 有序集合名<br>member: String - 成员值 | Double | 获取成员的分数 |
| `zrem(key, members...)` | key: String - 有序集合名<br>members: String... - 成员值 | void | 移除一个或多个成员 |

**示例：**
```java
Crydis.zadd("ranking", 95.5, "Alice");
Crydis.zadd("ranking", Map.of("Bob", 88.0, "Charlie", 92.5));
List<String> top3 = Crydis.zrange("ranking", 0, 2);
Double score = Crydis.zscore("ranking", "Alice");
```

#### 5.2 进阶操作

| 方法 | 参数 | 返回值 | 说明 |
|------|------|--------|------|
| `zcard(key)` | key: String - 有序集合名 | long | 获取有序集合的成员数量 |
| `zcount(key, min, max)` | key: String - 有序集合名<br>min: double - 最小分数<br>max: double - 最大分数 | long | 统计指定分数范围内的成员数量 |
| `zincrby(key, increment, member)` | key: String - 有序集合名<br>increment: double - 增量<br>member: String - 成员值 | Double | 对成员的分数进行增量操作 |

---

### 六、计数器操作

| 方法 | 参数 | 返回值 | 说明 |
|------|------|--------|------|
| `incr(key)` | key: String - 键名 | long | 对值进行+1操作 |
| `incrBy(key, increment)` | key: String - 键名<br>increment: long - 增量值 | long | 对值进行指定增量操作 |
| `decr(key)` | key: String - 键名 | long | 对值进行-1操作 |
| `decrBy(key, decrement)` | key: String - 键名<br>decrement: long - 减量值 | long | 对值进行指定减量操作 |

**示例：**
```java
Long count = Crydis.incr("counter:visits");
count = Crydis.incrBy("counter:visits", 10);
count = Crydis.decr("counter:visits");
count = Crydis.decrBy("counter:visits", 5);
```

---

### 七、对象序列化操作

| 方法 | 参数 | 返回值 | 说明 |
|------|------|--------|------|
| `setObject(key, obj)` | key: String - 键名<br>obj: Object - 要序列化的对象 | void | 将对象序列化为JSON并存储 |
| `setObject(key, obj, expireTime, timeUnit)` | key: String - 键名<br>obj: Object - 要序列化的对象<br>expireTime: long - 过期时间<br>timeUnit: TimeUnit - 时间单位 | void | 将对象序列化并设置过期时间 |
| `getObject(key, clazz)` | key: String - 键名<br>clazz: Class<T> - 对象类型 | T | 反序列化获取对象 |

**示例：**
```java
User user = new User(1, "Cikian", "cikian@cikian.com");
Crydis.setObject("user:1", user);
Crydis.setObject("user:2", user, 60, TimeUnit.MINUTES);
User retrieved = Crydis.getObject("user:1", User.class);
```

---

### 八、分布式锁操作

| 方法 | 参数 | 返回值 | 说明 |
|------|------|--------|------|
| `tryLock(key, expireTime, timeUnit)` | key: String - 锁名<br>expireTime: long - 锁过期时间<br>timeUnit: TimeUnit - 时间单位 | boolean | 获取锁（自动生成唯一value） |
| `tryLock(key, value, expireTime, timeUnit)` | key: String - 锁名<br>value: String - 唯一标识<br>expireTime: long - 锁过期时间<br>timeUnit: TimeUnit - 时间单位 | boolean | 获取锁（自定义value） |
| `unlock(key)` | key: String - 锁名 | void | 释放锁（简单方式） |
| `unlock(key, expectedValue)` | key: String - 锁名<br>expectedValue: String - 期望值 | boolean | 释放锁（安全方式，验证value） |

**示例：**
```java
// 安全锁（推荐）
String requestId = UUID.randomUUID().toString();
if (Crydis.tryLock("lock:order:123", requestId, 30, TimeUnit.SECONDS)) {
    try {
        processOrder(123);
    } finally {
        Crydis.unlock("lock:order:123", requestId);
    }
}
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
5. **序列化**：对象序列化使用 Jackson 实现，确保实体类有默认构造函数

## 🔍 常见问题 (FAQ)

### Q: Spring Boot 项目中如何禁用自动配置？

**A:** 在启动类上添加排除注解：

```java
@SpringBootApplication(exclude = CrydisAutoConfiguration.class)
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
```

### Q: 如何处理连接超时问题？

**A:** 可以通过配置 `timeout` 和 `max-wait` 参数来调整：

```yaml
crydis:
  timeout: 5000        # 连接超时时间
  max-wait: 5000       # 最大等待时间
```

### Q: 支持 Redis Cluster 吗？

**A:** 当前版本暂不支持 Redis Cluster，仅支持单机模式。Cluster 支持正在开发中。

### Q: 对象序列化失败怎么办？

**A:** 确保你的实体类：
- 有默认无参构造函数
- 字段有 getter/setter 方法
- 没有循环引用

## 🔗 性能说明

- **连接池**：基于 Apache Commons Pool 2 实现高效连接复用
- **线程安全**：所有操作均为线程安全，适合高并发场景
- **零拷贝**：避免不必要的数据复制

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