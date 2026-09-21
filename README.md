# Crydis

![Maven Central](https://img.shields.io/maven-central/v/cn.cikian/crydis?style=flat-square)
![Java](https://img.shields.io/badge/Java-8%2B-green?style=flat-square)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-2.6.6%2B-brightgreen?style=flat-square)
![License](https://img.shields.io/badge/license-MIT-blue?style=flat-square)

## 简介

Crydis 是一个轻量级 Redis 工具库，基于 **Jedis 7.5.2** 构建，支持 Java 8+ 与 Spring Boot 2.6.6+，同时支持非 Spring 项目。提供静态方法调用方式，无需注入即可使用。

> 本版本（0.2.3）包含一轮安全性修复，**升级前请先阅读 [从旧版本升级](#-从旧版本升级)**。

## ✨ 核心特性

- 🚀 **基于 Jedis 7.5.2**，使用 `DefaultJedisClientConfig` 构建器
- 📌 **静态方法调用**：直接使用 `Crydis.xxx()`，无需注入
- 🎯 **Spring Boot 自动配置**：`ck.crydis.enable=true` 即启用
- 🔧 **非 Spring 友好**：`CrydisManager.builder()` 流式初始化
- 🔒 **安全默认**：反序列化白名单默认收窄、分布式锁带 token 归属校验
- 📦 **完整功能**：String、Hash、List、Set、ZSet、计数器、对象序列化、分布式锁
- 🧩 **依赖隔离**：打包时 relocation Jedis，与业务已有的 Jedis 版本共存

## 📦 Maven 依赖

```xml
<dependency>
    <groupId>cn.cikian</groupId>
    <artifactId>crydis</artifactId>
    <version>0.2.3</version>
</dependency>
```

## 🚀 快速开始

### Spring Boot 项目

#### 1. 配置（application.yml）

```yaml
ck:
  crydis:
    enable: true
    host: localhost
    port: 6379
    password:               # 无密码则留空
    # user:                 # 仅 Redis >= 6.0 的 ACL 用户才需要配置，见下方"兼容性说明"
    database: 0
    timeout: 3000
    max-active: 50
    max-idle: 10
    min-idle: 5
    max-wait: 3000
    # allowed-packages:     # 反序列化白名单，仅在需要还原多态字段时配置
    #   - cn.cikian.demo.
    # unwrap-quoted-string: false   # 仅用于兼容历史数据，见"从旧版本升级"
```

> 配置前缀为 **`ck.crydis`**（不是 `crydis`）。前缀写错时 `@ConditionalOnProperty` 匹配不上，
> 自动配置会**静默失效**且不报错，请以 `CrydisConfiguration` 的 `@ConfigurationProperties` 为准。

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

Spring 容器关闭时会自动释放连接池（`CrydisAutoConfiguration` 实现了 `DisposableBean`）。

---

### 非 Spring 项目

```java
import cn.cikian.crydis.CrydisManager;
import cn.cikian.crydis.service.Crydis;

public class Main {
    public static void main(String[] args) {
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

        Crydis.set("key", "value");
        System.out.println("Value: " + Crydis.get("key"));

        // 程序结束时释放连接池（幂等，可重复调用）
        CrydisManager.destroy();
    }
}
```

### 连接池为懒加载

`new RedisClient(config)` **不会**立即建立连接池，第一次真正执行 Redis 命令时才创建。
这样避免了"对象创建了却从未被托管"导致的连接池泄漏，也让初始化异常发生在明确的调用点。

## 📖 API 文档

> 返回值类型均与源码一致。所有方法在参数非法时抛出 `IllegalArgumentException`，
> 在 Redis 操作失败时抛出 `RuntimeException`（cause 为原始的 Jedis 异常）。

### 一、String 操作

| 方法 | 参数 | 返回值 | 说明 |
|------|------|--------|------|
| `set(key, value)` | key/value: String | void | 设置字符串值，value 不可为 null |
| `set(key, value, expireTime, timeUnit)` | expireTime: long, timeUnit: TimeUnit | void | 设置值并指定过期时间（必须 ≥ 1 秒） |
| `setNX(key, value)` | key/value: String | void | 仅当 key 不存在时设置 |
| `get(key)` | key: String | String | 获取值，不存在返回 null |
| `exists(key)` | key: String | boolean | 判断键是否存在 |
| `delete(key)` | key: String | void | 删除单个键 |
| `delete(keys...)` | keys: String... | void | 删除多个键，不可传空 |
| `expire(key, expireTime, timeUnit)` | expireTime: long, timeUnit: TimeUnit | boolean | 设置过期时间，key 不存在返回 false |
| `ttl(key)` | key: String | long | 剩余过期秒数 |
| `append(key, value)` | key/value: String | Long | 追加内容，返回追加后长度 |
| `strlen(key)` | key: String | Long | 字符串长度 |
| `getSet(key, value)` | key/value: String | String | 返回旧值并设置新值 |
| `mget(keys...)` | keys: String... | List&lt;String&gt; | 批量获取，不存在的键为 null |
| `mset(keyValuePairs...)` | 偶数个 String | void | 批量设置，参数个数必须为偶数 |

### 二、Hash 操作

| 方法 | 参数 | 返回值 | 说明 |
|------|------|--------|------|
| `hset(key, field, value)` | 均为 String | void | 设置字段值 |
| `hmset(key, hash)` | hash: Map&lt;String,String&gt; | void | 批量设置字段，不可传空 map |
| `hget(key, field)` | 均为 String | String | 获取字段值 |
| `hgetAll(key)` | key: String | Map&lt;String,String&gt; | 获取全部字段 |
| `hexists(key, field)` | 均为 String | boolean | 字段是否存在 |
| `hdel(key, fields...)` | fields: String... | void | 删除字段 |
| `hkeys(key)` | key: String | Set&lt;String&gt; | 所有字段名 |
| `hvals(key)` | key: String | List&lt;String&gt; | 所有字段值 |
| `hlen(key)` | key: String | Long | 字段数量 |
| `hincrBy(key, field, increment)` | increment: long | Long | 字段值增量 |

> `hmset` 会优先使用单条 `HSET key f1 v1 f2 v2`（Redis ≥ 4.0），
> 在老服务端上自动回退为逐字段写入，因此 Redis 3.x 同样可用。

### 三、List 操作

| 方法 | 参数 | 返回值 | 说明 |
|------|------|--------|------|
| `lpush(key, values...)` | values: String... | void | 左侧插入 |
| `rpush(key, values...)` | values: String... | void | 右侧插入 |
| `lpop(key)` / `rpop(key)` | key: String | String | 弹出元素 |
| `lrange(key, start, end)` | start/end: long | List&lt;String&gt; | 范围查询，`0, -1` 为全部 |
| `llen(key)` | key: String | Long | 列表长度 |
| `lindex(key, index)` | index: long | String | 指定索引元素 |
| `lset(key, index, value)` | index: long, value: String | String | 设置索引元素（底层返回 "OK"） |
| `linsert(key, before, pivot, value)` | before: boolean | Long | 在 pivot 前/后插入 |
| `ltrim(key, start, end)` | start/end: long | String | 裁剪列表（底层返回 "OK"） |

### 四、Set 操作

| 方法 | 参数 | 返回值 | 说明 |
|------|------|--------|------|
| `sadd(key, members...)` | members: String... | void | 添加成员 |
| `smembers(key)` | key: String | Set&lt;String&gt; | 全部成员 |
| `sismember(key, member)` | 均为 String | boolean | 是否为成员 |
| `srem(key, members...)` | members: String... | void | 移除成员 |
| `scard(key)` | key: String | Long | 集合大小 |
| `spop(key)` | key: String | String | 随机弹出一个 |
| `spop(key, count)` | count: long | Set&lt;String&gt; | 随机弹出多个，**需要 Redis ≥ 3.2** |
| `srandmember(key)` | key: String | String | 随机取一个（不弹出） |
| `srandmember(key, count)` | count: int | List&lt;String&gt; | 随机取多个（不弹出） |
| `sinterstore(dest, keys...)` | dest: String, keys: String... | Long | 交集存储 |
| `sunionstore(dest, keys...)` | dest: String, keys: String... | Long | 并集存储 |

### 五、ZSet 操作

| 方法 | 参数 | 返回值 | 说明 |
|------|------|--------|------|
| `zadd(key, score, member)` | score: double | Long | 添加成员 |
| `zadd(key, scoreMembers)` | Map&lt;String,Double&gt; | Long | 批量添加 |
| `zrange(key, start, end)` | start/end: long | List&lt;String&gt; | 按分数升序范围 |
| `zrangeWithScores(key, start, end)` | start/end: long | List&lt;Tuple&gt; | 含分数，`Tuple.getScore()` 返回 double |
| `zrank(key, member)` | 均为 String | Long | 升序排名，不存在返回 null |
| `zscore(key, member)` | 均为 String | Double | 分数 |
| `zrem(key, members...)` | members: String... | Long | 移除成员 |
| `zcard(key)` | key: String | Long | 成员数量 |
| `zcount(key, min, max)` | min/max: double | Long | 分数区间计数 |
| `zincrby(key, increment, member)` | increment: double | Double | 分数增量 |

### 六、计数器

| 方法 | 返回值 |
|------|--------|
| `incr(key)` / `decr(key)` | Long |
| `incrBy(key, increment)` / `decrBy(key, decrement)` | Long |

### 七、对象序列化

| 方法 | 参数 | 返回值 |
|------|------|--------|
| `setObject(key, object)` | object: T | void |
| `setObject(key, object, expireTime, timeUnit)` | expireTime: long | void |
| `getObject(key, clazz)` | clazz: Class&lt;T&gt; | T（不存在返回 null） |
| `getObject(key, clazz, allowedPackagePrefixes...)` | 额外白名单前缀 | T |

```java
User user = new User(1, "Cikian", "cikian@cikian.com");
Crydis.setObject("user:1", user);
Crydis.setObject("user:2", user, 60, TimeUnit.MINUTES);
User retrieved = Crydis.getObject("user:1", User.class);
```

序列化使用 fastjson2，并写入 `@type` 以便还原多态类型；反序列化受白名单约束（见下节）。

### 八、分布式锁

推荐使用带 token 的 API（token 由调用方持有，保证只释放自己的锁）：

```java
RedisClient client = Crydis.getRedisClient();
String token = client.tryLockWithToken("lock:order:123", 30, TimeUnit.SECONDS);
if (token != null) {
    try {
        processOrder(123);
    } finally {
        client.unlock("lock:order:123", token);   // 校验 token 后才删除，Lua 脚本保证原子性
    }
}
```

| 方法 | 返回值 | 说明 |
|------|--------|------|
| `tryLockWithToken(key, expireTime, timeUnit)` | String（失败返回 null） | **推荐**，内部生成 UUID token 并返回 |
| `tryLockWithToken(key, token, expireTime, timeUnit)` | boolean | 使用自定义 token |
| `unlock(key, expectedValue)` | boolean | 安全释放，仅当值匹配才删除 |
| `tryLock(key, expireTime, timeUnit)` | boolean | ⚠️ 已废弃：token 不外泄，只能配不安全的 `unlock(key)` |
| `unlock(key)` | void | ⚠️ 已废弃：无归属校验的 `DEL`，可能删除他人的锁 |

行为约定：
- 参数非法（过期 ≤ 0、不足 1 秒、token 为空）抛 `IllegalArgumentException`
- 锁被占用或 Redis 不可用时返回 `false` / `null`，**不抛异常**，避免打断业务控制流
- 过期时间全程使用 `long`，不存在旧版本 `(int)` 强转溢出问题

### 九、键查询

| 方法 | 返回值 | 说明 |
|------|--------|------|
| `scan(pattern)` | Set&lt;String&gt; | **推荐**，基于 SCAN 分批游标，不阻塞 Redis |
| `keys(pattern)` | Set&lt;String&gt; | ⚠️ 已废弃：`KEYS` 是 O(N) 阻塞命令 |
| `getKeysWithValues(pattern)` | Map&lt;String,String&gt; | ⚠️ 已废弃：非 String 类型会被跳过 |

## 🔐 反序列化安全

旧版本在未配置白名单时使用 `SupportAutoType` **全局盲放**，`getObject` 会把 JSON 中 `@type`
指定的任意类实例化，存在反序列化 RCE 风险（已实测复现）。当前版本的策略：

1. 白名单**始终生效**。未配置 `ck.crydis.allowed-packages` 时，自动收窄为"只放行目标类型自身"（含其嵌套类型）。
2. 目标类型不允许是 `Object.class`——fastjson2 在 `Object.class` 下会忽略过滤器，直接按 `@type` 实例化（实测确认）。
3. `@type` 不在白名单内时**直接抛异常**，不再静默降级为 `JSONObject` 让调用方后续遇到 `ClassCastException`。
4. fastjson2 已升级到 **2.0.65**（2.0.58 落在 2026 年 AutoType 绕过漏洞的影响区间 ≤ 2.0.62 内）。

需要还原多态字段（如接口字段存了具体实现类）时，显式配置前缀：

```yaml
ck:
  crydis:
    allowed-packages:
      - cn.cikian.demo.        # 注意结尾的点
```

> 白名单是**文本前缀**匹配，**不支持 `*` 通配符**：`cn.foo.` 放行该包下所有类，
> `cn.foo.*` 则一个都放行不了（`*` 被当作字面量）。

## ⚠️ 兼容性说明

| 能力 | 要求 |
|------|------|
| 基础命令 | Redis 2.6+ |
| `spop(key, count)` | **Redis ≥ 3.2**（SPOP 带 count 是该版本引入的） |
| `hmset` 单条多字段写入 | Redis ≥ 4.0（老版本自动回退逐字段写入） |
| `user` 配置项（ACL） | **Redis ≥ 6.0** |

关于 `user`：两参数 `AUTH user pass` 是 Redis 6.0 引入的语法。**未配置 `user` 时只发送单参数
`AUTH password`**；旧版本会强制填入 `default`，导致 Redis < 6.0 且设置密码时握手直接失败
（`ERR wrong number of arguments for 'auth' command`）。

## 🔧 配置项

以下配置项均位于 **`ck.crydis`** 前缀下：YAML 中写成 `ck.crydis.xxx`，properties 中写成 `ck.crydis.xxx`。

| 配置项 | 类型 | 默认值 | 说明 |
|-------|------|-------|------|
| enable | boolean | false | 是否启用自动配置 |
| host | String | - | Redis 地址，不能为空 |
| port | int | 6379 | 端口 |
| user | String | - | ACL 用户名，仅 Redis ≥ 6.0 需要 |
| password | String | - | 密码，无密码留空 |
| database | int | 0 | 数据库索引 |
| timeout | int | 3000 | 连接/读写超时（ms） |
| max-active | int | 50 | 最大连接数 |
| max-idle | int | 10 | 最大空闲连接 |
| min-idle | int | 5 | 最小空闲连接 |
| max-wait | long | 3000 | 获取连接最大等待（ms） |
| allowed-packages | List&lt;String&gt; | 空 | 反序列化白名单前缀 |
| unwrap-quoted-string | boolean | false | 兼容历史数据的去引号开关 |

## 🔄 从旧版本升级

本次修复包含若干**行为变更**，升级时请确认：

1. **首尾带引号的字符串不再被剥离**。旧版本写入 `"abc"` 读出来是 `abc`（数据被篡改）。
   若 Redis 中已有历史数据依赖该行为，设置 `ck.crydis.unwrap-quoted-string=true` 打开兼容模式。
2. **`getObject` 不再接受 `Object.class`**，且白名单外的 `@type` 会抛异常而不是降级为 `JSONObject`。
3. **`hmset` 拒绝 null/空 map**，`mset` 拒绝奇数个参数，`delete()` 拒绝空参数，
   `set`/`setObject` 拒绝 null 值，过期时间 ≤ 0 或不足 1 秒会抛 `IllegalArgumentException`。
4. **`Crydis.init()` 不再"只认第一次调用"**。重复初始化会关闭旧连接池并使用新配置重建，
   修复了 Spring 上下文刷新后静态入口仍指向已关闭连接池的问题。
5. **分布式锁**：`tryLock` 在 Redis 异常时返回 `false` 而非抛异常；`unlock(key)` 与
   `tryLock(key, expire, unit)` 已标记 `@Deprecated`，请迁移到 `tryLockWithToken` + `unlock(key, token)`。
6. **`KEYS` 相关 API 已废弃**，请改用 `scan(pattern)`。
7. **`getObject(key, clazz, Filter...)` 重载已移除**：fastjson2 的 `Context` 只保留**一个**
   autoType 处理器，原先"逐个 config(filter)"的写法会让后传入的过滤器整体覆盖白名单
   （已实测确认）。请改用 `getObject(key, clazz, String... allowedPackagePrefixes)` 传入前缀。

## 📊 项目结构

```
cn.cikian.crydis
├── CrydisManager.java                    # 非Spring项目管理器（流式 Builder）
├── model/
│   └── CrydisConfiguration.java          # 配置类
├── service/
│   ├── Crydis.java                       # 静态方法入口
│   └── RedisClient.java                  # Redis 客户端核心实现
├── autoconfigure/
│   └── CrydisAutoConfiguration.java      # Spring Boot 自动配置
└── exception/
    └── CikException.java                 # 自定义异常（暂未在内部使用）
```

## 🧪 测试

```bash
mvn test        # 需要本地 Redis（127.0.0.1:6379），使用 database 8 与 14
mvn clean package
```

- `CrydisTest`：功能覆盖
- `CrydisRegressionTest`：缺陷回归（AUTH 握手、读写对称、反序列化安全、锁语义、
  参数校验、生命周期与连接池）

## ❓ FAQ

### Q: Spring Boot 中如何禁用自动配置？

```java
@SpringBootApplication(exclude = CrydisAutoConfiguration.class)
public class Application { ... }
```

### Q: 支持 Redis Cluster / Sentinel 吗？

当前版本仅支持单机模式，Cluster 与 Sentinel 尚未支持。

### Q: 对象序列化失败怎么办？

- 实体类需要无参构造函数与 getter/setter
- 若含多态字段（JSON 中带 `@type`），需要把对应包前缀加入 `ck.crydis.allowed-packages`
- 不要使用 `Object.class` 作为目标类型

### Q: 如何避免连接告警 "pool has been closed"？

升级到 0.2.3+。该问题源于旧版本 `Crydis.init()` 忽略第二次初始化，
导致 Spring 上下文刷新后静态入口仍指向已关闭的旧连接池。

## 🤝 贡献

欢迎提交 Issue 和 Pull Request！

## 📄 License

[MIT](LICENSE)

## 🔗 相关资源

- **GitHub**：https://github.com/Cikian/crydis
- **作者网站**：https://www.cikian.cn
- **Issue 跟踪**：https://github.com/Cikian/crydis/issues

---

**最后更新**：2026-09-18
**当前版本**：0.2.3
**维护者**：[Cikian Chen](https://www.cikian.cn)
