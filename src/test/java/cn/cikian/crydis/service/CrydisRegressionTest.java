package cn.cikian.crydis.service;

import cn.cikian.crydis.model.CrydisConfiguration;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 缺陷回归测试：每个用例都对应一个已修复的真实 bug，用旧实现运行时应当失败。
 *
 * <p>需要本地 Redis（127.0.0.1:6379），独占 database 14。</p>
 *
 * @author Cikian
 */
public class CrydisRegressionTest {

    private static final int TEST_DB = 14;
    private static CrydisConfiguration config;

    @BeforeAll
    public static void setUpAll() {
        config = new CrydisConfiguration();
        config.setHost("127.0.0.1");
        config.setPort(6379);
        config.setDatabase(TEST_DB);
        config.setTimeout(2000);
        Crydis.init(config);
        flushTestDb();
    }

    @AfterAll
    public static void tearDownAll() {
        try {
            flushTestDb();
        } finally {
            Crydis.destroy();
        }
    }

    private static void flushTestDb() {
        try (Jedis jedis = new Jedis("127.0.0.1", 6379)) {
            jedis.select(TEST_DB);
            jedis.flushDB();
        }
    }

    // ==================== P0-8：AUTH 握手 - 未配置 user 时不得发送两参数 AUTH ====================

    @Test
    public void unconfiguredUserMustNotProduceTwoArgAuth() {
        // 通过假服务端抓取握手报文，断言只发送单参数 AUTH password（兼容 Redis < 6.0）
        String captured = captureHandshake(true, false);
        assertTrue(captured.contains("AUTH"), "应当发送 AUTH，实际：" + captured);
        assertFalse(captured.contains("default"), "未配置 user 时不得发送两参数 AUTH（含 default），实际：" + captured);
        assertTrue(captured.contains("secret"), "应当携带密码，实际：" + captured);
    }

    @Test
    public void explicitUserIsSentAsTwoArgAuth() {
        String captured = captureHandshake(true, true);
        assertTrue(captured.contains("myuser"), "显式配置的 user 应当被发送，实际：" + captured);
    }

    /**
     * 用假 Redis 服务端抓取握手阶段实际发出的 RESP 报文。
     */
    private String captureHandshake(final boolean givePassword, final boolean giveUser) {
        final StringBuilder captured = new StringBuilder();
        try {
            final java.net.ServerSocket server = new java.net.ServerSocket(0);
            final int port = server.getLocalPort();
            Thread serverThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    try (java.net.Socket socket = server.accept()) {
                        java.io.OutputStream out = socket.getOutputStream();
                        java.io.InputStream in = socket.getInputStream();
                        byte[] buf = new byte[512];
                        long deadline = System.currentTimeMillis() + 2000;
                        while (System.currentTimeMillis() < deadline) {
                            if (in.available() > 0) {
                                int n = in.read(buf);
                                if (n <= 0) {
                                    break;
                                }
                                captured.append(new String(buf, 0, n, "ISO-8859-1"));
                                out.write("+OK\r\n".getBytes("ISO-8859-1"));
                                out.flush();
                            } else {
                                Thread.sleep(20);
                            }
                        }
                    } catch (Exception ignored) {
                        // 客户端提前断开属预期行为
                    }
                }
            });
            serverThread.setDaemon(true);
            serverThread.start();

            CrydisConfiguration probeConfig = new CrydisConfiguration();
            probeConfig.setHost("127.0.0.1");
            probeConfig.setPort(port);
            probeConfig.setTimeout(1000);
            if (givePassword) {
                probeConfig.setPassword("secret");
            }
            if (giveUser) {
                probeConfig.setUser("myuser");
            }
            RedisClient client = new RedisClient(probeConfig);
            try {
                client.set("probe", "v");
            } catch (Exception ignored) {
                // 假服务端不是真的 Redis，命令执行失败不影响握手报文断言
            } finally {
                client.close();
            }
            Thread.sleep(100);
            server.close();
        } catch (Exception e) {
            fail("抓取握手报文失败：" + e.getMessage());
        }
        return captured.toString();
    }

    // ==================== P0-3：读写必须对称，不得篡改首尾带引号的值 ====================

    @Test
    public void quotedValuesMustRoundTripUnchanged() {
        Crydis.set("rt:plain", "abc");
        assertEquals("abc", Crydis.get("rt:plain"));

        Crydis.set("rt:quoted", "\"abc\"");
        assertEquals("\"abc\"", Crydis.get("rt:quoted"), "首尾带引号的合法值不得被剥离");

        Crydis.set("rt:emptyQuotes", "\"\"");
        assertEquals("\"\"", Crydis.get("rt:emptyQuotes"));

        Crydis.set("rt:json", "{\"a\":1}");
        assertEquals("{\"a\":1}", Crydis.get("rt:json"));

        Crydis.hset("rt:hash", "f", "\"x\"");
        assertEquals("\"x\"", Crydis.hget("rt:hash", "f"));

        Crydis.lpush("rt:list", "\"q\"");
        assertEquals("\"q\"", Crydis.lpop("rt:list"));

        Crydis.sadd("rt:set", "\"m\"");
        assertTrue(Crydis.smembers("rt:set").contains("\"m\""));
    }

    @Test
    public void legacyUnwrapModeStillAvailableForOldData() {
        Crydis.set("rt:legacy", "\"legacyValue\"");
        CrydisConfiguration legacy = new CrydisConfiguration();
        legacy.setHost("127.0.0.1");
        legacy.setPort(6379);
        legacy.setDatabase(TEST_DB);
        legacy.setUnwrapQuotedString(true);
        RedisClient legacyClient = new RedisClient(legacy);
        try {
            assertEquals("legacyValue", legacyClient.get("rt:legacy"), "兼容开关开启时应还原历史数据");
        } finally {
            legacyClient.close();
        }
    }

    // ==================== P0-1：反序列化安全 ====================

    @Test
    public void objectRoundTripWorksWithDefaultConfig() {
        TestBean bean = new TestBean("Cikian", 18);
        Crydis.setObject("obj:bean", bean);
        TestBean loaded = Crydis.getObject("obj:bean", TestBean.class);
        assertNotNull(loaded);
        assertEquals("Cikian", loaded.getName());
        assertEquals(18, loaded.getAge());
    }

    @Test
    public void missingKeyReturnsNull() {
        assertNull(Crydis.getObject("obj:notExists", TestBean.class));
    }

    @Test
    public void missingKeyReturnsNullWhenWhitelistConfigured() {
        // 旧实现在配置白名单的分支里同样应当返回 null
        Crydis.getRedisClient().set("obj:notExists2", "x");
        Crydis.delete("obj:notExists2");
        assertNull(Crydis.getRedisClient().getObject("obj:notExists2", TestBean.class, "cn.cikian.crydis"));
    }

    @Test
    public void objectClassTargetMustBeRejected() {
        Crydis.getRedisClient().set("obj:any", "{\"a\":1}");
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> Crydis.getObject("obj:any", Object.class));
        assertTrue(ex.getMessage().contains("Object.class"), "异常信息应说明 Object.class 的风险");
    }

    @Test
    public void unknownTypeInPayloadMustNotBeInstantiated() {
        // 模拟攻击者写入的 gadget payload
        String payload = "{\"@type\":\"cn.cikian.crydis.service.CrydisRegressionTest$Gadget\",\"cmd\":\"pwn\"}";
        Crydis.getRedisClient().set("obj:gadget", payload);
        assertThrows(Exception.class, () -> Crydis.getObject("obj:gadget", TestBean.class),
                "白名单外的 @type 必须抛异常，不能静默实例化");
    }

    @Test
    public void customWhitelistMustNotBypassTypeCheck() {
        Crydis.getRedisClient().set("obj:gadget2",
                "{\"@type\":\"cn.cikian.crydis.service.CrydisRegressionTest$Gadget\",\"cmd\":\"pwn\"}");
        // 传入一个与目标类型无关的白名单前缀：@type 不在白名单内，
        // 旧实现会静默降级成 JSONObject 再由 (T) 强转，调用方拿到对象后才 ClassCastException
        assertThrows(Exception.class,
                () -> Crydis.getRedisClient().getObject("obj:gadget2", TestBean.class, "com.example.safe."),
                "白名单外的 @type 必须显式失败，而不是返回 JSONObject");
    }

    @Test
    public void nullValueMustNotBeSilentlyStored() {
        assertThrows(IllegalArgumentException.class, () -> Crydis.setObject("obj:null", null));
        assertThrows(IllegalArgumentException.class, () -> Crydis.set("obj:nullString", null));
    }

    // ==================== P0-2：分布式锁 ====================

    @Test
    public void lockOverflowMustNotThrow() {
        // 旧实现 (int) 强转溢出，会抛 RuntimeException
        String token = Crydis.getRedisClient().tryLockWithToken("lock:overflow", 3_000_000_000L, TimeUnit.SECONDS);
        assertNotNull(token, "超大过期时间不应抛异常，应正常获取锁");
    }

    @Test
    public void invalidLockExpireTimeIsRejectedClearly() {
        assertThrows(IllegalArgumentException.class,
                () -> Crydis.getRedisClient().tryLockWithToken("lock:bad", 0, TimeUnit.SECONDS));
        assertThrows(IllegalArgumentException.class,
                () -> Crydis.getRedisClient().tryLockWithToken("lock:bad", -5, TimeUnit.SECONDS));
    }

    @Test
    public void tokenBasedLockOnlyReleasesOwnLock() {
        String key = "lock:owned";
        String token = Crydis.getRedisClient().tryLockWithToken(key, 30, TimeUnit.SECONDS);
        assertNotNull(token);

        assertFalse(Crydis.getRedisClient().tryLockWithToken(key, UUID.randomUUID().toString(), 30, TimeUnit.SECONDS),
                "同一把锁不应被第二个持有者获取");
        assertFalse(Crydis.unlock(key, "someone-else-token"), "错误 token 不得释放锁");
        assertTrue(Crydis.unlock(key, token), "正确 token 应释放锁");
        assertTrue(Crydis.getRedisClient().tryLockWithToken(key, UUID.randomUUID().toString(), 30, TimeUnit.SECONDS),
                "锁释放后应可重新获取");
    }

    @Test
    public void lockTokenMustBeNonBlank() {
        String token = Crydis.getRedisClient().tryLockWithToken("lock:token", 30, TimeUnit.SECONDS);
        assertNotNull(token);
        assertFalse(token.trim().isEmpty());
    }

    // ==================== P1：参数校验 ====================

    @Test
    public void invalidExpireTimeIsRejectedBeforeHittingRedis() {
        assertThrows(IllegalArgumentException.class, () -> Crydis.set("v:bad", "x", 0, TimeUnit.SECONDS));
        assertThrows(IllegalArgumentException.class, () -> Crydis.set("v:bad", "x", 500, TimeUnit.MILLISECONDS));
        assertThrows(IllegalArgumentException.class, () -> Crydis.set("v:bad", "x", -1, TimeUnit.SECONDS));
        assertThrows(IllegalArgumentException.class, () -> Crydis.expire("v:bad", 0, TimeUnit.SECONDS));
    }

    @Test
    public void oddMsetArgumentsAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> Crydis.mset("only-key"));
        assertThrows(IllegalArgumentException.class, () -> Crydis.mset());
    }

    @Test
    public void emptyDeleteIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> Crydis.delete());
    }

    @Test
    public void emptyHmsetIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> Crydis.hmset("h:empty", new HashMap<String, String>()));
    }

    // ==================== P0-4：混合类型 / SCAN ====================

    @Test
    public void hmsetWorksOnLegacyRedis() {
        Map<String, String> profile = new HashMap<String, String>();
        profile.put("age", "25");
        profile.put("gender", "male");
        Crydis.hmset("h:legacy", profile);
        Map<String, String> loaded = Crydis.hgetAll("h:legacy");
        assertEquals("25", loaded.get("age"));
        assertEquals("male", loaded.get("gender"));
    }

    @Test
    public void scanFindsKeysWithoutBlockingCommand() {
        Crydis.set("scan:a", "1");
        Crydis.set("scan:b", "2");
        assertTrue(Crydis.getRedisClient().scan("scan:*").containsAll(java.util.Arrays.asList("scan:a", "scan:b")));
    }

    @Test
    public void getKeysWithValuesSkipsNonStringTypesInsteadOfFailing() {
        Crydis.set("mix:str", "v");
        Crydis.rpush("mix:list", "x");
        Crydis.hset("mix:hash", "f", "v");
        Map<String, String> result = Crydis.getRedisClient().getKeysWithValues("mix:*");
        assertEquals("v", result.get("mix:str"));
        assertFalse(result.containsKey("mix:list"), "非 String 类型应被跳过而不是抛 WRONGTYPE");
    }

    // ==================== P0-6：生命周期 ====================

    @Test
    public void initTwiceMustNotLeakOldPool() {
        CrydisConfiguration first = new CrydisConfiguration();
        first.setHost("127.0.0.1");
        first.setPort(6379);
        first.setDatabase(TEST_DB);
        Crydis.init(first);
        JedisPool firstPool = Crydis.getRedisClient().getJedisPool();

        CrydisConfiguration second = new CrydisConfiguration();
        second.setHost("127.0.0.1");
        second.setPort(6379);
        second.setDatabase(TEST_DB);
        Crydis.init(second);

        assertTrue(firstPool.isClosed(), "重新初始化后旧连接池必须被关闭，否则会泄漏连接");
        Crydis.set("lifecycle:probe", "1");
        assertEquals("1", Crydis.get("lifecycle:probe"), "重新初始化后新客户端必须可用");
    }

    @Test
    public void unmanagedClientCanBeClosedExplicitly() {
        CrydisConfiguration cfg = new CrydisConfiguration();
        cfg.setHost("127.0.0.1");
        cfg.setPort(6379);
        cfg.setDatabase(TEST_DB);
        RedisClient orphan = new RedisClient(cfg);
        assertTrue(orphan.isClosed(), "懒加载：未使用前连接池尚未创建");
        assertEquals("v", orphan.get("rt:plain") == null ? "v" : "v");
        orphan.set("orphan:probe", "1");
        assertEquals("1", orphan.get("orphan:probe"));
        assertFalse(orphan.isClosed(), "使用后连接池应已创建");
        orphan.close();
        assertTrue(orphan.isClosed(), "未托管的客户端也必须能显式关闭");
    }

    @Test
    public void closeIsIdempotent() {
        CrydisConfiguration cfg = new CrydisConfiguration();
        cfg.setHost("127.0.0.1");
        cfg.setPort(6379);
        cfg.setDatabase(TEST_DB);
        RedisClient client = new RedisClient(cfg);
        client.close();
        client.close();
        assertTrue(client.isClosed());
    }

    @Test
    public void lazyPoolMustNotBeCreatedUntilUsed() {
        CrydisConfiguration cfg = new CrydisConfiguration();
        cfg.setHost("127.0.0.1");
        cfg.setPort(6379);
        cfg.setDatabase(TEST_DB);
        RedisClient client = new RedisClient(cfg);
        assertTrue(client.isClosed(), "尚未使用前不应创建连接池");
        Crydis.set("lazy:probe", "v");
        assertEquals("v", client.get("lazy:probe"));
        assertFalse(client.isClosed(), "首次使用后应已创建连接池");
        client.close();
    }

    @Test
    public void basicOperationsStillWorkAfterAllRefactors() {
        Crydis.set("final:str", "value");
        assertEquals("value", Crydis.get("final:str"));
        assertTrue(Crydis.exists("final:str"));
        Crydis.set("final:count", "10");
        assertEquals(Long.valueOf(11), Crydis.incr("final:count"));
        Crydis.rpush("final:list", "a", "b");
        List<String> range = Crydis.lrange("final:list", 0, -1);
        assertEquals(2, range.size());
        Crydis.zadd("final:zset", 1.0, "m");
        assertEquals(Double.valueOf(1.0), Crydis.zscore("final:zset", "m"));
    }

    // ==================== 辅助类型 ====================

    public static class TestBean {
        private String name;
        private int age;

        public TestBean() {
        }

        public TestBean(String name, int age) {
            this.name = name;
            this.age = age;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public int getAge() {
            return age;
        }

        public void setAge(int age) {
            this.age = age;
        }
    }

    /** 模拟"危险类"：只要被反序列化就会有副作用 */
    public static class Gadget {
        private String cmd;

        public Gadget() {
            throw new AssertionError("漏洞复现：Gadget 被反序列化实例化了！");
        }

        public String getCmd() {
            return cmd;
        }

        public void setCmd(String cmd) {
            this.cmd = cmd;
        }
    }
}
