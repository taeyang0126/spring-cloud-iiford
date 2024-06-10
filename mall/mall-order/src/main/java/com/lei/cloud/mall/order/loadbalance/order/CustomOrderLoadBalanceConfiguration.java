package com.lei.cloud.mall.order.loadbalance.order;

import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.cloud.loadbalancer.cache.LoadBalancerCacheManager;
import org.springframework.cloud.loadbalancer.config.LoadBalancerZoneConfig;
import org.springframework.cloud.loadbalancer.core.CachingServiceInstanceListSupplier;
import org.springframework.cloud.loadbalancer.core.DiscoveryClientServiceInstanceListSupplier;
import org.springframework.cloud.loadbalancer.core.ServiceInstanceListSupplier;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;

/**
 * <p>
 * 自定义订单loadBalance配置
 * <li>
 *     <ol>调用非当前集群的服务</ol>
 * </li>
 * </p>
 *
 * @author 伍磊
 */
@Log4j2
public class CustomOrderLoadBalanceConfiguration {

    @Bean
    public ServiceInstanceListSupplier diffZoneServiceInstanceListSupplier(
            DiscoveryClient discoveryClient,
            Environment environment,
            ConfigurableApplicationContext context,
            LoadBalancerZoneConfig zoneConfig
    ) {
        // 构建服务发现
        DiscoveryClientServiceInstanceListSupplier discoveryClientServiceInstanceListSupplier = new DiscoveryClientServiceInstanceListSupplier(discoveryClient, environment);
        // 构建不同zone转发
        DiffZoneServiceInstanceListSupplier diffZoneServiceInstanceListSupplier = new DiffZoneServiceInstanceListSupplier(discoveryClientServiceInstanceListSupplier, zoneConfig);
        // 如果有缓存则使用缓存
        ObjectProvider<LoadBalancerCacheManager> cacheManagerProvider = context
                .getBeanProvider(LoadBalancerCacheManager.class);
        if (cacheManagerProvider.getIfAvailable() != null) {
            return new CachingServiceInstanceListSupplier(diffZoneServiceInstanceListSupplier, cacheManagerProvider.getIfAvailable());
        }
        if (log.isWarnEnabled()) {
            log.warn("LoadBalancerCacheManager not available, returning delegate without caching.");
        }
        return diffZoneServiceInstanceListSupplier;
    }
}
