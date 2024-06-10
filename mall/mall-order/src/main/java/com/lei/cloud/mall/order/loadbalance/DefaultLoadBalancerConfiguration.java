package com.lei.cloud.mall.order.loadbalance;

import brave.Tracer;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.cloud.loadbalancer.cache.LoadBalancerCacheManager;
import org.springframework.cloud.loadbalancer.config.LoadBalancerZoneConfig;
import org.springframework.cloud.loadbalancer.core.CachingServiceInstanceListSupplier;
import org.springframework.cloud.loadbalancer.core.DiscoveryClientServiceInstanceListSupplier;
import org.springframework.cloud.loadbalancer.core.ReactorLoadBalancer;
import org.springframework.cloud.loadbalancer.core.ServiceInstanceListSupplier;
import org.springframework.cloud.loadbalancer.support.LoadBalancerClientFactory;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;

/**
 * <p>
 * loa
 * </p>
 *
 * @author 伍磊
 */
@Log4j2
public class DefaultLoadBalancerConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public ServiceInstanceListSupplier serviceInstanceListSupplier(
            DiscoveryClient discoveryClient,
            Environment environment,
            ConfigurableApplicationContext context,
            LoadBalancerZoneConfig zoneConfig
    ) {
        // 构建服务发现
        DiscoveryClientServiceInstanceListSupplier discoveryClientServiceInstanceListSupplier = new DiscoveryClientServiceInstanceListSupplier(discoveryClient, environment);
        // 构建相同zone转发
        SameZoneOnlyServiceInstanceListSupplier sameZoneOnlyServiceInstanceListSupplier = new SameZoneOnlyServiceInstanceListSupplier(discoveryClientServiceInstanceListSupplier, zoneConfig);
        // 如果有缓存则使用缓存
        ObjectProvider<LoadBalancerCacheManager> cacheManagerProvider = context
                .getBeanProvider(LoadBalancerCacheManager.class);
        if (cacheManagerProvider.getIfAvailable() != null) {
            return new CachingServiceInstanceListSupplier(sameZoneOnlyServiceInstanceListSupplier, cacheManagerProvider.getIfAvailable());
        }
        if (log.isWarnEnabled()) {
            log.warn("LoadBalancerCacheManager not available, returning delegate without caching.");
        }
        return sameZoneOnlyServiceInstanceListSupplier;
    }

    @Bean
    @ConditionalOnMissingBean
    public ReactorLoadBalancer<ServiceInstance> reactorServiceInstanceLoadBalancer(
            Environment environment,
            LoadBalancerClientFactory loadBalancerClientFactory,
            Tracer tracer
    ) {
        String name = environment.getProperty(LoadBalancerClientFactory.PROPERTY_NAME);
        return new RoundRobinWithRequestSeparatedPositionLoadBalancer(
                // 获取到子上下文中的 ServiceInstanceListSupplier
                loadBalancerClientFactory.getLazyProvider(name, ServiceInstanceListSupplier.class),
                name,
                tracer
        );
    }


}
