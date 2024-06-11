package com.lei.cloud.mall.order.loadbalance;

import brave.Span;
import brave.Tracer;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.cloud.client.loadbalancer.reactive.ReactiveLoadBalancer;
import org.springframework.cloud.loadbalancer.support.LoadBalancerClientFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.mockito.Mockito.when;

/**
 * <p>
 * LoadBalanceTest
 * </p>
 *
 * @author 伍磊
 */
@SpringBootTest(properties = {"spring.cloud.loadbalancer.zone=zone1"})
public class LoadBalanceTest {

    // 自动装配，即自动装配 spring.factories
    @EnableAutoConfiguration
    @Configuration
    @ImportAutoConfiguration(classes = LoadBalancerAutoConfiguration.class)
    public static class App {

        @Bean
        @Primary
        public DiscoveryClient myDiscoveryClient() {

            ServiceInstance serviceInstance1 = Mockito.spy(ServiceInstance.class);
            ServiceInstance serviceInstance2 = Mockito.spy(ServiceInstance.class);
            ServiceInstance serviceInstance3 = Mockito.spy(ServiceInstance.class);
            Map<String, String> zone1 = Map.ofEntries(
                    Map.entry("zone", "zone1")
            );
            Map<String, String> zone2 = Map.ofEntries(
                    Map.entry("zone", "zone2")
            );
            when(serviceInstance1.getMetadata()).thenReturn(zone1);
            when(serviceInstance1.getInstanceId()).thenReturn("instance1");
            when(serviceInstance2.getMetadata()).thenReturn(zone1);
            when(serviceInstance2.getInstanceId()).thenReturn("instance2");
            when(serviceInstance3.getMetadata()).thenReturn(zone2);
            when(serviceInstance3.getInstanceId()).thenReturn("instance3");

            DiscoveryClient spy = Mockito.spy(DiscoveryClient.class);
            when(spy.getInstances("testService"))
                    .thenReturn(List.of(serviceInstance1, serviceInstance2, serviceInstance3));
            return spy;
        }

    }

    @SpyBean
    private LoadBalancerClientFactory loadBalancerClientFactory;
    @SpyBean
    private Tracer tracer;


    /**
     * 只返回同一个 zone 下的实例
     */
    @Test
    public void testFilteredByZone() {
        ReactiveLoadBalancer<ServiceInstance> testService =
                loadBalancerClientFactory.getInstance("testService");
        for (int i = 0; i < 100; i++) {
            ServiceInstance server = Mono.from(testService.choose()).block().getServer();
            // 必须处于和当前实例同一个zone下
            assertEquals(server.getMetadata().get("zone"), "zone1");
        }
    }

    /**
     * 返回不同的实例
     */
    @Test
    public void testReturnNext() {
        ReactiveLoadBalancer<ServiceInstance> testService =
                loadBalancerClientFactory.getInstance("testService");
        Span span = tracer.nextSpan();
        for (int i = 0; i < 100; i++) {
            // 同一个线程中使用同一个traceId访问，每次选择不同的实例
            try (Tracer.SpanInScope cleared = tracer.withSpanInScope(span)) {
                ServiceInstance server1 = Mono.from(testService.choose()).block().getServer();
                ServiceInstance server2 = Mono.from(testService.choose()).block().getServer();
                // 每次选择的是不同实例
                assertNotEquals(server1.getInstanceId(), server2.getInstanceId());
            }
        }
    }

    /**
     * 跨线程，默认情况下是可能返回同一实例的，在我们的实现下，保持
     * span 则会返回下一个实例，这样保证多线程环境同一个 request 重试会返回下一实例
     *
     * @throws Exception
     */
    @Test
    public void testSameSpanReturnNext() throws Exception {
        Span span = tracer.nextSpan();
        for (int i = 0; i < 100; i++) {
            try (Tracer.SpanInScope cleared = tracer.withSpanInScope(span)) {
                ReactiveLoadBalancer<ServiceInstance> testService =
                        loadBalancerClientFactory.getInstance("testService");
                // 线程1访问
                ServiceInstance serviceInstance1 = Mono.from(testService.choose()).block().getServer();
                // 线程2访问
                AtomicReference<ServiceInstance> serviceInstance2 = new AtomicReference<>();
                Thread thread = new Thread(() -> {
                    try (Tracer.SpanInScope cleared2 = tracer.withSpanInScope(span)) {
                        serviceInstance2.set(Mono.from(testService.choose()).block().getServer());
                    }
                });
                thread.start();
                thread.join();
                assertNotEquals(serviceInstance1.getInstanceId(), serviceInstance2.get().getInstanceId());
            }
        }
    }


}
