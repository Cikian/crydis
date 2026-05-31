package cn.cikian.crydis.autoconfigure;

import cn.cikian.crydis.model.CrydisConfiguration;
import cn.cikian.crydis.service.Crydis;
import cn.cikian.crydis.service.RedisClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Crydis 自动配置类
 * 当Spring项目引入此依赖并配置crydis相关属性后，自动初始化
 *
 * @author Cikian
 * @version 1.0
 * @since 2026-05-31
 */
@Configuration
@ConditionalOnClass(Crydis.class)
@EnableConfigurationProperties(CrydisConfiguration.class)
@ConditionalOnProperty(prefix = "crydis", name = "enable", havingValue = "true")
public class CrydisAutoConfiguration {
    private static final Logger log = LoggerFactory.getLogger(CrydisAutoConfiguration.class);

    @Bean
    @ConditionalOnMissingBean(RedisClient.class)
    public RedisClient crydisRedisClient(CrydisConfiguration config) {
        log.info("Crydis 正在初始化 - 配置: {}", config);
        RedisClient redisClient = new RedisClient(config);
        log.info("Crydis RedisClient 初始化成功");
        return redisClient;
    }

    @Bean
    @ConditionalOnMissingBean(name = "crydisInit")
    public String crydisInit(RedisClient redisClient, CrydisConfiguration config) {
        log.info("Crydis 正在注册静态方法入口...");
        Crydis.init(redisClient);
        log.info("Crydis 初始化完成，可以直接使用 Crydis.xxx() 调用");
        return "crydis";
    }
}
