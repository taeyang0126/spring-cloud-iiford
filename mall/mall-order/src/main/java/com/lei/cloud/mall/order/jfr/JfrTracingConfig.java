package com.lei.cloud.mall.order.jfr;

import brave.Tracer;
import org.springframework.boot.actuate.autoconfigure.metrics.web.servlet.WebMvcMetricsAutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.cloud.sleuth.autoconfig.instrument.web.ConditionalOnSleuthWeb;
import org.springframework.cloud.sleuth.autoconfig.instrument.web.SleuthWebProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.servlet.DispatcherType;

/**
 * <p>
 * JfrTracingConfig
 * </p>
 *
 * @author 伍磊
 */
@Configuration(proxyBeanMethods = false)
@EnableAutoConfiguration(exclude = WebMvcMetricsAutoConfiguration.class)
public class JfrTracingConfig {

    @Bean
    @ConditionalOnSleuthWeb
    public FilterRegistrationBean<JFRTracingFilter> jfrTracingFilter(Tracer tracer, SleuthWebProperties webProperties) {
        JFRTracingFilter filter = new JFRTracingFilter(tracer);
        FilterRegistrationBean<JFRTracingFilter> registration = new FilterRegistrationBean<>(filter);
        // 构建在sleuth之后，避免拿不到traceId & spanId
        registration.setOrder(webProperties.getFilterOrder() + 1);
        registration.setDispatcherTypes(DispatcherType.REQUEST, DispatcherType.ASYNC);
        return registration;
    }

}
