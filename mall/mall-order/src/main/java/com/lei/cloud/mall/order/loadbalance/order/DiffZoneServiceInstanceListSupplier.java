package com.lei.cloud.mall.order.loadbalance.order;

import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.loadbalancer.config.LoadBalancerZoneConfig;
import org.springframework.cloud.loadbalancer.core.DelegatingServiceInstanceListSupplier;
import org.springframework.cloud.loadbalancer.core.ServiceInstanceListSupplier;
import org.springframework.util.CollectionUtils;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * <p>
 * 调用服务的zone与当前服务不在一个集群间的调用
 * </p>
 *
 * @author 伍磊
 */
public class DiffZoneServiceInstanceListSupplier extends DelegatingServiceInstanceListSupplier {

    private static String ZONE = "zone";
    private final LoadBalancerZoneConfig zoneConfig;
    private String zone;

    public DiffZoneServiceInstanceListSupplier(ServiceInstanceListSupplier delegate,
                                               LoadBalancerZoneConfig zoneConfig) {
        super(delegate);
        this.zoneConfig = zoneConfig;
    }

    @Override
    public Flux<List<ServiceInstance>> get() {
        return getDelegate().get().map(this::filteredByZone);
    }

    private List<ServiceInstance> filteredByZone(List<ServiceInstance> serviceInstances) {
        if (CollectionUtils.isEmpty(serviceInstances)) {
            return List.of();
        }
        if (zone == null) {
            zone = zoneConfig.getZone();
        }
        // 当前服务zone=null & 调用服务zone不为null
        Predicate<ServiceInstance> zoneIsNull = t -> zone == null && getZone(t) != null;
        // 当前服务zone!=null & 与调用服务zone不一致
        Predicate<ServiceInstance> zoneNotNull = t -> zone != null && !zone.equals(getZone(t));
        return serviceInstances.stream()
                .filter(t -> zoneIsNull.test(t) || zoneNotNull.test(t))
                .collect(Collectors.toList());
    }

    private String getZone(ServiceInstance serviceInstance) {
        Map<String, String> metadata = serviceInstance.getMetadata();
        if (metadata != null) {
            return metadata.get(ZONE);
        }
        return null;
    }
}
