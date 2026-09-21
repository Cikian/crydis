package cn.cikian.crydis.service;

import cn.cikian.crydis.model.CrydisConfiguration;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONReader;
import com.alibaba.fastjson2.JSONWriter;
import com.alibaba.fastjson2.filter.Filter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.DefaultJedisClientConfig;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.exceptions.JedisDataException;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisClientConfig;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.args.ListPosition;
import redis.clients.jedis.resps.Tuple;
import redis.clients.jedis.params.ScanParams;
import redis.clients.jedis.params.SetParams;
import redis.clients.jedis.resps.ScanResult;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.TimeUnit;


/**
 * Redis客户端封装（基于 Jedis 7.5.2+ 现代化重构）
 *
 * @author Cikian
 * @version 1.0
 * @since 2026-05-31
 */
public class RedisClient {
    private static final Logger log = LoggerFactory.getLogger(RedisClient.class);

    /**
     * 单次 SCAN 的游标批量大小
     */
    private static final int SCAN_BATCH = 512;

    private volatile JedisPool jedisPool;
    private final CrydisConfiguration configuration;

    /**
     * 最近一次 {@link #tryLock(String, long, TimeUnit)} 成功获取锁时使用的 token。
     * 仅用于兼容旧 API，新代码请使用 {@link #tryLockWithToken(String, long, TimeUnit)}。
     */
    private volatile String lastLockToken;

    public RedisClient(CrydisConfiguration configuration) {
        this.configuration = configuration;
        // 连接池改为懒加载：构造时不再建立连接池，避免"构造了但从未被托管"的池泄漏，
        // 也避免初始化失败被包装成 RuntimeException 时丢失原始异常类型。
    }

    /**
     * 懒加载并返回连接池（双重检查 + volatile，保证多线程下只创建一个池）。
     */
    public JedisPool getJedisPool() {
        JedisPool pool = this.jedisPool;
        if (pool == null) {
            synchronized (this) {
                pool = this.jedisPool;
                if (pool == null) {
                    pool = createJedisPool();
                    this.jedisPool = pool;
                }
            }
        }
        return pool;
    }

    private JedisPool createJedisPool() {
        JedisPoolConfig poolConfig = new JedisPoolConfig();
        poolConfig.setMaxTotal(configuration.getMaxActive() != null ? configuration.getMaxActive() : 50);
        poolConfig.setMaxIdle(configuration.getMaxIdle() != null ? configuration.getMaxIdle() : 10);
        poolConfig.setMinIdle(configuration.getMinIdle() != null ? configuration.getMinIdle() : 5);
        // setMaxWaitMillis 在 commons-pool2 中已标记 @Deprecated，改用 Duration 形式
        long maxWait = configuration.getMaxWait() != null ? configuration.getMaxWait() : 3000L;
        poolConfig.setMaxWait(Duration.ofMillis(maxWait));

        String host = configuration.getHost();
        if (host == null || host.trim().isEmpty()) {
            throw new IllegalArgumentException("Redis host 不能为空");
        }
        int port = configuration.getPort() != null ? configuration.getPort() : 6379;
        String password = configuration.getPassword();
        String user = configuration.getUser();
        int database = configuration.getDatabase() != null ? configuration.getDatabase() : 0;
        int timeout = configuration.getTimeout() != null ? configuration.getTimeout() : 3000;

        // 使用 Jedis 7.x 推荐的 ClientConfig 构建器替代废弃的多参数硬编码构造函数
        DefaultJedisClientConfig.Builder configBuilder = DefaultJedisClientConfig.builder()
                .connectionTimeoutMillis(timeout)
                .socketTimeoutMillis(timeout)
                .database(database);

        // 注意：Jedis 只要 user 非 null 就会发送两参数 AUTH（AUTH user pass），
        // 而两参数 AUTH 是 Redis 6.0 才支持的语法。此前这里在未配置 user 时
        // 强制填入 "default"，导致 Redis < 6.0 且配置了密码时握手直接失败：
        //   ERR wrong number of arguments for 'auth' command
        // 因此：未显式配置 user 时只发送单参数 AUTH password。
        if (password != null && !password.isEmpty()) {
            configBuilder.password(password);
            if (user != null && !user.isEmpty()) {
                configBuilder.user(user);
            }
        }

        JedisClientConfig clientConfig = configBuilder.build();
        log.info("Crydis RedisClient 初始化连接池 - {}:{}", host, port);
        return new JedisPool(poolConfig, new HostAndPort(host, port), clientConfig);
    }

    private Jedis getJedis() {
        return getJedisPool().getResource();
    }

    public void set(String key, String value) {
        if (value == null) {
            throw new IllegalArgumentException("set 的 value 不能为 null；如需删除请调用 delete(key)");
        }
        try (Jedis jedis = getJedis()) {
            jedis.set(key, value);
            log.debug("SET key={}", key);
        } catch (IllegalArgumentException e) {
            // 参数校验异常直接透传，避免被下面的 RuntimeException 掩盖真实原因
            throw e;
        } catch (Exception e) {
            log.error("SET key={} 失败", key, e);
            throw new RuntimeException("Redis SET操作失败", e);
        }
    }

    public void set(String key, String value, long expireTime, TimeUnit timeUnit) {
        long seconds = toSeconds(expireTime, timeUnit);
        try (Jedis jedis = getJedis()) {
            jedis.setex(key, seconds, value);
            log.debug("SET key={} expire={}s", key, seconds);
        } catch (IllegalArgumentException e) {
            // 参数校验异常直接透传，避免被下面的 RuntimeException 掩盖真实原因
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Redis SET操作失败", e);
        }
    }

    public void setNX(String key, String value) {
        try (Jedis jedis = getJedis()) {
            jedis.setnx(key, value);
            log.debug("SETNX key={}", key);
        } catch (IllegalArgumentException e) {
            // 参数校验异常直接透传，避免被下面的 RuntimeException 掩盖真实原因
            throw e;
        } catch (Exception e) {
            log.error("SETNX key={} 失败", key, e);
            throw new RuntimeException("Redis SETNX操作失败", e);
        }
    }

    public String get(String key) {
        try (Jedis jedis = getJedis()) {
            String value = jedis.get(key);
            log.debug("GET key={}", key);
            return unwrapJsonString(value);
        } catch (IllegalArgumentException e) {
            // 参数校验异常直接透传，避免被下面的 RuntimeException 掩盖真实原因
            throw e;
        } catch (Exception e) {
            log.error("GET key={} 失败", key, e);
            throw new RuntimeException("Redis GET操作失败", e);
        }
    }

    public void delete(String key) {
        try (Jedis jedis = getJedis()) {
            jedis.del(key);
            log.debug("DEL key={}", key);
        } catch (IllegalArgumentException e) {
            // 参数校验异常直接透传，避免被下面的 RuntimeException 掩盖真实原因
            throw e;
        } catch (Exception e) {
            log.error("DEL key={} 失败", key, e);
            throw new RuntimeException("Redis DELETE操作失败", e);
        }
    }

    public void delete(String... keys) {
        if (keys == null || keys.length == 0) {
            throw new IllegalArgumentException("delete 的 keys 不能为空");
        }
        try (Jedis jedis = getJedis()) {
            jedis.del(keys);
            log.debug("DEL keys={}", Arrays.toString(keys));
        } catch (IllegalArgumentException e) {
            // 参数校验异常直接透传，避免被下面的 RuntimeException 掩盖真实原因
            throw e;
        } catch (Exception e) {
            log.error("DEL keys={} 失败", Arrays.toString(keys), e);
            throw new RuntimeException("Redis DELETE操作失败", e);
        }
    }

    public boolean exists(String key) {
        try (Jedis jedis = getJedis()) {
            return jedis.exists(key);
        } catch (IllegalArgumentException e) {
            // 参数校验异常直接透传，避免被下面的 RuntimeException 掩盖真实原因
            throw e;
        } catch (Exception e) {
            log.error("EXISTS key={} 失败", key, e);
            throw new RuntimeException("Redis EXISTS操作失败", e);
        }
    }

    public boolean expire(String key, long expireTime, TimeUnit timeUnit) {
        try (Jedis jedis = getJedis()) {
            return jedis.expire(key, toSeconds(expireTime, timeUnit)) > 0;
        } catch (IllegalArgumentException e) {
            // 参数校验异常直接透传，避免被下面的 RuntimeException 掩盖真实原因
            throw e;
        } catch (Exception e) {
            log.error("EXPIRE key={} 失败", key, e);
            throw new RuntimeException("Redis EXPIRE操作失败", e);
        }
    }

    public long ttl(String key) {
        try (Jedis jedis = getJedis()) {
            return jedis.ttl(key);
        } catch (IllegalArgumentException e) {
            // 参数校验异常直接透传，避免被下面的 RuntimeException 掩盖真实原因
            throw e;
        } catch (Exception e) {
            log.error("TTL key={} 失败", key, e);
            throw new RuntimeException("Redis TTL操作失败", e);
        }
    }

    public void hset(String key, String field, String value) {
        try (Jedis jedis = getJedis()) {
            jedis.hset(key, field, value);
            log.debug("HSET key={} field={}", key, field);
        } catch (IllegalArgumentException e) {
            // 参数校验异常直接透传，避免被下面的 RuntimeException 掩盖真实原因
            throw e;
        } catch (Exception e) {
            log.error("HSET key={} field={} 失败", key, field, e);
            throw new RuntimeException("Redis HSET操作失败", e);
        }
    }

    /**
     * 批量设置哈希字段。
     *
     * <p>使用 Jedis 7 的原生 {@code HSET key field value [field value ...]} 一次性提交，
     * 避免旧实现"循环单字段 hset"带来的 N 次网络往返。</p>
     *
     * @param hash 字段值映射，不能为 null
     */
    public void hmset(String key, Map<String, String> hash) {
        if (hash == null || hash.isEmpty()) {
            throw new IllegalArgumentException("hmset 的 hash 不能为 null 或空");
        }
        try (Jedis jedis = getJedis()) {
            hsetAll(jedis, key, hash);
            log.debug("HMSET key={} fieldCount={}", key, hash.size());
        } catch (IllegalArgumentException e) {
            // 参数校验异常直接透传，避免被下面的 RuntimeException 掩盖真实原因
            throw e;
        } catch (Exception e) {
            log.error("HMSET key={} 失败", key, e);
            throw new RuntimeException("Redis HMSET操作失败", e);
        }
    }

    /**
     * 批量写哈希字段。
     *
     * <p>优先使用 Jedis 7 的原生单条命令 {@code HSET key f1 v1 f2 v2}（一次网络往返），
     * 该语法要求 Redis >= 4.0；在 Redis 3.x 等老服务端上会返回 unknown command，
     * 此时自动回退为逐字段 HSET，以保证兼容性（代价是 N 次网络往返）。</p>
     */
    private void hsetAll(Jedis jedis, String key, Map<String, String> hash) {
        try {
            jedis.hset(key, hash);
        } catch (JedisDataException e) {
            log.debug("服务端不支持多字段 HSET（Redis < 4.0），回退为逐字段写入 key={}", key);
            for (Map.Entry<String, String> entry : hash.entrySet()) {
                jedis.hset(key, entry.getKey(), entry.getValue());
            }
        }
    }

    public String hget(String key, String field) {
        try (Jedis jedis = getJedis()) {
            return unwrapJsonString(jedis.hget(key, field));
        } catch (IllegalArgumentException e) {
            // 参数校验异常直接透传，避免被下面的 RuntimeException 掩盖真实原因
            throw e;
        } catch (Exception e) {
            log.error("HGET key={} field={} 失败", key, field, e);
            throw new RuntimeException("Redis HGET操作失败", e);
        }
    }

    public Map<String, String> hgetAll(String key) {
        try (Jedis jedis = getJedis()) {
            Map<String, String> result = jedis.hgetAll(key);
            result.replaceAll((k, v) -> unwrapJsonString(v));
            return result;
        } catch (IllegalArgumentException e) {
            // 参数校验异常直接透传，避免被下面的 RuntimeException 掩盖真实原因
            throw e;
        } catch (Exception e) {
            log.error("HGETALL key={} 失败", key, e);
            throw new RuntimeException("Redis HGETALL操作失败", e);
        }
    }

    public void hdel(String key, String... fields) {
        try (Jedis jedis = getJedis()) {
            jedis.hdel(key, fields);
            log.debug("HDEL key={} fields={}", key, Arrays.toString(fields));
        } catch (IllegalArgumentException e) {
            // 参数校验异常直接透传，避免被下面的 RuntimeException 掩盖真实原因
            throw e;
        } catch (Exception e) {
            log.error("HDEL key={} 失败", key, e);
            throw new RuntimeException("Redis HDEL操作失败", e);
        }
    }

    public boolean hexists(String key, String field) {
        try (Jedis jedis = getJedis()) {
            return jedis.hexists(key, field);
        } catch (IllegalArgumentException e) {
            // 参数校验异常直接透传，避免被下面的 RuntimeException 掩盖真实原因
            throw e;
        } catch (Exception e) {
            log.error("HEXISTS key={} field={} 失败", key, field, e);
            throw new RuntimeException("Redis HEXISTS操作失败", e);
        }
    }

    public Long incr(String key) {
        try (Jedis jedis = getJedis()) {
            return jedis.incr(key);
        } catch (IllegalArgumentException e) {
            // 参数校验异常直接透传，避免被下面的 RuntimeException 掩盖真实原因
            throw e;
        } catch (Exception e) {
            log.error("INCR key={} 失败", key, e);
            throw new RuntimeException("Redis INCR操作失败", e);
        }
    }

    public Long decr(String key) {
        try (Jedis jedis = getJedis()) {
            return jedis.decr(key);
        } catch (IllegalArgumentException e) {
            // 参数校验异常直接透传，避免被下面的 RuntimeException 掩盖真实原因
            throw e;
        } catch (Exception e) {
            log.error("DECR key={} 失败", key, e);
            throw new RuntimeException("Redis DECR操作失败", e);
        }
    }

    public Long incrBy(String key, long increment) {
        try (Jedis jedis = getJedis()) {
            return jedis.incrBy(key, increment);
        } catch (IllegalArgumentException e) {
            // 参数校验异常直接透传，避免被下面的 RuntimeException 掩盖真实原因
            throw e;
        } catch (Exception e) {
            log.error("INCRBY key={} 失败", key, e);
            throw new RuntimeException("Redis INCRBY操作失败", e);
        }
    }

    public Long decrBy(String key, long decrement) {
        try (Jedis jedis = getJedis()) {
            return jedis.decrBy(key, decrement);
        } catch (IllegalArgumentException e) {
            // 参数校验异常直接透传，避免被下面的 RuntimeException 掩盖真实原因
            throw e;
        } catch (Exception e) {
            log.error("DECRBY key={} 失败", key, e);
            throw new RuntimeException("Redis DECRBY操作失败", e);
        }
    }

    public void lpush(String key, String... values) {
        try (Jedis jedis = getJedis()) {
            jedis.lpush(key, values);
            log.debug("LPUSH key={} values={}", key, Arrays.toString(values));
        } catch (IllegalArgumentException e) {
            // 参数校验异常直接透传，避免被下面的 RuntimeException 掩盖真实原因
            throw e;
        } catch (Exception e) {
            log.error("LPUSH key={} 失败", key, e);
            throw new RuntimeException("Redis LPUSH操作失败", e);
        }
    }

    public void rpush(String key, String... values) {
        try (Jedis jedis = getJedis()) {
            jedis.rpush(key, values);
            log.debug("RPUSH key={} values={}", key, Arrays.toString(values));
        } catch (IllegalArgumentException e) {
            // 参数校验异常直接透传，避免被下面的 RuntimeException 掩盖真实原因
            throw e;
        } catch (Exception e) {
            log.error("RPUSH key={} 失败", key, e);
            throw new RuntimeException("Redis RPUSH操作失败", e);
        }
    }

    public String lpop(String key) {
        try (Jedis jedis = getJedis()) {
            return unwrapJsonString(jedis.lpop(key));
        } catch (IllegalArgumentException e) {
            // 参数校验异常直接透传，避免被下面的 RuntimeException 掩盖真实原因
            throw e;
        } catch (Exception e) {
            log.error("LPOP key={} 失败", key, e);
            throw new RuntimeException("Redis LPOP操作失败", e);
        }
    }

    public String rpop(String key) {
        try (Jedis jedis = getJedis()) {
            return unwrapJsonString(jedis.rpop(key));
        } catch (IllegalArgumentException e) {
            // 参数校验异常直接透传，避免被下面的 RuntimeException 掩盖真实原因
            throw e;
        } catch (Exception e) {
            log.error("RPOP key={} 失败", key, e);
            throw new RuntimeException("Redis RPOP操作失败", e);
        }
    }

    public List<String> lrange(String key, long start, long end) {
        try (Jedis jedis = getJedis()) {
            List<String> result = jedis.lrange(key, start, end);
            unwrapAll(result);
            return result;
        } catch (IllegalArgumentException e) {
            // 参数校验异常直接透传，避免被下面的 RuntimeException 掩盖真实原因
            throw e;
        } catch (Exception e) {
            log.error("LRANGE key={} 失败", key, e);
            throw new RuntimeException("Redis LRANGE操作失败", e);
        }
    }

    public Long llen(String key) {
        try (Jedis jedis = getJedis()) {
            return jedis.llen(key);
        } catch (IllegalArgumentException e) {
            // 参数校验异常直接透传，避免被下面的 RuntimeException 掩盖真实原因
            throw e;
        } catch (Exception e) {
            log.error("LLEN key={} 失败", key, e);
            throw new RuntimeException("Redis LLEN操作失败", e);
        }
    }

    public void sadd(String key, String... members) {
        try (Jedis jedis = getJedis()) {
            jedis.sadd(key, members);
            log.debug("SADD key={} members={}", key, Arrays.toString(members));
        } catch (IllegalArgumentException e) {
            // 参数校验异常直接透传，避免被下面的 RuntimeException 掩盖真实原因
            throw e;
        } catch (Exception e) {
            log.error("SADD key={} 失败", key, e);
            throw new RuntimeException("Redis SADD操作失败", e);
        }
    }

    public Set<String> smembers(String key) {
        try (Jedis jedis = getJedis()) {
            Set<String> result = jedis.smembers(key);
            Set<String> unwrapped = new HashSet<>();
            for (String member : result) {
                unwrapped.add(unwrapJsonString(member));
            }
            return unwrapped;
        } catch (IllegalArgumentException e) {
            // 参数校验异常直接透传，避免被下面的 RuntimeException 掩盖真实原因
            throw e;
        } catch (Exception e) {
            log.error("SMEMBERS key={} 失败", key, e);
            throw new RuntimeException("Redis SMEMBERS操作失败", e);
        }
    }

    public boolean sismember(String key, String member) {
        try (Jedis jedis = getJedis()) {
            return jedis.sismember(key, member);
        } catch (IllegalArgumentException e) {
            // 参数校验异常直接透传，避免被下面的 RuntimeException 掩盖真实原因
            throw e;
        } catch (Exception e) {
            log.error("SISMEMBER key={} 失败", key, e);
            throw new RuntimeException("Redis SISMEMBER操作失败", e);
        }
    }

    public void srem(String key, String... members) {
        try (Jedis jedis = getJedis()) {
            jedis.srem(key, members);
            log.debug("SREM key={} members={}", key, Arrays.toString(members));
        } catch (IllegalArgumentException e) {
            // 参数校验异常直接透传，避免被下面的 RuntimeException 掩盖真实原因
            throw e;
        } catch (Exception e) {
            log.error("SREM key={} 失败", key, e);
            throw new RuntimeException("Redis SREM操作失败", e);
        }
    }

    public <T> void setObject(String key, T value) {
        String json = serialize(value);
        try (Jedis jedis = getJedis()) {
            jedis.set(key, json);
            log.debug("SET OBJECT key={}, valueLength={}", key, json == null ? 0 : json.length());
        } catch (IllegalArgumentException e) {
            // 参数校验异常直接透传，避免被下面的 RuntimeException 掩盖真实原因
            throw e;
        } catch (Exception e) {
            log.error("SET OBJECT key={} 失败", key, e);
            throw new RuntimeException("Redis SET操作失败", e);
        }
    }

    public <T> void setObject(String key, T value, long expireTime, TimeUnit timeUnit) {
        String json = serialize(value);
        long seconds = toSeconds(expireTime, timeUnit);
        try (Jedis jedis = getJedis()) {
            jedis.setex(key, seconds, json);
            log.debug("SET OBJECT key={}, expire={}s", key, seconds);
        } catch (IllegalArgumentException e) {
            // 参数校验异常直接透传，避免被下面的 RuntimeException 掩盖真实原因
            throw e;
        } catch (Exception e) {
            log.error("SET OBJECT key={} 失败", key, e);
            throw new RuntimeException("Redis SETEX操作失败", e);
        }
    }

    /**
     * 反序列化对象。
     *
     * <p>白名单默认收窄为"只放行目标类型自身"，未配置 {@code crydis.allowed-packages} 时
     * 也可安全还原普通实体与同包嵌套类型。</p>
     *
     * @param key   键名
     * @param clazz 目标类型，不能为 null 或 Object.class
     * @return 反序列化结果，key 不存在时返回 null
     */
    public <T> T getObject(String key, Class<T> clazz) {
        return getObject(key, clazz, (String[]) null);
    }

    /**
     * 反序列化对象，并指定额外的 autoType 白名单前缀。
     *
     * <p>安全策略（与旧版行为不同，旧版在未配置白名单时使用
     * {@code SupportAutoType} 全局盲放，存在反序列化 RCE 风险）：</p>
     * <ol>
     *   <li>目标类型不允许为 {@code null} 或 {@code Object.class}：这两种情况下
     *       fastjson2 会完全忽略过滤器，直接按 JSON 里的 {@code @type} 实例化任意类。</li>
     *   <li>白名单 = 配置项 {@code crydis.allowed-packages} ∪ 本次传入的前缀 ∪ 目标类型
     *       {@code clazz} 自身，保证正常数据可还原。</li>
     *   <li>JSON 中的 {@code @type} 不在白名单内时直接抛异常，不再静默降级为
     *       {@code JSONObject}（旧版会导致调用方在使用结果时才收到 ClassCastException）。</li>
     * </ol>
     *
     * <p>注意 fastjson2 的白名单是<b>文本前缀</b>匹配且不支持 {@code *} 通配符：
     * {@code "cn.foo."} 放行 {@code cn.foo} 包下所有类，{@code "cn.foo.*"} 则什么都放行不了。</p>
     *
     * @param key                    键名
     * @param clazz                  目标类型，不能为 null 或 Object.class
     * @param allowedPackagePrefixes 额外放行的包/类名前缀，可为 null
     */
    public <T> T getObject(String key, Class<T> clazz, String... allowedPackagePrefixes) {
        try (Jedis jedis = getJedis()) {
            String json = jedis.get(key);
            log.debug("GET OBJECT key={}, rawValueLength={}", key, json == null ? 0 : json.length());
            return deserialize(json, clazz, buildAutoTypeFilter(clazz, allowedPackagePrefixes));
        } catch (IllegalArgumentException e) {
            // 参数校验异常直接透传，避免被下面的 RuntimeException 掩盖真实原因
            throw e;
        } catch (Exception e) {
            log.error("GET OBJECT key={} 失败", key, e);
            throw new RuntimeException("Redis GET操作失败", e);
        }
    }


    public Long append(String key, String value) {
        try (Jedis jedis = getJedis()) {
            Long result = jedis.append(key, value);
            log.debug("APPEND key={} value={}", key, value);
            return result;
        } catch (IllegalArgumentException e) {
            // 参数校验异常直接透传，避免被下面的 RuntimeException 掩盖真实原因
            throw e;
        } catch (Exception e) {
            log.error("APPEND key={} 失败", key, e);
            throw new RuntimeException("Redis APPEND操作失败", e);
        }
    }

    public Long strlen(String key) {
        try (Jedis jedis = getJedis()) {
            return jedis.strlen(key);
        } catch (IllegalArgumentException e) {
            // 参数校验异常直接透传，避免被下面的 RuntimeException 掩盖真实原因
            throw e;
        } catch (Exception e) {
            log.error("STRLEN key={} 失败", key, e);
            throw new RuntimeException("Redis STRLEN操作失败", e);
        }
    }

    public String getSet(String key, String value) {
        try (Jedis jedis = getJedis()) {
            String result = jedis.getSet(key, value);
            log.debug("GETSET key={}", key);
            return unwrapJsonString(result);
        } catch (IllegalArgumentException e) {
            // 参数校验异常直接透传，避免被下面的 RuntimeException 掩盖真实原因
            throw e;
        } catch (Exception e) {
            log.error("GETSET key={} 失败", key, e);
            throw new RuntimeException("Redis GETSET操作失败", e);
        }
    }

    public List<String> mget(String... keys) {
        try (Jedis jedis = getJedis()) {
            List<String> result = jedis.mget(keys);
            unwrapAll(result);
            return result;
        } catch (IllegalArgumentException e) {
            // 参数校验异常直接透传，避免被下面的 RuntimeException 掩盖真实原因
            throw e;
        } catch (Exception e) {
            log.error("MGET keys={} 失败", Arrays.toString(keys), e);
            throw new RuntimeException("Redis MGET操作失败", e);
        }
    }

    public void mset(String... keyValuePairs) {
        if (keyValuePairs == null || keyValuePairs.length == 0 || keyValuePairs.length % 2 != 0) {
            throw new IllegalArgumentException("mset 需要偶数个非空参数（key value 成对），当前长度：" + (keyValuePairs == null ? 0 : keyValuePairs.length));
        }
        try (Jedis jedis = getJedis()) {
            jedis.mset(keyValuePairs);
            log.debug("MSET keyValuePairs={}", Arrays.toString(keyValuePairs));
        } catch (IllegalArgumentException e) {
            // 参数校验异常直接透传，避免被下面的 RuntimeException 掩盖真实原因
            throw e;
        } catch (Exception e) {
            log.error("MSET 失败", e);
            throw new RuntimeException("Redis MSET操作失败", e);
        }
    }

    public Set<String> hkeys(String key) {
        try (Jedis jedis = getJedis()) {
            return jedis.hkeys(key);
        } catch (IllegalArgumentException e) {
            // 参数校验异常直接透传，避免被下面的 RuntimeException 掩盖真实原因
            throw e;
        } catch (Exception e) {
            log.error("HKEYS key={} 失败", key, e);
            throw new RuntimeException("Redis HKEYS操作失败", e);
        }
    }

    public List<String> hvals(String key) {
        try (Jedis jedis = getJedis()) {
            List<String> result = jedis.hvals(key);
            unwrapAll(result);
            return result;
        } catch (IllegalArgumentException e) {
            // 参数校验异常直接透传，避免被下面的 RuntimeException 掩盖真实原因
            throw e;
        } catch (Exception e) {
            log.error("HVALS key={} 失败", key, e);
            throw new RuntimeException("Redis HVALS操作失败", e);
        }
    }

    public Long hlen(String key) {
        try (Jedis jedis = getJedis()) {
            return jedis.hlen(key);
        } catch (IllegalArgumentException e) {
            // 参数校验异常直接透传，避免被下面的 RuntimeException 掩盖真实原因
            throw e;
        } catch (Exception e) {
            log.error("HLEN key={} 失败", key, e);
            throw new RuntimeException("Redis HLEN操作失败", e);
        }
    }

    public Long hincrBy(String key, String field, long increment) {
        try (Jedis jedis = getJedis()) {
            return jedis.hincrBy(key, field, increment);
        } catch (IllegalArgumentException e) {
            // 参数校验异常直接透传，避免被下面的 RuntimeException 掩盖真实原因
            throw e;
        } catch (Exception e) {
            log.error("HINCRBY key={} field={} 失败", key, field, e);
            throw new RuntimeException("Redis HINCRBY操作失败", e);
        }
    }

    public Long zadd(String key, double score, String member) {
        try (Jedis jedis = getJedis()) {
            Long result = jedis.zadd(key, score, member);
            log.debug("ZADD key={} score={} member={}", key, score, member);
            return result;
        } catch (IllegalArgumentException e) {
            // 参数校验异常直接透传，避免被下面的 RuntimeException 掩盖真实原因
            throw e;
        } catch (Exception e) {
            log.error("ZADD key={} 失败", key, e);
            throw new RuntimeException("Redis ZADD操作失败", e);
        }
    }

    public Long zadd(String key, Map<String, Double> scoreMembers) {
        try (Jedis jedis = getJedis()) {
            Long result = jedis.zadd(key, scoreMembers);
            log.debug("ZADD key={} scoreMembers={}", key, scoreMembers);
            return result;
        } catch (IllegalArgumentException e) {
            // 参数校验异常直接透传，避免被下面的 RuntimeException 掩盖真实原因
            throw e;
        } catch (Exception e) {
            log.error("ZADD key={} 失败", key, e);
            throw new RuntimeException("Redis ZADD操作失败", e);
        }
    }

    public List<String> zrange(String key, long start, long end) {
        try (Jedis jedis = getJedis()) {
            return jedis.zrange(key, start, end);
        } catch (IllegalArgumentException e) {
            // 参数校验异常直接透传，避免被下面的 RuntimeException 掩盖真实原因
            throw e;
        } catch (Exception e) {
            log.error("ZRANGE key={} 失败", key, e);
            throw new RuntimeException("Redis ZRANGE操作失败", e);
        }
    }

    public List<Tuple> zrangeWithScores(String key, long start, long end) {
        try (Jedis jedis = getJedis()) {
            return jedis.zrangeWithScores(key, start, end);
        } catch (IllegalArgumentException e) {
            // 参数校验异常直接透传，避免被下面的 RuntimeException 掩盖真实原因
            throw e;
        } catch (Exception e) {
            log.error("ZRANGEWITHSCORES key={} 失败", key, e);
            throw new RuntimeException("Redis ZRANGEWITHSCORES操作失败", e);
        }
    }

    public Long zrank(String key, String member) {
        try (Jedis jedis = getJedis()) {
            return jedis.zrank(key, member);
        } catch (IllegalArgumentException e) {
            // 参数校验异常直接透传，避免被下面的 RuntimeException 掩盖真实原因
            throw e;
        } catch (Exception e) {
            log.error("ZRANK key={} member={} 失败", key, member, e);
            throw new RuntimeException("Redis ZRANK操作失败", e);
        }
    }

    public Double zscore(String key, String member) {
        try (Jedis jedis = getJedis()) {
            return jedis.zscore(key, member);
        } catch (IllegalArgumentException e) {
            // 参数校验异常直接透传，避免被下面的 RuntimeException 掩盖真实原因
            throw e;
        } catch (Exception e) {
            log.error("ZSCORE key={} member={} 失败", key, member, e);
            throw new RuntimeException("Redis ZSCORE操作失败", e);
        }
    }

    public Long zrem(String key, String... members) {
        try (Jedis jedis = getJedis()) {
            Long result = jedis.zrem(key, members);
            log.debug("ZREM key={} members={}", key, Arrays.toString(members));
            return result;
        } catch (IllegalArgumentException e) {
            // 参数校验异常直接透传，避免被下面的 RuntimeException 掩盖真实原因
            throw e;
        } catch (Exception e) {
            log.error("ZREM key={} 失败", key, e);
            throw new RuntimeException("Redis ZREM操作失败", e);
        }
    }

    public Long zcard(String key) {
        try (Jedis jedis = getJedis()) {
            return jedis.zcard(key);
        } catch (IllegalArgumentException e) {
            // 参数校验异常直接透传，避免被下面的 RuntimeException 掩盖真实原因
            throw e;
        } catch (Exception e) {
            log.error("ZCARD key={} 失败", key, e);
            throw new RuntimeException("Redis ZCARD操作失败", e);
        }
    }

    public Long zcount(String key, double min, double max) {
        try (Jedis jedis = getJedis()) {
            return jedis.zcount(key, min, max);
        } catch (IllegalArgumentException e) {
            // 参数校验异常直接透传，避免被下面的 RuntimeException 掩盖真实原因
            throw e;
        } catch (Exception e) {
            log.error("ZCOUNT key={} 失败", key, e);
            throw new RuntimeException("Redis ZCOUNT操作失败", e);
        }
    }

    public Double zincrby(String key, double increment, String member) {
        try (Jedis jedis = getJedis()) {
            return jedis.zincrby(key, increment, member);
        } catch (IllegalArgumentException e) {
            // 参数校验异常直接透传，避免被下面的 RuntimeException 掩盖真实原因
            throw e;
        } catch (Exception e) {
            log.error("ZINCRBY key={} 失败", key, e);
            throw new RuntimeException("Redis ZINCRBY操作失败", e);
        }
    }

    public Long scard(String key) {
        try (Jedis jedis = getJedis()) {
            return jedis.scard(key);
        } catch (IllegalArgumentException e) {
            // 参数校验异常直接透传，避免被下面的 RuntimeException 掩盖真实原因
            throw e;
        } catch (Exception e) {
            log.error("SCARD key={} 失败", key, e);
            throw new RuntimeException("Redis SCARD操作失败", e);
        }
    }

    public String spop(String key) {
        try (Jedis jedis = getJedis()) {
            return unwrapJsonString(jedis.spop(key));
        } catch (IllegalArgumentException e) {
            // 参数校验异常直接透传，避免被下面的 RuntimeException 掩盖真实原因
            throw e;
        } catch (Exception e) {
            log.error("SPOP key={} 失败", key, e);
            throw new RuntimeException("Redis SPOP操作失败", e);
        }
    }

    public Set<String> spop(String key, long count) {
        try (Jedis jedis = getJedis()) {
            Set<String> result = jedis.spop(key, (int) count);
            Set<String> unwrapped = new HashSet<>();
            for (String member : result) {
                unwrapped.add(unwrapJsonString(member));
            }
            return unwrapped;
        } catch (IllegalArgumentException e) {
            // 参数校验异常直接透传，避免被下面的 RuntimeException 掩盖真实原因
            throw e;
        } catch (Exception e) {
            log.error("SPOP key={} count={} 失败", key, count, e);
            throw new RuntimeException("Redis SPOP操作失败", e);
        }
    }

    public String srandmember(String key) {
        try (Jedis jedis = getJedis()) {
            return unwrapJsonString(jedis.srandmember(key));
        } catch (IllegalArgumentException e) {
            // 参数校验异常直接透传，避免被下面的 RuntimeException 掩盖真实原因
            throw e;
        } catch (Exception e) {
            log.error("SRANDMEMBER key={} 失败", key, e);
            throw new RuntimeException("Redis SRANDMEMBER操作失败", e);
        }
    }

    public List<String> srandmember(String key, int count) {
        try (Jedis jedis = getJedis()) {
            List<String> result = jedis.srandmember(key, count);
            unwrapAll(result);
            return result;
        } catch (IllegalArgumentException e) {
            // 参数校验异常直接透传，避免被下面的 RuntimeException 掩盖真实原因
            throw e;
        } catch (Exception e) {
            log.error("SRANDMEMBER key={} count={} 失败", key, count, e);
            throw new RuntimeException("Redis SRANDMEMBER操作失败", e);
        }
    }

    public Long sinterstore(String destination, String... keys) {
        try (Jedis jedis = getJedis()) {
            return jedis.sinterstore(destination, keys);
        } catch (IllegalArgumentException e) {
            // 参数校验异常直接透传，避免被下面的 RuntimeException 掩盖真实原因
            throw e;
        } catch (Exception e) {
            log.error("SINTERSTORE destination={} keys={} 失败", destination, Arrays.toString(keys), e);
            throw new RuntimeException("Redis SINTERSTORE操作失败", e);
        }
    }

    public Long sunionstore(String destination, String... keys) {
        try (Jedis jedis = getJedis()) {
            return jedis.sunionstore(destination, keys);
        } catch (IllegalArgumentException e) {
            // 参数校验异常直接透传，避免被下面的 RuntimeException 掩盖真实原因
            throw e;
        } catch (Exception e) {
            log.error("SUNIONSTORE destination={} keys={} 失败", destination, Arrays.toString(keys), e);
            throw new RuntimeException("Redis SUNIONSTORE操作失败", e);
        }
    }

    public String lindex(String key, long index) {
        try (Jedis jedis = getJedis()) {
            return unwrapJsonString(jedis.lindex(key, index));
        } catch (IllegalArgumentException e) {
            // 参数校验异常直接透传，避免被下面的 RuntimeException 掩盖真实原因
            throw e;
        } catch (Exception e) {
            log.error("LINDEX key={} index={} 失败", key, index, e);
            throw new RuntimeException("Redis LINDEX操作失败", e);
        }
    }

    public String lset(String key, long index, String value) {
        try (Jedis jedis = getJedis()) {
            return jedis.lset(key, index, value);
        } catch (IllegalArgumentException e) {
            // 参数校验异常直接透传，避免被下面的 RuntimeException 掩盖真实原因
            throw e;
        } catch (Exception e) {
            log.error("LSET key={} index={} 失败", key, index, e);
            throw new RuntimeException("Redis LSET操作失败", e);
        }
    }

    public Long linsert(String key, boolean before, String pivot, String value) {
        try (Jedis jedis = getJedis()) {
            return jedis.linsert(key, before ? ListPosition.BEFORE : ListPosition.AFTER, pivot, value);
        } catch (IllegalArgumentException e) {
            // 参数校验异常直接透传，避免被下面的 RuntimeException 掩盖真实原因
            throw e;
        } catch (Exception e) {
            log.error("LINSERT key={} 失败", key, e);
            throw new RuntimeException("Redis LINSERT操作失败", e);
        }
    }

    public String ltrim(String key, long start, long end) {
        try (Jedis jedis = getJedis()) {
            return jedis.ltrim(key, start, end);
        } catch (IllegalArgumentException e) {
            // 参数校验异常直接透传，避免被下面的 RuntimeException 掩盖真实原因
            throw e;
        } catch (Exception e) {
            log.error("LTRIM key={} 失败", key, e);
            throw new RuntimeException("Redis LTRIM操作失败", e);
        }
    }

    /**
     * 尝试获取分布式锁，并返回本次锁的 token（成功时非 null）。
     *
     * <p>这是<b>推荐用法</b>：token 由调用方持有并用于 {@link #unlock(String, String)}，
     * 从而保证只释放自己持有的锁。旧版 {@link #tryLock(String, long, TimeUnit)}
     * 内部随机生成 token 且不返回给调用方，导致只能使用不安全的
     * {@link #unlock(String)}，属于设计缺陷。</p>
     *
     * <pre>
     * String token = Crydis.getRedisClient().tryLockWithToken("lock:order:1", 30, TimeUnit.SECONDS);
     * if (token != null) {
     *     try { ... } finally { Crydis.getRedisClient().unlock("lock:order:1", token); }
     * }
     * </pre>
     *
     * @return 成功返回锁 token，失败或发生 Redis 异常时返回 null（获取锁失败属于正常控制流，不抛异常）
     */
    public String tryLockWithToken(String key, long expireTime, TimeUnit timeUnit) {
        String token = UUID.randomUUID().toString();
        return tryLockWithToken(key, token, expireTime, timeUnit) ? token : null;
    }

    /**
     * 使用指定 token 尝试获取分布式锁（token 需全局唯一，通常为 UUID）。
     *
     * @return true 表示获取成功；false 表示锁已被占用或 Redis 不可用（异常被吞掉并记录 WARN 日志）
     */
    public boolean tryLockWithToken(String key, String token, long expireTime, TimeUnit timeUnit) {
        if (key == null || key.isEmpty()) {
            throw new IllegalArgumentException("锁的 key 不能为空");
        }
        if (token == null || token.isEmpty()) {
            throw new IllegalArgumentException("锁的 token 不能为空，请使用 UUID 等全局唯一值");
        }
        long seconds = toSeconds(expireTime, timeUnit);
        try (Jedis jedis = getJedis()) {
            String result = jedis.set(key, token, SetParams.setParams().nx().ex(seconds));
            boolean locked = "OK".equals(result);
            if (locked) {
                log.debug("获取锁成功 key={} expire={}s", key, seconds);
            }
            return locked;
        } catch (IllegalArgumentException e) {
            // 参数校验异常直接透传，避免被下面的 RuntimeException 掩盖真实原因
            throw e;
        } catch (Exception e) {
            // 获取锁失败是正常控制流，不应抛出异常打断调用方的业务逻辑
            log.warn("获取锁失败（Redis 异常），key={}，返回 false", key, e);
            return false;
        }
    }

    /**
     * 尝试获取分布式锁（兼容旧 API）。
     *
     * @return true 表示获取成功
     * @deprecated 该重载内部生成 token 后不返回，调用方只能使用不安全的
     * {@link #unlock(String)} 释放锁，存在误删他人锁的风险。
     * 请改用 {@link #tryLockWithToken(String, long, TimeUnit)}。
     */
    @Deprecated
    public boolean tryLock(String key, long expireTime, TimeUnit timeUnit) {
        String token = tryLockWithToken(key, expireTime, timeUnit);
        if (token == null) {
            return false;
        }
        // 仅为兼容旧行为保存 token；并发调用同一实例时该字段会被覆盖，故不推荐使用本重载
        this.lastLockToken = token;
        return true;
    }

    /**
     * 使用指定 token 尝试获取分布式锁（兼容旧 API）。
     *
     * @deprecated 请改用 {@link #tryLockWithToken(String, String, long, TimeUnit)}，
     * 后者在 Redis 异常时返回 false 而不是抛异常。
     */
    @Deprecated
    public boolean tryLock(String key, String value, long expireTime, TimeUnit timeUnit) {
        return tryLockWithToken(key, value, expireTime, timeUnit);
    }

    /**
     * 获取最近一次通过 {@link #tryLock(String, long, TimeUnit)} 成功获取锁时使用的 token。
     */
    public String getLastLockToken() {
        return lastLockToken;
    }

    /**
     * 释放锁（不校验归属）。
     *
     * @deprecated 该实现直接 DEL，任何持有相同 key 的进程都能删除他人的锁，
     * 可能破坏互斥语义。请使用 {@link #unlock(String, String)} 做归属校验后再删除。
     */
    @Deprecated
    public void unlock(String key) {
        try (Jedis jedis = getJedis()) {
            jedis.del(key);
            log.debug("释放锁（未校验归属）key={}", key);
        } catch (IllegalArgumentException e) {
            // 参数校验异常直接透传，避免被下面的 RuntimeException 掩盖真实原因
            throw e;
        } catch (Exception e) {
            log.error("释放锁失败 key={}", key, e);
            throw new RuntimeException("Redis 释放锁失败", e);
        }
    }

    /**
     * 安全释放锁：仅当锁的当前值等于 expectedValue 时才删除（Lua 脚本保证原子性）。
     *
     * @return true 表示锁确实由本次调用释放；false 表示锁不属于自己或已过期
     */
    public boolean unlock(String key, String expectedValue) {
        if (key == null || key.isEmpty()) {
            throw new IllegalArgumentException("锁的 key 不能为空");
        }
        if (expectedValue == null || expectedValue.isEmpty()) {
            throw new IllegalArgumentException("expectedValue 不能为空，请传入获取锁时使用的 token");
        }
        String luaScript = "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end";
        try (Jedis jedis = getJedis()) {
            Object raw = jedis.eval(luaScript, Collections.singletonList(key), Collections.singletonList(expectedValue));
            boolean success = raw instanceof Number && ((Number) raw).longValue() > 0;
            if (!success) {
                log.debug("释放锁失败（token 不匹配或锁已过期）key={}", key);
            }
            return success;
        } catch (IllegalArgumentException e) {
            // 参数校验异常直接透传，避免被下面的 RuntimeException 掩盖真实原因
            throw e;
        } catch (Exception e) {
            log.error("释放锁失败 key={}", key, e);
            throw new RuntimeException("Redis 释放锁失败", e);
        }
    }

    /**
     * 使用 SCAN 游标分批获取匹配的键（生产环境推荐）。
     *
     * <p>相比 {@link #keys(String)}，SCAN 不会长时间阻塞 Redis，适合大 key 空间。</p>
     *
     * @param pattern 匹配模式，如 {@code user:*}
     * @return 匹配到的键集合（注意：SCAN 在 rehash 期间可能返回重复键，已用 Set 去重；
     * 但并发写入时无法保证快照一致性）
     */
    public Set<String> scan(String pattern) {
        Set<String> result = new LinkedHashSet<String>();
        String cursor = ScanParams.SCAN_POINTER_START;
        ScanParams params = new ScanParams().match(pattern).count(SCAN_BATCH);
        try (Jedis jedis = getJedis()) {
            do {
                ScanResult<String> scanResult = jedis.scan(cursor, params);
                result.addAll(scanResult.getResult());
                cursor = scanResult.getCursor();
            } while (!ScanParams.SCAN_POINTER_START.equals(cursor));
            log.debug("SCAN pattern={}, count={}", pattern, result.size());
            return result;
        } catch (IllegalArgumentException e) {
            // 参数校验异常直接透传，避免被下面的 RuntimeException 掩盖真实原因
            throw e;
        } catch (Exception e) {
            log.error("SCAN pattern={} 失败", pattern, e);
            throw new RuntimeException("Redis SCAN操作失败", e);
        }
    }

    /**
     * 获取匹配模式的所有键。
     *
     * @deprecated KEYS 是 O(N) 命令，会阻塞 Redis 单线程，生产环境大 key 空间下可能导致服务不可用。
     * 请改用 {@link #scan(String)}。
     */
    @Deprecated
    public Set<String> keys(String pattern) {
        try (Jedis jedis = getJedis()) {
            Set<String> result = jedis.keys(pattern);
            log.warn("使用了阻塞命令 KEYS pattern={}, count={}，建议改用 scan()", pattern, result.size());
            return result;
        } catch (IllegalArgumentException e) {
            // 参数校验异常直接透传，避免被下面的 RuntimeException 掩盖真实原因
            throw e;
        } catch (Exception e) {
            log.error("KEYS pattern={} 失败", pattern, e);
            throw new RuntimeException("Redis KEYS操作失败", e);
        }
    }

    /**
     * 获取匹配模式的所有 String 类型键值对。
     *
     * @deprecated 存在两个问题：1) 内部使用阻塞命令 KEYS；2) 对匹配到的非 String 类型键
     * （hash/list/set/zset）会触发 WRONGTYPE 错误；3) 遍历 GET 无法保证快照一致。
     * 请改用 {@link #scan(String)} + {@link #mget(String...)} 自行组合。
     */
    @Deprecated
    public Map<String, String> getKeysWithValues(String pattern) {
        try (Jedis jedis = getJedis()) {
            Set<String> matched = jedis.keys(pattern);
            Map<String, String> result = new HashMap<String, String>();
            for (String key : matched) {
                if (!"string".equals(jedis.type(key))) {
                    log.warn("键 {} 不是 String 类型，已跳过（原实现会直接抛 WRONGTYPE）", key);
                    continue;
                }
                result.put(key, unwrapJsonString(jedis.get(key)));
            }
            log.debug("GETKEYSWITHVALUES pattern={}, count={}", pattern, result.size());
            return result;
        } catch (IllegalArgumentException e) {
            // 参数校验异常直接透传，避免被下面的 RuntimeException 掩盖真实原因
            throw e;
        } catch (Exception e) {
            log.error("GETKEYSWITHVALUES pattern={} 失败", pattern, e);
            throw new RuntimeException("Redis 获取键值对失败", e);
        }
    }

    /**
     * 关闭连接池。可重复调用（幂等）。
     */
    public void close() {
        shutdown();
    }

    /**
     * 连接池是否已关闭。
     *
     * @return true 表示连接池已关闭或尚未创建（懒加载），此时任何操作都会失败
     */
    public boolean isClosed() {
        JedisPool pool = this.jedisPool;
        return pool == null || pool.isClosed();
}

    /**
     * 关闭连接池并释放所有连接（幂等，可重复调用）。
     *
     * <p>方法名刻意不叫 close：Spring 对 @Bean 方法会推断名为 close 的销毁方法，
     * 若同时配置了显式销毁方法会导致连接池被关闭两次。此处统一由
     * {@link #close()} 与 Spring 的 {@code @PreDestroy} 调用的 {@code shutdown()} 收口。</p>
     */
    public void shutdown() {
        JedisPool pool = this.jedisPool;
        if (pool != null && !pool.isClosed()) {
            pool.close();
            log.info("Crydis RedisClient 连接池已关闭");
        }
    }

    /**
     * 关闭连接池（幂等）。
     *
     * @deprecated 请改用 {@link #close()}，命名与 Spring 的销毁方法约定保持一致。
     */
    @Deprecated
    public void destroy() {
        close();
    }

    /**
     * 将对象序列化为符合 Redis 存储的 JSON 字符串
     * 使用 WriteClassName 附带类型信息，以便反序列化还原
     */
    private <T> String serialize(T value) {
        if (value == null) {
            throw new IllegalArgumentException("setObject 的 value 不能为 null；如需删除缓存请调用 delete(key)");
        }
        if (value instanceof String) {
            return (String) value;
        }
        return JSON.toJSONString(value, JSONWriter.Feature.WriteClassName);
    }

    /**
     * 安全的反序列化实现。
     *
     * <p>与旧版的关键差异：</p>
     * <ul>
     *   <li>拒绝 clazz == null 与 clazz == Object.class：fastjson2 在这两种目标类型下会
     *       完全忽略过滤器，直接按 JSON 中的 @type 实例化任意类（已实测验证）。</li>
     *   <li>白名单始终生效：显式配置 crydis.allowed-packages 时以配置为准，未配置时自动收窄为
     *       “只放行目标类型 clazz 自身”（fastjson2 的前缀匹配同时覆盖其嵌套类型），
     *       而不是旧版的全局盲放。</li>
     *   <li>@type 不在白名单内时抛出异常，不再静默降级成 JSONObject，
     *       避免调用方在使用返回值时才收到 ClassCastException。</li>
     * </ul>
     */
    private <T> T deserialize(String json, Class<T> clazz, Filter autoTypeFilter) {
        if (clazz == null) {
            throw new IllegalArgumentException("反序列化目标类型 clazz 不能为 null");
        }
        if (clazz == Object.class) {
            throw new IllegalArgumentException(
                    "getObject 的目标类型不能是 Object.class：fastjson2 在 Object.class 下会忽略白名单，"
                            + "按 JSON 中的 @type 实例化任意类，存在反序列化 RCE 风险。请传入明确的实体类型");
        }
        if (json == null || json.isEmpty()) {
            return null;
        }
        if (clazz == String.class) {
            return clazz.cast(json);
        }
        // 必须使用 JSON.parseObject(json, clazz, filter, SupportAutoType)：
        // 已实测验证，只有该路径会在 @type 与目标类型不匹配（即命中白名单外的类型）时抛出 JSONException；
        // 换成 JSONReader.read(clazz) 会静默返回一个字段全为默认值的空对象，等于掩盖了数据异常。
        Object result = JSON.parseObject(json, clazz, autoTypeFilter, JSONReader.Feature.SupportAutoType);
        if (result == null) {
            return null;
        }
        if (clazz.isInstance(result)) {
            return clazz.cast(result);
        }
        // 双保险：任何未被转换为目标类型的结果都在这里显式失败
        // 双保险：任何未被转换为目标类型的结果都在这里显式失败，
        // 避免调用方在使用返回值时才收到 ClassCastException
        throw new IllegalStateException("Redis 中存储的数据与目标类型不匹配：" + result.getClass().getName()
                + " 无法转换为 " + clazz.getName() + "，请检查 crydis.allowed-packages 白名单配置");
    }

    /**
     * 构建 autoType 白名单过滤器。
     *
     * <p>白名单来源取并集：配置项 {@code crydis.allowed-packages} ∪ 本次传入的前缀 ∪
     * 目标类型 {@code clazz} 自身（兜底，保证正常数据可还原）。</p>
     *
     * <p>注意 fastjson2 白名单是<b>文本前缀</b>匹配且不支持 {@code *} 通配符：
     * {@code "cn.foo."} 放行该包下所有类，{@code "cn.foo.*"} 则一个都放行不了。</p>
     */
    private Filter buildAutoTypeFilter(Class<?> clazz, String... extraPrefixes) {
        List<String> accepts = new ArrayList<String>(4);
        List<String> allowedPackages = configuration.getAllowedPackages();
        if (allowedPackages != null) {
            for (String pkg : allowedPackages) {
                if (pkg != null && !pkg.trim().isEmpty()) {
                    accepts.add(pkg.trim());
                }
            }
        }
        if (extraPrefixes != null) {
            for (String prefix : extraPrefixes) {
                if (prefix != null && !prefix.trim().isEmpty()) {
                    accepts.add(prefix.trim());
                }
            }
        }
        // 无论是否配置白名单，目标类型自身始终放行，否则正常数据都无法还原
        accepts.add(clazz.getName());
        return JSONReader.autoTypeFilter(accepts.toArray(new String[0]));
    }

    /**
     * 还原最外层带双引号的纯 JSON 字符串。
     *
     * <p>该行为会破坏以引号开头结尾的<b>合法</b>值（写入 {@code "abc"} 读出来变成 {@code abc}），
     * 因此默认关闭。仅当 {@code crydis.unwrap-quoted-string=true} 时才启用，
     * 用于兼容历史版本写入的数据。</p>
     */
    private String unwrapJsonString(String value) {
        if (!configuration.isUnwrapQuotedString()) {
            return value;
        }
        if (value != null && value.startsWith("\"") && value.endsWith("\"") && value.length() >= 2) {
            return value.substring(1, value.length() - 1);
        }
        return value;
    }

    /**
     * 对查询结果中的字符串集合做统一的“去引号”处理。
     * 默认配置下是空操作；启用时按索引原地替换，避免额外拷贝。
     */
    @SuppressWarnings("unchecked")
    private <C extends Collection<String>> C unwrapAll(C values) {
        if (!configuration.isUnwrapQuotedString() || values == null || values.isEmpty()) {
            return values;
        }
        if (values instanceof List) {
            List<String> list = (List<String>) values;
            for (int i = 0, n = list.size(); i < n; i++) {
                list.set(i, unwrapJsonString(list.get(i)));
            }
            return values;
        }
        Collection<String> converted = values instanceof Set
                ? new LinkedHashSet<String>()
                : new ArrayList<String>();
        for (String value : values) {
            converted.add(unwrapJsonString(value));
        }
        return (C) converted;
    }

    /**
     * 将过期时间换算为秒，并做客户端校验。
     *
     * <p>旧实现直接 timeUnit.toSeconds(expireTime) 后强转 int：小于 1 秒的值会被截断成 0、
     * 超过 int 范围的值会溢出为负数，最终由 Redis 抛出难以定位的错误。</p>
     */
    private long toSeconds(long expireTime, TimeUnit timeUnit) {
        if (timeUnit == null) {
            throw new IllegalArgumentException("timeUnit 不能为 null");
        }
        if (expireTime <= 0) {
            throw new IllegalArgumentException("过期时间必须大于 0，当前值：" + expireTime + " " + timeUnit);
        }
        long seconds = timeUnit.toSeconds(expireTime);
        if (seconds <= 0) {
            throw new IllegalArgumentException("过期时间不足 1 秒（当前 " + expireTime + " " + timeUnit
                    + "），Redis 过期精度为秒，请改用不小于 1 秒的值");
        }
        return seconds;
    }
}
