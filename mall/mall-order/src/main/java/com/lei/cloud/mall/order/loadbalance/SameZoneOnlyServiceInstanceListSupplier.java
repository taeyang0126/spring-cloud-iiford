package com.lei.cloud.mall.order.loadbalance;

import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.loadbalancer.config.LoadBalancerZoneConfig;
import org.springframework.cloud.loadbalancer.core.DelegatingServiceInstanceListSupplier;
import org.springframework.cloud.loadbalancer.core.ServiceInstanceListSupplier;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * <p>
 * 只返回与当前实例同一个 Zone 的服务实例，不同 zone 之间的服务不互相调用
 * </p>
 *
 * @author 伍磊
 */
public class SameZoneOnlyServiceInstanceListSupplier extends DelegatingServiceInstanceListSupplier {

    private final String ZONE = "zone";
    private final LoadBalancerZoneConfig loadBalancerZoneConfig;
    private String zone;

    public SameZoneOnlyServiceInstanceListSupplier(ServiceInstanceListSupplier delegate, LoadBalancerZoneConfig loadBalancerZoneConfig) {
        super(delegate);
        this.loadBalancerZoneConfig = loadBalancerZoneConfig;
    }

    @Override
    public Flux<List<ServiceInstance>> get() {
        return getDelegate().get().map(this::filteredByZone);
    }

    private List<ServiceInstance> filteredByZone(List<ServiceInstance> serviceInstances) {
        if (zone == null) {
            zone = loadBalancerZoneConfig.getZone();
        }
        if (zone != null) {
            List<ServiceInstance> filteredInstances = new ArrayList<>();
            for (ServiceInstance serviceInstance : serviceInstances) {
                String instanceZone = getZone(serviceInstance);
                if (zone.equalsIgnoreCase(instanceZone)) {
                    filteredInstances.add(serviceInstance);
                }
            }
            if (!filteredInstances.isEmpty()) {
                return filteredInstances;
            }
        }

        // 我们这里为了实现不同 zone 之间不互相调用需要返回空列表
        return List.of();
    }


    // 读取实例的 zone，没有配置则为 null
    private String getZone(ServiceInstance serviceInstance) {
        Map<String, String> metadata = serviceInstance.getMetadata();
        if (metadata != null) {
            return metadata.get(ZONE);
        }
        return null;
    }
}
