package com.lei.cloud.mall.order.loadbalance;

import org.springframework.cloud.loadbalancer.annotation.LoadBalancerClients;
import org.springframework.context.annotation.Configuration;

/**
 * <p>
 * 定义默认的配置为 DefaultLoadBalancerConfiguration
 * @see DefaultLoadBalancerConfiguration
 * </p>
 *
 * @author 伍磊
 */
@Configuration(proxyBeanMethods = false)
@LoadBalancerClients(defaultConfiguration = DefaultLoadBalancerConfiguration.class)
public class LoadBalancerAutoConfiguration {
}
