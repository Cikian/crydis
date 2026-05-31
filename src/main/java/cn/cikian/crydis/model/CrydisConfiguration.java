package cn.cikian.crydis.model;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Crydis配置类
 *
 * @author Cikian
 * @version 1.0
 * @since 2026-05-31
 */
@Data
@ConfigurationProperties(prefix = "crydis")
public class CrydisConfiguration {

    private boolean enable = false;

    private String host;

    private Integer port = 6379;

    private String password;

    private Integer database = 0;

    private Integer timeout = 3000;

    private Integer maxActive = 50;

    private Integer maxIdle = 10;

    private Integer minIdle = 5;

    private Long maxWait = 3000L;

    public CrydisConfiguration() {
    }

    @Override
    public String toString() {
        return "---Crydis配置信息---" +
                "是否启用：" + isEnable() + "\n" +
                "地址：" + this.host + "\n" +
                "端口：" + this.port + "\n" +
                "数据库：" + this.database + "\n" +
                "超时时间：" + this.timeout + "ms\n" +
                "最大连接数：" + this.maxActive + "\n" +
                "最大空闲连接：" + this.maxIdle + "\n" +
                "最小空闲连接：" + this.minIdle + "\n" +
                "最大等待时间：" + this.maxWait + "ms\n";
    }
}
