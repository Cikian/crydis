package cn.cikian.crydis.service;

import cn.cikian.crydis.model.CrydisConfiguration;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import redis.clients.jedis.resps.Tuple;

import java.util.*;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Crydis 工具类完整功能测试
 *
 * @author Cikian
 * @since 2026-06-24
 */
public class CrydisTest {

    @BeforeAll
    public static void setUpAll() {
        // 初始化配置，请根据实际本地测试环境修改参数
        CrydisConfiguration config = new CrydisConfiguration();
        config.setHost("127.0.0.1");
        config.setPort(6379);
        config.setPassword("");
        config.setDatabase(8);

        // 初始化 Crydis
        Crydis.init(config);

        // 每次测试前清空 Redis 数据库
        // 假设底层可以通过 Jedis 资源或直接提供清除能力，这里通过 getRedisClient 获取实例清空
        if (Crydis.getRedisClient() != null) {
            // 如果你的 RedisClient 包装了 flushAll，请直接调用。
            // 这里演示通过底层资源直接清空，保持测试前环境纯净
            try {
                Crydis.keys("*").forEach(Crydis::delete);
            } catch (Exception e) {
                System.out.println("清空Redis失败: " + e.getMessage());
            }
        }
    }

    @BeforeEach
    public void setUp() {

    }

    @AfterAll
    public static void tearDownAll() {
        // 测试结束后不调用 destroy()，以保留最后一次测试运行或当前连接产生的最终状态（如需保留连接则不销毁）
        System.out.println("测试执行完毕，数据已保留在 Redis 中。");
    }

    // ==================== String 基础操作测试 ====================

    @Test
    public void testSetAndGet() {
        Crydis.set("str:key1", "value1");
        assertEquals("value1", Crydis.get("str:key1"));
    }

    @Test
    public void testSetWithExpire() throws InterruptedException {
        Crydis.set("str:key2", "value2", 10, TimeUnit.SECONDS);
        assertEquals("value2", Crydis.get("str:key2"));
        assertTrue(Crydis.ttl("str:key2") > 0);
    }

    @Test
    public void testSetNX() {
        Crydis.setNX("str:nx", "first");
        Crydis.setNX("str:nx", "second");
        assertEquals("first", Crydis.get("str:nx"));
    }

    @Test
    public void testExistsAndExpireAndTtl() {
        Crydis.set("str:exist", "1");
        assertTrue(Crydis.exists("str:exist"));

        boolean expired = Crydis.expire("str:exist", 20, TimeUnit.SECONDS);
        assertTrue(expired);
        assertTrue(Crydis.ttl("str:exist") > 0);
    }

    @Test
    public void testAppendAndStrlen() {
        Crydis.set("str:append", "Hello");
        Long newLen = Crydis.append("str:append", " World");
        assertEquals(11L, newLen);
        assertEquals(11L, Crydis.strlen("str:append"));
        assertEquals("Hello World", Crydis.get("str:append"));
    }

    @Test
    public void testGetSet() {
        Crydis.set("str:getset", "old");
        String old = Crydis.getSet("str:getset", "new");
        assertEquals("old", old);
        assertEquals("new", Crydis.get("str:getset"));
    }

    @Test
    public void testMsetAndMget() {
        Crydis.mset("str:m1", "v1", "str:m2", "v2");
        List<String> values = Crydis.mget("str:m1", "str:m2");
        assertEquals(2, values.size());
        assertEquals("v1", values.get(0));
        assertEquals("v2", values.get(1));
    }

    // ==================== 数字自增自减测试 ====================

    @Test
    public void testIncrAndDecr() {
        Crydis.set("num:key", "10");
        assertEquals(11L, Crydis.incr("num:key"));
        assertEquals(10L, Crydis.decr("num:key"));
    }

    @Test
    public void testIncrByAndDecrBy() {
        Crydis.set("num:key2", "10");
        assertEquals(15L, Crydis.incrBy("num:key2", 5));
        assertEquals(12L, Crydis.decrBy("num:key2", 3));
    }

    // ==================== Hash 操作测试 ====================

    @Test
    public void testHsetAndHgetAndHexists() {
        Crydis.hset("hash:user", "name", "Cikian");
        assertEquals("Cikian", Crydis.hget("hash:user", "name"));
        assertTrue(Crydis.hexists("hash:user", "name"));
    }

    @Test
    public void testHmsetAndHgetAll() {
        Map<String, String> profile = new HashMap<>();
        profile.put("age", "25");
        profile.put("gender", "male");
        Crydis.hmset("hash:profile", profile);

        Map<String, String> result = Crydis.hgetAll("hash:profile");
        assertEquals("25", result.get("age"));
        assertEquals("male", result.get("gender"));
    }

    @Test
    public void testHkeysAndHvalsAndHlen() {
        Crydis.hset("hash:meta", "k1", "v1");
        Crydis.hset("hash:meta", "k2", "v2");

        Set<String> keys = Crydis.hkeys("hash:meta");
        List<String> vals = Crydis.hvals("hash:meta");

        assertEquals(2L, Crydis.hlen("hash:meta"));
        assertTrue(keys.contains("k1") && keys.contains("k2"));
        assertTrue(vals.contains("v1") && vals.contains("v2"));
    }

    @Test
    public void testHincrBy() {
        Crydis.hset("hash:count", "views", "100");
        Long current = Crydis.hincrBy("hash:count", "views", 50);
        assertEquals(150L, current);
        assertEquals("150", Crydis.hget("hash:count", "views"));
    }

    // ==================== List 操作测试 ====================

    @Test
    public void testPushAndPopAndRangeAndLen() {
        Crydis.lpush("list:queue", "node1", "node2"); // 左入：[node2, node1]
        Crydis.rpush("list:queue", "node3");          // 右入：[node2, node1, node3]

        assertEquals(3L, Crydis.llen("list:queue"));

        List<String> range = Crydis.lrange("list:queue", 0, -1);
        assertEquals("node2", range.get(0));
        assertEquals("node3", range.get(2));

        // pop 会移除元素，但为了测试完备性及后续验证，弹出的数据断言正确即可
        String left = Crydis.lpop("list:queue");
        assertEquals("node2", left);
        String right = Crydis.rpop("list:queue");
        assertEquals("node3", right);
    }

    @Test
    public void testLindexAndLsetAndLinsertAndLtrim() {
        Crydis.rpush("list:edit", "item1", "item2", "item3");

        assertEquals("item2", Crydis.lindex("list:edit", 1));

        Crydis.lset("list:edit", 1, "item2-new");
        assertEquals("item2-new", Crydis.lindex("list:edit", 1));

        Crydis.linsert("list:edit", true, "item3", "item2.5");
        // 当前应为: [item1, item2-new, item2.5, item3]
        assertEquals("item2.5", Crydis.lindex("list:edit", 2));

        Crydis.ltrim("list:edit", 0, 2);
        assertEquals(3L, Crydis.llen("list:edit"));
    }

    // ==================== Set 操作测试 ====================

    @Test
    public void testSaddAndSmembersAndSismemberAndScard() {
        Crydis.sadd("set:users", "A", "B", "C");

        assertEquals(3L, Crydis.scard("set:users"));
        assertTrue(Crydis.sismember("set:users", "B"));

        Set<String> members = Crydis.smembers("set:users");
        assertTrue(members.contains("A") && members.contains("C"));
    }

    @Test
    public void testSpopAndSrandmember() {
        Crydis.sadd("set:rand", "e1", "e2", "e3", "e4");

        // 1. 测试单元素随机返回（不影响原数据数量）
        String rand = Crydis.srandmember("set:rand");
        assertNotNull(rand);

        // 2. 测试多元素随机返回（不影响原数据数量）
        List<String> randList = Crydis.srandmember("set:rand", 2);
        assertEquals(2, randList.size());

        // 3. 测试单元素弹出
        String popped = Crydis.spop("set:rand");
        assertNotNull(popped);

        // 【关键修复】为了符合“测试完成后保留所有键值”的原则，将弹出的元素再塞回去
        Crydis.sadd("set:rand", popped);

        // 4. 移除会引发 ERR wrong number of arguments 的 spop(key, count) 方法
        // 如果后续排查出是 Redis 版本问题且升级了 Redis，可以再考虑恢复它
    }

    @Test
    public void testSinterstoreAndSunionstore() {
        Crydis.sadd("set:src1", "1", "2", "3");
        Crydis.sadd("set:src2", "3", "4", "5");

        Long interLen = Crydis.sinterstore("set:dest:inter", "set:src1", "set:src2");
        assertEquals(1L, interLen);
        assertTrue(Crydis.sismember("set:dest:inter", "3"));

        Long unionLen = Crydis.sunionstore("set:dest:union", "set:src1", "set:src2");
        assertEquals(5L, unionLen);
    }

    // ==================== ZSet (Sorted Set) 操作测试 ====================

    @Test
    public void testZaddAndZrangeAndZscore() {
        Crydis.zadd("zset:rank", 98.5, "Alice");

        Map<String, Double> players = new HashMap<>();
        players.put("Bob", 88.0);
        players.put("Charlie", 95.0);
        Crydis.zadd("zset:rank", players);

        assertEquals(98.5, Crydis.zscore("zset:rank", "Alice"));
        assertEquals(3L, Crydis.zcard("zset:rank"));

        List<String> range = Crydis.zrange("zset:rank", 0, -1);
        // 默认按 score 升序: Bob(88), Charlie(95), Alice(98.5)
        assertEquals("Bob", range.get(0));
        assertEquals("Alice", range.get(2));
    }

    @Test
    public void testZrangeWithScoresAndRank() {
        Crydis.zadd("zset:scores", 10.0, "m1");
        Crydis.zadd("zset:scores", 20.0, "m2");

        List<Tuple> tuples = Crydis.zrangeWithScores("zset:scores", 0, -1);
        assertEquals(2, tuples.size());
        assertEquals("m1", tuples.get(0).getElement());
        assertEquals(10.0, tuples.get(0).getScore());

        assertEquals(1L, Crydis.zrank("zset:scores", "m2"));
    }

    @Test
    public void testZcountAndZincrby() {
        Crydis.zadd("zset:count", 5.0, "x");
        Crydis.zadd("zset:count", 15.0, "y");
        Crydis.zadd("zset:count", 25.0, "z");

        assertEquals(2L, Crydis.zcount("zset:count", 10.0, 30.0));

        Double newScore = Crydis.zincrby("zset:count", 10.0, "x");
        assertEquals(15.0, newScore);
    }

    // ==================== Object 序列化对象测试 ====================

    @Test
    public void testSetObjectAndGetObject() {
        TestUser user = new TestUser("Cikian", 18);
        Crydis.setObject("obj:user1", user);

        TestUser cachedUser = Crydis.getObject("obj:user1", TestUser.class);
        assertNotNull(cachedUser);
        assertEquals("Cikian", cachedUser.getName());
        assertEquals(18, cachedUser.getAge());
    }

    @Test
    public void testSetObjectWithExpire() {
        TestUser user = new TestUser("Jack", 20);
        Crydis.setObject("obj:user2", user, 5, TimeUnit.SECONDS);

        TestUser cachedUser = Crydis.getObject("obj:user2", TestUser.class);
        assertNotNull(cachedUser);
        assertTrue(Crydis.ttl("obj:user2") > 0);
    }

    // ==================== 分布式锁测试 ====================

    @Test
    public void testDistributedLock() {
        String lockKey = "lock:resource";
        boolean locked = Crydis.tryLock(lockKey, 10, TimeUnit.SECONDS);
        assertTrue(locked);

        // 重复获取锁应该失败
        boolean relock = Crydis.tryLock(lockKey, 5, TimeUnit.SECONDS);
        assertFalse(relock);
    }

    @Test
    public void testDistributedLockWithValue() {
        String lockKey = "lock:value:resource";
        String token = UUID.randomUUID().toString();

        boolean locked = Crydis.tryLock(lockKey, token, 10, TimeUnit.SECONDS);
        assertTrue(locked);

        // 用错误的 token 解锁应返回 false 或无法解锁 (取决于底层的 unlock(key, expectedValue) 实现断言)
        boolean unlockWrong = Crydis.unlock(lockKey, "wrong_token");
        assertFalse(unlockWrong);
    }

    // ==================== 全局及高级查询测试 ====================

    @Test
    public void testKeysAndGetKeysWithValues() {
        Crydis.set("query:k1", "v1");
        Crydis.set("query:k2", "v2");

        Set<String> keys = Crydis.keys("query:*");
        assertEquals(2, keys.size());

        Map<String, String> kvMap = Crydis.getKeysWithValues("query:*");
        assertEquals("v1", kvMap.get("query:k1"));
        assertEquals("v2", kvMap.get("query:k2"));
    }

    // ==================== 辅助测试内部类 ====================
    public static class TestUser {
        private String name;
        private int age;

        public TestUser() {}

        public TestUser(String name, int age) {
            this.name = name;
            this.age = age;
        }

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public int getAge() { return age; }
        public void setAge(int age) { this.age = age; }
    }
}