package com.lei.cloud.mall.order.webfactory;

import io.undertow.UndertowOptions;
import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.boot.web.embedded.undertow.ConfigurableUndertowWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;

/**
 * <p>
 * DefaultWebServerFactoryCustomizer
 * accesslog中打印请求时间
 * </p>
 *
 * @author 伍磊
 */
public class DefaultWebServerFactoryCustomizer implements WebServerFactoryCustomizer<ConfigurableUndertowWebServerFactory> {


    private final ServerProperties serverProperties;

    public DefaultWebServerFactoryCustomizer(ServerProperties serverProperties) {
        this.serverProperties = serverProperties;
    }

    @Override
    public void customize(ConfigurableUndertowWebServerFactory factory) {
        String pattern = serverProperties.getUndertow().getAccesslog().getPattern();
        // 如果 accesslog 配置中打印了响应时间，则打开记录请求开始时间配置
        if (logRequestProcessingTiming(pattern)) {
            factory.addBuilderCustomizers(builder ->
                    // 记录请求开始时间
                    builder.setServerOption(
                            UndertowOptions.RECORD_REQUEST_START_TIME,
                            true
                    )
            );
        }
    }

    private boolean logRequestProcessingTiming(String pattern) {
        // 如果没有配置 accesslog，则直接返回 false
        if (StringUtils.isBlank(pattern)) {
            return false;
        }
        // 目前只有 %D 和 %T 这两个占位符和响应时间有关，通过这个判断
        return pattern.contains("%D") || pattern.contains("%T");
    }

}
