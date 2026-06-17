package cn.cikian.crydis.service;

import cn.cikian.crydis.model.CrydisConfiguration;
import org.junit.jupiter.api.*;
import java.util.*;
import java.util.concurrent.TimeUnit;
import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class CrydisTest {
    private static final String TEST_PREFIX = "crydis:test:";

    @BeforeAll
    public static void setup() {
        CrydisConfiguration config = new CrydisConfiguration();
        config.setHost("localhost");
        config.setPort(6379);
        config.setDatabase(0);
        Crydis.init(config);
    }

//    @AfterAll
    public static void teardown() {
        Crydis.destroy();
    }

//    @AfterEach
    public void cleanUp() {
        Set<String> keys = Crydis.getRedisClient().getJedisPool().getResource().keys(TEST_PREFIX + "*");
        if (!keys.isEmpty()) {
            Crydis.delete(keys.toArray(new String[0]));
        }
    }

    @Test @Order(1) public void testStringBasic() {
        String key = TEST_PREFIX + "string:basic";
        Crydis.set(key, "test-value");
        assertEquals("test-value", Crydis.get(key));
        assertTrue(Crydis.exists(key));
        Crydis.delete(key);
        assertFalse(Crydis.exists(key));
    }

    @Test @Order(2) public void testStringAppend() {
        String key = TEST_PREFIX + "string:append";
        Crydis.set(key, "Hello");
        Crydis.append(key, " World");
        String s = Crydis.get(key);
        assertEquals("Hello World", s);
    }

    @Test @Order(3) public void testStringStrlen() {
        String key = TEST_PREFIX + "string:strlen";
        Crydis.set(key, "Hello");
        assertEquals(5L, Crydis.strlen(key));
    }

    @Test @Order(4) public void testStringGetSet() {
        String key = TEST_PREFIX + "string:getset";
        Crydis.set(key, "old-value");
        assertEquals("old-value", Crydis.getSet(key, "new-value"));
        assertEquals("new-value", Crydis.get(key));
    }

    @Test @Order(5) public void testStringMGetMSet() {
        String k1 = TEST_PREFIX + "mget:1";
        String k2 = TEST_PREFIX + "mget:2";
        Crydis.mset(k1, "v1", k2, "v2");
        List<String> values = Crydis.mget(k1, k2);
        assertEquals("v1", values.get(0));
        assertEquals("v2", values.get(1));
    }

    @Test @Order(6) public void testStringExpireTtl() {
        String key = TEST_PREFIX + "string:expire";
        Crydis.set(key, "test");
        assertTrue(Crydis.expire(key, 10, TimeUnit.SECONDS));
        assertTrue(Crydis.ttl(key) > 0);
    }

    @Test @Order(7) public void testCounter() {
        String key = TEST_PREFIX + "counter";
        assertEquals(1L, Crydis.incr(key));
        assertEquals(2L, Crydis.incr(key));
        assertEquals(7L, Crydis.incrBy(key, 5));
        assertEquals(6L, Crydis.decr(key));
        assertEquals(4L, Crydis.decrBy(key, 2));
    }

    @Test @Order(8) public void testHashBasic() {
        String key = TEST_PREFIX + "hash:basic";
        Crydis.hset(key, "name", "Cikian");
        Crydis.hset(key, "age", "25");
        assertEquals("Cikian", Crydis.hget(key, "name"));
        assertEquals("25", Crydis.hget(key, "age"));
        assertTrue(Crydis.hexists(key, "name"));
        Crydis.hdel(key, "age");
        assertFalse(Crydis.hexists(key, "age"));
    }

    @Test @Order(9) public void testHashMSet() {
        String key = TEST_PREFIX + "hash:mset";
        Map<String, String> data = new HashMap<>();
        data.put("k1", "v1");
        data.put("k2", "v2");
        Crydis.hmset(key, data);
        assertEquals("v1", Crydis.hget(key, "k1"));
    }

    @Test @Order(10) public void testHashKeysValuesLen() {
        String key = TEST_PREFIX + "hash:keysvals";
        Crydis.hset(key, "a", "1");
        Crydis.hset(key, "b", "2");
        assertEquals(2, Crydis.hkeys(key).size());
        assertEquals(2, Crydis.hvals(key).size());
        assertEquals(2L, Crydis.hlen(key));
    }

    @Test @Order(11) public void testHashIncrBy() {
        String key = TEST_PREFIX + "hash:incrby";
        Crydis.hset(key, "count", "10");
        assertEquals(15L, Crydis.hincrBy(key, "count", 5));
    }

    @Test @Order(12) public void testListBasic() {
        String key = TEST_PREFIX + "list:basic";
        Crydis.rpush(key, "a", "b", "c");
        assertEquals(3L, Crydis.llen(key));
        assertEquals("a", Crydis.lpop(key));
        assertEquals("c", Crydis.rpop(key));
    }

    @Test @Order(13) public void testListLpush() {
        String key = TEST_PREFIX + "list:lpush";
        Crydis.lpush(key, "x", "y", "z");
        assertEquals(3L, Crydis.llen(key));
        assertEquals("z", Crydis.lpop(key));
    }

    @Test @Order(14) public void testListRangeIndex() {
        String key = TEST_PREFIX + "list:range";
        Crydis.rpush(key, "a", "b", "c", "d", "e");
        List<String> range = Crydis.lrange(key, 1, 3);
        assertEquals(3, range.size());
        assertEquals("c", Crydis.lindex(key, 2));
    }

    @Test @Order(15) public void testListSetInsertTrim() {
        String key = TEST_PREFIX + "list:setinsert";
        Crydis.rpush(key, "a", "b", "d");
        Crydis.lset(key, 1, "B");
        assertEquals("B", Crydis.lindex(key, 1));
        Crydis.linsert(key, true, "d", "c");
        assertEquals("c", Crydis.lindex(key, 2));
        Crydis.ltrim(key, 0, 2);
        assertEquals(3L, Crydis.llen(key));
    }

    @Test @Order(16) public void testSetBasic() {
        String key = TEST_PREFIX + "set:basic";
        Crydis.sadd(key, "java", "redis", "spring");
        assertEquals(3L, Crydis.scard(key));
        assertTrue(Crydis.sismember(key, "java"));
        assertFalse(Crydis.sismember(key, "python"));
        Crydis.srem(key, "spring");
        assertEquals(2L, Crydis.scard(key));
    }

    @Test @Order(17) public void testSetPopRandmember() {
        String key = TEST_PREFIX + "set:poprand";
        Crydis.sadd(key, "a", "b", "c", "d", "e");
        String popped = Crydis.spop(key);
        assertNotNull(popped);
        assertEquals(4L, Crydis.scard(key));
    }

    @Test @Order(18) public void testSetSrandmember() {
        String key = TEST_PREFIX + "set:srand";
        Crydis.sadd(key, "a", "b", "c");
        String random = Crydis.srandmember(key);
        assertNotNull(random);
        List<String> list = Crydis.srandmember(key, 2);
        assertEquals(2, list.size());
    }

    @Test @Order(19) public void testSetStore() {
        String k1 = TEST_PREFIX + "set:store:1";
        String k2 = TEST_PREFIX + "set:store:2";
        String dest = TEST_PREFIX + "set:store:dest";
        Crydis.sadd(k1, "a", "b", "c");
        Crydis.sadd(k2, "b", "c", "d");
        Crydis.sinterstore(dest, k1, k2);
        assertEquals(2L, Crydis.scard(dest));
        Crydis.sunionstore(dest, k1, k2);
        assertEquals(4L, Crydis.scard(dest));
    }

    @Test @Order(20) public void testZSetBasic() {
        String key = TEST_PREFIX + "zset:basic";
        Crydis.zadd(key, 95.5, "Alice");
        Crydis.zadd(key, 88.0, "Bob");
        assertEquals(2L, Crydis.zcard(key));
        assertEquals(95.5, Crydis.zscore(key, "Alice"), 0.001);
        List<String> top2 = Crydis.zrange(key, 0, 1);
        assertEquals(2, top2.size());
        Crydis.zrem(key, "Bob");
        assertEquals(1L, Crydis.zcard(key));
    }

    @Test @Order(21) public void testZSetRangeWithScores() {
        String key = TEST_PREFIX + "zset:scores";
        Crydis.zadd(key, 100.0, "A");
        Crydis.zadd(key, 90.0, "B");
        List<redis.clients.jedis.resps.Tuple> tuples = Crydis.zrangeWithScores(key, 0, -1);
        assertEquals(2, tuples.size());
    }

    @Test @Order(22) public void testZSetCountIncrby() {
        String key = TEST_PREFIX + "zset:countincr";
        Crydis.zadd(key, 85.0, "Tom");
        Crydis.zadd(key, 95.0, "Jerry");
        assertEquals(1L, Crydis.zcount(key, 90, 100));
        Double newScore = Crydis.zincrby(key, 5.0, "Tom");
        assertEquals(90.0, newScore, 0.001);
    }

    @Test @Order(23) public void testZSetBatchAdd() {
        String key = TEST_PREFIX + "zset:batch";
        Map<String, Double> data = new HashMap<>();
        data.put("X", 1.0);
        data.put("Y", 2.0);
        Crydis.zadd(key, data);
        assertEquals(2L, Crydis.zcard(key));
    }

    @Test @Order(24) public void testObjectSerialization() {
        String key = TEST_PREFIX + "object";
        User user = new User(1, "Cikian", "cikian@cikian.com");
        Crydis.setObject(key, user);
        User retrieved = Crydis.getObject(key, User.class);
        assertNotNull(retrieved);
        assertEquals("Cikian", retrieved.getName());
    }

    @Test @Order(25) public void testDistributedLock() {
        String key = TEST_PREFIX + "lock:test";
        boolean locked = Crydis.tryLock(key, 10, TimeUnit.SECONDS);
        assertTrue(locked);
        assertFalse(Crydis.tryLock(key, 10, TimeUnit.SECONDS));
        Crydis.unlock(key);
        assertTrue(Crydis.tryLock(key, 10, TimeUnit.SECONDS));
        Crydis.unlock(key);
    }

    @Test @Order(26) public void testDistributedLockWithValue() {
        String key = TEST_PREFIX + "lock:value";
        String v1 = UUID.randomUUID().toString();
        String v2 = UUID.randomUUID().toString();
        assertTrue(Crydis.tryLock(key, v1, 10, TimeUnit.SECONDS));
        assertFalse(Crydis.tryLock(key, v2, 10, TimeUnit.SECONDS));
        assertTrue(Crydis.unlock(key, v1));
        assertTrue(Crydis.tryLock(key, v2, 10, TimeUnit.SECONDS));
        Crydis.unlock(key, v2);
    }

    public static class User {
        private int id;
        private String name;
        private String email;
        public User() {}
        public User(int id, String name, String email) {
            this.id = id; this.name = name; this.email = email;
        }
        public int getId() { return id; }
        public void setId(int id) { this.id = id; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
    }
}
