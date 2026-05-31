package cn.cikian.crydis.config;


import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.classic.spi.Configurator;
import ch.qos.logback.core.LogbackException;
import ch.qos.logback.core.joran.spi.JoranException;
import ch.qos.logback.core.spi.ContextAwareBase;

import java.net.URL;

/**
 * @author Cikian
 * @version 1.0
 * @implNote 智能日志配置装载器：当宿主项目无标准 logback 配置时，自动启用工具类精美日志
 * @see <a href="https://www.cikian.cn">https://www.cikian.cn</a>
 * @since 2026-05-16 02:33
 */
public class SmartLogbackConfigurator extends ContextAwareBase implements Configurator {
    @Override
    public void configure(LoggerContext loggerContext) {
        // 1. 检查类路径下是否存在宿主项目的标准配置文件
        URL hostConfig = Thread.currentThread().getContextClassLoader().getResource("logback.xml");
        URL hostTestConfig = Thread.currentThread().getContextClassLoader().getResource("logback-test.xml");

        if (hostConfig != null || hostTestConfig != null) {
            // 发现宿主项目自定义了日志配置，工具类选择静默，直接返回让 Logback 走标准默认流
            addInfo("检测到宿主项目已存在自带的 logback 配置文件，Cik-OSS 保持静默。");
            return;
        }

        // 2. 如果宿主项目没有任何 Logback 配置，则动态加载工具类内置的专属精美配置
        addInfo("未检测到宿主项目logback日志配置，正在启用 Cikian logback配置...");
        System.out.println("未检测到宿主项目logback日志配置，正在启用 Cikian logback配置...");
        URL defaultConfig = Thread.currentThread().getContextClassLoader().getResource("logback-default-cik.xml");

        if (defaultConfig != null) {
            try {
                JoranConfigurator configurator = new JoranConfigurator();
                configurator.setContext(loggerContext);

                // 关键点：1.2.x 必须在 doConfigure 之前 reset，清除默认生成的控制台 Appender
                loggerContext.reset();

                configurator.doConfigure(defaultConfig);
            } catch (LogbackException e) {
                addError("加载工具类内置日志配置失败", e);
            } catch (JoranException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
