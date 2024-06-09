package com.lei.cloud.mall.order;

import brave.Span;
import brave.Tracer;
import brave.propagation.TraceContext;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.EurekaClientAutoConfiguration;
import org.springframework.cloud.netflix.eureka.EurekaDiscoveryClientConfiguration;
import org.springframework.cloud.netflix.eureka.reactive.EurekaReactiveDiscoveryClientConfiguration;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

/**
 * <p>
 * SpringBootApplication
 * </p>
 *
 * @author 伍磊
 */
@SpringBootApplication(exclude = {
        // 排除eureka
        EurekaReactiveDiscoveryClientConfiguration.class,
        EurekaDiscoveryClientConfiguration.class,
        EurekaClientAutoConfiguration.class
})
@Log4j2
@RestController
@RequestMapping("/order")
public class OrderApplication implements CommandLineRunner {

    @Autowired
    private Tracer tracer;

    @Autowired
    private RestTemplate restTemplate;

    public static void main(String[] args) {
        SpringApplication.run(OrderApplication.class, args);
    }

    @GetMapping("/health")
    public String health() {
        log.info("请求...");
        Span span = tracer.currentSpan();
        new Thread(() -> {
            // 新构造的线程默认没有span，这里包装一层
            try (Tracer.SpanInScope cleared = tracer.withSpanInScope(span)) {
                log.info("新线程....");
            }
        }).start();

        // 请求外部服务
        restTemplate.getForEntity("http://localhost:8080/order/health1", String.class);

        return "OK";
    }

    @GetMapping("/health1")
    public String health1() throws InterruptedException {
        TraceContext context = tracer.currentSpan().context();
        log.info("health1....{}", context);
        TimeUnit.MILLISECONDS.sleep(ThreadLocalRandom.current().nextInt(950, 1050));
        return "ok1!";
    }

    @Override
    public void run(String... args) throws Exception {
        log.info("test....");
        Span span = tracer.currentSpan();
        System.out.println();

        // 之后在没有链路追踪信息的地方，使用 span 包裹起来
        try (Tracer.SpanInScope cleared = tracer.withSpanInScope(span)) {
            // 你的业务代码
        }
    }
}
