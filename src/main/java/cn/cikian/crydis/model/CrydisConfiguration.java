package cn.cikian.crydis.model;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * Crydis配置类
 *
 * @author Cikian
 * @version 1.0
 * @since 2026-05-31
 */
@Data
@ConfigurationProperties(prefix = "ck.crydis")
public class CrydisConfiguration {

    private boolean enable = false;

    private String host;

    private Integer port = 6379;

    private String user;

    private String password;

    private Integer database = 0;

    private Integer timeout = 3000;

    private Integer maxActive = 50;

    private Integer maxIdle = 10;

    private Integer minIdle = 5;

    private Long maxWait = 3000L;

    private List<String> allowedPackages = new java.util.ArrayList<>();

    /**
     * 是否还原最外层带双引号的字符串（仅用于兼容历史版本写入的数据）。
     *
     * <p>历史版本写入 String 时不做任何转义，读取时却统一剥离首尾双引号，
     * 导致写入 {@code "abc"} 读出来变成 {@code abc}（数据被静默篡改）。
     * 默认 {@code false} 表示不做任何处理、保证读写对称；仅当需要读取历史数据时才设为 true。</p>
     */
    private boolean unwrapQuotedString = false;

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
                "最大等待时间：" + this.maxWait + "ms\n" +
                "安全白名单：" + this.allowedPackages + "\n" +
                "还原引号字符串：" + this.unwrapQuotedString + "\n";
    }
}
