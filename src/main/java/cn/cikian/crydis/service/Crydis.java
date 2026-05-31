package cn.cikian.crydis.service;

import cn.cikian.crydis.model.CrydisConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * Crydis Redis工具类
 * 提供静态方法调用，支持Spring和非Spring项目
 *
 * @author Cikian
 * @version 1.0
 * @since 2026-05-31
 */
public class Crydis {
    private static final Logger log = LoggerFactory.getLogger(Crydis.class);
    private static RedisClient redisClient;
    private static volatile boolean initialized = false;

    public Crydis(CrydisConfiguration configuration) {
        redisClient = new RedisClient(configuration);
    }

    public Crydis(RedisClient redisClient) {
        Crydis.redisClient = redisClient;
    }

    public static void init(CrydisConfiguration configuration) {
        if (!initialized) {
            synchronized (Crydis.class) {
                if (!initialized) {
                    redisClient = new RedisClient(configuration);
                    initialized = true;
                    log.info("Crydis 初始化成功");
                }
            }
        }
    }

    public static void init(RedisClient redisClient) {
        if (!initialized) {
            synchronized (Crydis.class) {
                if (!initialized) {
                    Crydis.redisClient = redisClient;
                    initialized = true;
                    log.info("Crydis 初始化成功（使用外部RedisClient）");
                }
            }
        }
    }

    public static void destroy() {
        if (redisClient != null) {
            redisClient.destroy();
            redisClient = null;
            initialized = false;
            log.info("Crydis 已销毁");
        }
    }

    private static void checkInit() {
        if (redisClient == null) {
            throw new IllegalStateException("Crydis 未初始化，请先调用 Crydis.init() 进行初始化");
        }
    }

    public static void set(String key, String value) {
        checkInit();
        redisClient.set(key, value);
    }

    public static void set(String key, String value, long expireTime, TimeUnit timeUnit) {
        checkInit();
        redisClient.set(key, value, expireTime, timeUnit);
    }

    public static void setNX(String key, String value) {
        checkInit();
        redisClient.setNX(key, value);
    }

    public static String get(String key) {
        checkInit();
        return redisClient.get(key);
    }

    public static void delete(String key) {
        checkInit();
        redisClient.delete(key);
    }

    public static void delete(String... keys) {
        checkInit();
        redisClient.delete(keys);
    }

    public static boolean exists(String key) {
        checkInit();
        return redisClient.exists(key);
    }

    public static boolean expire(String key, long expireTime, TimeUnit timeUnit) {
        checkInit();
        return redisClient.expire(key, expireTime, timeUnit);
    }

    public static long ttl(String key) {
        checkInit();
        return redisClient.ttl(key);
    }

    public static void hset(String key, String field, String value) {
        checkInit();
        redisClient.hset(key, field, value);
    }

    public static void hmset(String key, Map<String, String> hash) {
        checkInit();
        redisClient.hmset(key, hash);
    }

    public static String hget(String key, String field) {
        checkInit();
        return redisClient.hget(key, field);
    }

    public static Map<String, String> hgetAll(String key) {
        checkInit();
        return redisClient.hgetAll(key);
    }

    public static void hdel(String key, String... fields) {
        checkInit();
        redisClient.hdel(key, fields);
    }

    public static boolean hexists(String key, String field) {
        checkInit();
        return redisClient.hexists(key, field);
    }

    public static Long incr(String key) {
        checkInit();
        return redisClient.incr(key);
    }

    public static Long decr(String key) {
        checkInit();
        return redisClient.decr(key);
    }

    public static Long incrBy(String key, long increment) {
        checkInit();
        return redisClient.incrBy(key, increment);
    }

    public static Long decrBy(String key, long decrement) {
        checkInit();
        return redisClient.decrBy(key, decrement);
    }

    public static void lpush(String key, String... values) {
        checkInit();
        redisClient.lpush(key, values);
    }

    public static void rpush(String key, String... values) {
        checkInit();
        redisClient.rpush(key, values);
    }

    public static String lpop(String key) {
        checkInit();
        return redisClient.lpop(key);
    }

    public static String rpop(String key) {
        checkInit();
        return redisClient.rpop(key);
    }

    public static List<String> lrange(String key, long start, long end) {
        checkInit();
        return redisClient.lrange(key, start, end);
    }

    public static Long llen(String key) {
        checkInit();
        return redisClient.llen(key);
    }

    public static void sadd(String key, String... members) {
        checkInit();
        redisClient.sadd(key, members);
    }

    public static Set<String> smembers(String key) {
        checkInit();
        return redisClient.smembers(key);
    }

    public static boolean sismember(String key, String member) {
        checkInit();
        return redisClient.sismember(key, member);
    }

    public static void srem(String key, String... members) {
        checkInit();
        redisClient.srem(key, members);
    }

    public static <T> void setObject(String key, T object) {
        checkInit();
        redisClient.setObject(key, object);
    }

    public static <T> void setObject(String key, T object, long expireTime, TimeUnit timeUnit) {
        checkInit();
        redisClient.setObject(key, object, expireTime, timeUnit);
    }

    public static <T> T getObject(String key, Class<T> clazz) {
        checkInit();
        return redisClient.getObject(key, clazz);
    }

    public static RedisClient getRedisClient() {
        return redisClient;
    }
}
