package com.lei.cloud.mall.order.config;

import com.lei.cloud.mall.order.webfactory.DefaultWebServerFactoryCustomizer;
import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * <p>
 * UndertowAutoConfiguration
 * </p>
 *
 * @author 伍磊
 */
@Configuration(proxyBeanMethods = false)
public class UndertowAutoConfiguration {

    @Bean
    public DefaultWebServerFactoryCustomizer defaultWebServerFactoryCustomizer(ServerProperties serverProperties) {
        return new DefaultWebServerFactoryCustomizer(serverProperties);
    }

}
