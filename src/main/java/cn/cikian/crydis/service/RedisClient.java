package cn.cikian.crydis.service;

import cn.cikian.crydis.model.CrydisConfiguration;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.DefaultJedisClientConfig;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisClientConfig;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

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
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private JedisPool jedisPool;
    private final CrydisConfiguration configuration;

    public RedisClient(CrydisConfiguration configuration) {
        this.configuration = configuration;
        initJedisPool();
    }

    private void initJedisPool() {
        try {
            JedisPoolConfig poolConfig = new JedisPoolConfig();
            poolConfig.setMaxTotal(configuration.getMaxActive() != null ? configuration.getMaxActive() : 50);
            poolConfig.setMaxIdle(configuration.getMaxIdle() != null ? configuration.getMaxIdle() : 10);
            poolConfig.setMinIdle(configuration.getMinIdle() != null ? configuration.getMinIdle() : 5);
            poolConfig.setMaxWaitMillis(configuration.getMaxWait() != null ? configuration.getMaxWait() : 3000);

            String host = configuration.getHost();
            int port = configuration.getPort() != null ? configuration.getPort() : 6379;
            String password = configuration.getPassword();
            int database = configuration.getDatabase() != null ? configuration.getDatabase() : 0;
            int timeout = configuration.getTimeout() != null ? configuration.getTimeout() : 3000;

            // 使用 Jedis 7.x 推荐的 ClientConfig 构建器替代废弃的多参数硬编码构造函数
            DefaultJedisClientConfig.Builder configBuilder = DefaultJedisClientConfig.builder()
                    .connectionTimeoutMillis(timeout)
                    .socketTimeoutMillis(timeout)
                    .database(database);

            if (password != null && !password.isEmpty()) {
                configBuilder.password(password);
            }

            JedisClientConfig clientConfig = configBuilder.build();
            jedisPool = new JedisPool(poolConfig, new HostAndPort(host, port), clientConfig);

            log.info("Crydis RedisClient 初始化成功 - {}:{}", host, port);
        } catch (Exception e) {
            log.error("Crydis RedisClient 初始化失败", e);
            throw new RuntimeException("Redis连接池初始化失败", e);
        }
    }

    private Jedis getJedis() {
        return jedisPool.getResource();
    }

    public void set(String key, String value) {
        try (Jedis jedis = getJedis()) {
            jedis.set(key, value);
            log.debug("SET key={}", key);
        } catch (Exception e) {
            log.error("SET key={} 失败", key, e);
            throw new RuntimeException("Redis SET操作失败", e);
        }
    }

    public void set(String key, String value, long expireTime, TimeUnit timeUnit) {
        try (Jedis jedis = getJedis()) {
            jedis.setex(key, timeUnit.toSeconds(expireTime), value);
            log.debug("SET key={} expire={}{}", key, expireTime, timeUnit);
        } catch (Exception e) {
            log.error("SET key={} 失败", key, e);
            throw new RuntimeException("Redis SET操作失败", e);
        }
    }

    public void setNX(String key, String value) {
        try (Jedis jedis = getJedis()) {
            jedis.setnx(key, value);
            log.debug("SETNX key={}", key);
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
        } catch (Exception e) {
            log.error("GET key={} 失败", key, e);
            throw new RuntimeException("Redis GET操作失败", e);
        }
    }

    private String unwrapJsonString(String value) {
        if (value == null) {
            return null;
        }
        if (value.length() >= 2 && value.startsWith("\"") && value.endsWith("\"")) {
            try {
                return objectMapper.readValue(value, String.class);
            } catch (JsonProcessingException e) {
                return value;
            }
        }
        return value;
    }

    public void delete(String key) {
        try (Jedis jedis = getJedis()) {
            jedis.del(key);
            log.debug("DEL key={}", key);
        } catch (Exception e) {
            log.error("DEL key={} 失败", key, e);
            throw new RuntimeException("Redis DELETE操作失败", e);
        }
    }

    public void delete(String... keys) {
        try (Jedis jedis = getJedis()) {
            jedis.del(keys);
            log.debug("DEL keys={}", Arrays.toString(keys));
        } catch (Exception e) {
            log.error("DEL keys={} 失败", Arrays.toString(keys), e);
            throw new RuntimeException("Redis DELETE操作失败", e);
        }
    }

    public boolean exists(String key) {
        try (Jedis jedis = getJedis()) {
            return jedis.exists(key);
        } catch (Exception e) {
            log.error("EXISTS key={} 失败", key, e);
            throw new RuntimeException("Redis EXISTS操作失败", e);
        }
    }

    public boolean expire(String key, long expireTime, TimeUnit timeUnit) {
        try (Jedis jedis = getJedis()) {
            return jedis.expire(key, timeUnit.toSeconds(expireTime)) > 0;
        } catch (Exception e) {
            log.error("EXPIRE key={} 失败", key, e);
            throw new RuntimeException("Redis EXPIRE操作失败", e);
        }
    }

    public long ttl(String key) {
        try (Jedis jedis = getJedis()) {
            return jedis.ttl(key);
        } catch (Exception e) {
            log.error("TTL key={} 失败", key, e);
            throw new RuntimeException("Redis TTL操作失败", e);
        }
    }

    public void hset(String key, String field, String value) {
        try (Jedis jedis = getJedis()) {
            jedis.hset(key, field, value);
            log.debug("HSET key={} field={}", key, field);
        } catch (Exception e) {
            log.error("HSET key={} field={} 失败", key, field, e);
            throw new RuntimeException("Redis HSET操作失败", e);
        }
    }

    public void hmset(String key, Map<String, String> hash) {
        try (Jedis jedis = getJedis()) {
            // 在 Redis 官方及 Jedis 5.x/7.x 中，hmset 命令已被废弃，统一由支持 Map 的 hset 方法代理
            jedis.hset(key, hash);
            log.debug("HMSET (via hset) key={}", key);
        } catch (Exception e) {
            log.error("HMSET key={} 失败", key, e);
            throw new RuntimeException("Redis HMSET操作失败", e);
        }
    }

    public String hget(String key, String field) {
        try (Jedis jedis = getJedis()) {
            return unwrapJsonString(jedis.hget(key, field));
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
        } catch (Exception e) {
            log.error("HGETALL key={} 失败", key, e);
            throw new RuntimeException("Redis HGETALL操作失败", e);
        }
    }

    public void hdel(String key, String... fields) {
        try (Jedis jedis = getJedis()) {
            jedis.hdel(key, fields);
            log.debug("HDEL key={} fields={}", key, Arrays.toString(fields));
        } catch (Exception e) {
            log.error("HDEL key={} 失败", key, e);
            throw new RuntimeException("Redis HDEL操作失败", e);
        }
    }

    public boolean hexists(String key, String field) {
        try (Jedis jedis = getJedis()) {
            return jedis.hexists(key, field);
        } catch (Exception e) {
            log.error("HEXISTS key={} field={} 失败", key, field, e);
            throw new RuntimeException("Redis HEXISTS操作失败", e);
        }
    }

    public Long incr(String key) {
        try (Jedis jedis = getJedis()) {
            return jedis.incr(key);
        } catch (Exception e) {
            log.error("INCR key={} 失败", key, e);
            throw new RuntimeException("Redis INCR操作失败", e);
        }
    }

    public Long decr(String key) {
        try (Jedis jedis = getJedis()) {
            return jedis.decr(key);
        } catch (Exception e) {
            log.error("DECR key={} 失败", key, e);
            throw new RuntimeException("Redis DECR操作失败", e);
        }
    }

    public Long incrBy(String key, long increment) {
        try (Jedis jedis = getJedis()) {
            return jedis.incrBy(key, increment);
        } catch (Exception e) {
            log.error("INCRBY key={} 失败", key, e);
            throw new RuntimeException("Redis INCRBY操作失败", e);
        }
    }

    public Long decrBy(String key, long decrement) {
        try (Jedis jedis = getJedis()) {
            return jedis.decrBy(key, decrement);
        } catch (Exception e) {
            log.error("DECRBY key={} 失败", key, e);
            throw new RuntimeException("Redis DECRBY操作失败", e);
        }
    }

    public void lpush(String key, String... values) {
        try (Jedis jedis = getJedis()) {
            jedis.lpush(key, values);
            log.debug("LPUSH key={} values={}", key, Arrays.toString(values));
        } catch (Exception e) {
            log.error("LPUSH key={} 失败", key, e);
            throw new RuntimeException("Redis LPUSH操作失败", e);
        }
    }

    public void rpush(String key, String... values) {
        try (Jedis jedis = getJedis()) {
            jedis.rpush(key, values);
            log.debug("RPUSH key={} values={}", key, Arrays.toString(values));
        } catch (Exception e) {
            log.error("RPUSH key={} 失败", key, e);
            throw new RuntimeException("Redis RPUSH操作失败", e);
        }
    }

    public String lpop(String key) {
        try (Jedis jedis = getJedis()) {
            return unwrapJsonString(jedis.lpop(key));
        } catch (Exception e) {
            log.error("LPOP key={} 失败", key, e);
            throw new RuntimeException("Redis LPOP操作失败", e);
        }
    }

    public String rpop(String key) {
        try (Jedis jedis = getJedis()) {
            return unwrapJsonString(jedis.rpop(key));
        } catch (Exception e) {
            log.error("RPOP key={} 失败", key, e);
            throw new RuntimeException("Redis RPOP操作失败", e);
        }
    }

    public List<String> lrange(String key, long start, long end) {
        try (Jedis jedis = getJedis()) {
            List<String> result = jedis.lrange(key, start, end);
            result.replaceAll(this::unwrapJsonString);
            return result;
        } catch (Exception e) {
            log.error("LRANGE key={} 失败", key, e);
            throw new RuntimeException("Redis LRANGE操作失败", e);
        }
    }

    public Long llen(String key) {
        try (Jedis jedis = getJedis()) {
            return jedis.llen(key);
        } catch (Exception e) {
            log.error("LLEN key={} 失败", key, e);
            throw new RuntimeException("Redis LLEN操作失败", e);
        }
    }

    public void sadd(String key, String... members) {
        try (Jedis jedis = getJedis()) {
            jedis.sadd(key, members);
            log.debug("SADD key={} members={}", key, Arrays.toString(members));
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
        } catch (Exception e) {
            log.error("SMEMBERS key={} 失败", key, e);
            throw new RuntimeException("Redis SMEMBERS操作失败", e);
        }
    }

    public boolean sismember(String key, String member) {
        try (Jedis jedis = getJedis()) {
            return jedis.sismember(key, member);
        } catch (Exception e) {
            log.error("SISMEMBER key={} 失败", key, e);
            throw new RuntimeException("Redis SISMEMBER操作失败", e);
        }
    }

    public void srem(String key, String... members) {
        try (Jedis jedis = getJedis()) {
            jedis.srem(key, members);
            log.debug("SREM key={} members={}", key, Arrays.toString(members));
        } catch (Exception e) {
            log.error("SREM key={} 失败", key, e);
            throw new RuntimeException("Redis SREM操作失败", e);
        }
    }

    public <T> void setObject(String key, T object) {
        try {
            if (object instanceof String) {
                set(key, (String) object);
            } else {
                String json = objectMapper.writeValueAsString(object);
                set(key, json);
            }
        } catch (JsonProcessingException e) {
            log.error("序列化对象失败 key={}", key, e);
            throw new RuntimeException("对象序列化失败", e);
        }
    }

    public <T> void setObject(String key, T object, long expireTime, TimeUnit timeUnit) {
        try {
            if (object instanceof String) {
                set(key, (String) object, expireTime, timeUnit);
            } else {
                String json = objectMapper.writeValueAsString(object);
                set(key, json, expireTime, timeUnit);
            }
        } catch (JsonProcessingException e) {
            log.error("序列化对象失败 key={}", key, e);
            throw new RuntimeException("对象序列化失败", e);
        }
    }

    public <T> T getObject(String key, Class<T> clazz) {
        try {
            String json = get(key);
            if (json == null) {
                return null;
            }
            if (clazz == String.class) {
                return clazz.cast(json);
            }
            return objectMapper.readValue(json, clazz);
        } catch (JsonProcessingException e) {
            log.error("反序列化对象失败 key={}", key, e);
            throw new RuntimeException("对象反序列化失败", e);
        }
    }

    public void destroy() {
        if (jedisPool != null && !jedisPool.isClosed()) {
            jedisPool.close();
            log.info("Crydis RedisClient 连接池已关闭");
        }
    }

    public JedisPool getJedisPool() {
        return jedisPool;
    }
}