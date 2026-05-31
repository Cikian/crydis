package cn.cikian.crydis;

import cn.cikian.crydis.service.Crydis;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Crydis 使用示例测试
 *
 * @author Cikian
 * @version 1.0
 * @since 2026-05-31
 */
public class CrydisTest {

    @Test
    public void testNonSpringUsage() {
        System.out.println("========== 非Spring项目使用示例 ==========");

        CrydisManager.builder()
                .host("localhost")
                .port(6379)
                .password(null)
                .database(1)
                .timeout(3000)
                .maxActive(50)
                .maxIdle(10)
                .minIdle(5)
                .maxWait(3000L)
                .init();

        testBasicOperations();

        CrydisManager.destroy();
        System.out.println("========== 测试完成 ==========");
    }

    private void testBasicOperations() {
        System.out.println("\n========== 基本操作测试 ==========");

        Crydis.set("test:key1", "Hello Crydis");
        String value = Crydis.get("test:key1");
        System.out.println("GET test:key1 = " + value);

        Crydis.set("test:key2", "Value with expire", 30, TimeUnit.SECONDS);
        boolean exists = Crydis.exists("test:key2");
        System.out.println("EXISTS test:key2 = " + exists);

        long ttl = Crydis.ttl("test:key2");
        System.out.println("TTL test:key2 = " + ttl + "s");

//        Crydis.delete("test:key1");
//        Crydis.delete("test:key2");
//        System.out.println("DELETE test:key1, test:key2");

        System.out.println("\n========== Hash操作测试 ==========");
        Map<String, String> hash = new HashMap<>();
        hash.put("field1", "value1");
        hash.put("field2", "value2");
        Crydis.hmset("test:hash", hash);

        String hget = Crydis.hget("test:hash", "field1");
        System.out.println("HGET test:hash field1 = " + hget);

        Map<String, String> hgetAll = Crydis.hgetAll("test:hash");
        System.out.println("HGETALL test:hash = " + hgetAll);

//        Crydis.delete("test:hash");

        System.out.println("\n========== 计数器测试 ==========");
        Crydis.set("test:counter", "100");
        Long incr = Crydis.incr("test:counter");
        System.out.println("INCR test:counter = " + incr);

        Long decr = Crydis.decr("test:counter");
        System.out.println("DECR test:counter = " + decr);

//        Crydis.delete("test:counter");

        System.out.println("\n========== 列表操作测试 ==========");
        Crydis.lpush("test:list", "a", "b", "c");
        Long llen = Crydis.llen("test:list");
        System.out.println("LLEN test:list = " + llen);

        java.util.List<String> lrange = Crydis.lrange("test:list", 0, -1);
        System.out.println("LRANGE test:list = " + lrange);

//        Crydis.delete("test:list");

        System.out.println("\n========== 对象操作测试 ==========");
        User user = new User(1, "Cikian", "cikian@cikian.com");
        Crydis.setObject("test:user", user);

        User retrievedUser = Crydis.getObject("test:user", User.class);
        System.out.println("GET OBJECT test:user = " + retrievedUser);

//        Crydis.delete("test:user");
    }

    static class User {
        private int id;
        private String name;
        private String email;

        public User() {}

        public User(int id, String name, String email) {
            this.id = id;
            this.name = name;
            this.email = email;
        }

        public int getId() { return id; }
        public void setId(int id) { this.id = id; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }

        @Override
        public String toString() {
            return "User{id=" + id + ", name='" + name + "', email='" + email + "'}";
        }
    }
}
