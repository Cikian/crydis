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

        /**
         * 支持非 Spring 链式配置安全包白名单列表。
         *
         * <p>fastjson2 的白名单是文本前缀匹配且不支持 {@code *} 通配符：
         * {@code "cn.foo."} 放行 {@code cn.foo} 包下所有类。只有在需要反序列化多态字段
         * （JSON 中带 {@code @type}）时才需要配置；不配置时默认只放行目标类型自身。</p>
         */
        public Builder allowedPackages(java.util.List<String> allowedPackages) {
            configuration.setAllowedPackages(allowedPackages);
            return this;
        }

        /**
         * 是否还原最外层带双引号的字符串，仅用于兼容历史版本写入的数据，默认 false。
         */
        public Builder unwrapQuotedString(boolean unwrapQuotedString) {
            configuration.setUnwrapQuotedString(unwrapQuotedString);
            return this;
        }

        public Crydis init() {
            return build();
        }

        /**
         * 构建并注册静态客户端。
         */
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