package cn.cikian.crydis;

import cn.cikian.crydis.model.CrydisConfiguration;
import cn.cikian.crydis.service.Crydis;
import cn.cikian.crydis.service.RedisClient;

/**
 * Crydis管理器（非Spring项目使用）
 * 提供流式API风格的手动配置方式
 *
 * @author Cikian
 * @version 1.0
 * @since 2026-05-31
 */
public class CrydisManager {

    private CrydisManager() {
    }

    public static class Builder {
        private final CrydisConfiguration configuration;

        public Builder() {
            this.configuration = new CrydisConfiguration();
        }

        public Builder host(String host) {
            configuration.setHost(host);
            return this;
        }

        public Builder port(int port) {
            configuration.setPort(port);
            return this;
        }

        public Builder password(String password) {
            configuration.setPassword(password);
            return this;
        }

        public Builder database(int database) {
            configuration.setDatabase(database);
            return this;
        }

        public Builder timeout(int timeout) {
            configuration.setTimeout(timeout);
            return this;
        }

        public Builder maxActive(int maxActive) {
            configuration.setMaxActive(maxActive);
            return this;
        }

        public Builder maxIdle(int maxIdle) {
            configuration.setMaxIdle(maxIdle);
            return this;
        }

        public Builder minIdle(int minIdle) {
            configuration.setMinIdle(minIdle);
            return this;
        }

        public Builder maxWait(long maxWait) {
            configuration.setMaxWait(maxWait);
            return this;
        }

        public Crydis init() {
            if (configuration.getHost() == null || configuration.getHost().isEmpty()) {
                throw new IllegalArgumentException("Redis host不能为空");
            }

            RedisClient redisClient = new RedisClient(configuration);
            Crydis.init(redisClient);
            // 修复原始代码中无论如何都返回 null 的 Bug
            return new Crydis(redisClient);
        }

        public Crydis build() {
            if (configuration.getHost() == null || configuration.getHost().isEmpty()) {
                throw new IllegalArgumentException("Redis host不能为空");
            }

            RedisClient redisClient = new RedisClient(configuration);
            Crydis.init(redisClient);
            return new Crydis(redisClient);
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    public static void destroy() {
        Crydis.destroy();
    }
}