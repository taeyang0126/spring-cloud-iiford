package com.lei.cloud.common;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.cloud.context.named.NamedContextFactory;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * <p>
 * CustomNameContextTest
 * </p>
 *
 * @author 伍磊
 */
public class CommonNameContextTest {

    private static final String PROPERTY_NAME = "test.client.name";

    @Test
    public void test() {
        // 1. 构建父上下文并刷新
        AnnotationConfigApplicationContext applicationContext = new AnnotationConfigApplicationContext();
        applicationContext.register(ParentConfig.class);
        applicationContext.refresh();
        ParentBean parentBean = applicationContext.getBean("parentBean", ParentBean.class);
        System.out.println(parentBean);
        assertNotNull(parentBean);

        // 2. 构建client
        // client中包含默认的配置类
        TestClient client = new TestClient(DefaultTestClientConfig.class);
        // 设置父上下文
        client.setApplicationContext(applicationContext);

        // 3. 构建两个子上下文
        TestSpec testSpec1 = new TestSpec("service1", new Class[]{Service1Config.class});
        TestSpec testSpec2 = new TestSpec("service2", new Class[]{Service2Config.class});
        // 字上下文设置到TestClient中
        client.setConfigurations(List.of(testSpec1, testSpec2));

        // 4. 验证结果
        // 1. 获取父上下文中的bean，且验证时同一个bean
        ParentBean service1ParentBean = client.getInstance("service1", ParentBean.class);
        ParentBean service2ParentBean = client.getInstance("service2", ParentBean.class);
        assertNotNull(service1ParentBean);
        assertNotNull(service2ParentBean);
        assertEquals(service1ParentBean, service2ParentBean);

        // 2. 验证子上下文中独立的bean
        Service1ConfigBean service1ConfigBean = client.getInstance("service1", Service1ConfigBean.class);
        assertNotNull(service1ConfigBean);
        ChildBean service1ChildBean = client.getInstance("service1", ChildBean.class);
        assertNotNull(service1ChildBean);
        assertEquals(service1ChildBean.name, "service1");

        Service2ConfigBean service2ConfigBean = client.getInstance("service2", Service2ConfigBean.class);
        assertNotNull(service2ConfigBean);
        ChildBean service2ChildBean = client.getInstance("service2", ChildBean.class);
        assertNotNull(service2ChildBean);
        //assertEquals(service2ChildBean.name, "service2");
        // 这里子上下文中没有定义，所以使用的是默认的配置文件中的bean
        assertEquals(service2ChildBean.name, "test");

        // 3. 验证client默认的配置
        CommonTestBean service1CommonBean = client.getInstance("service1", CommonTestBean.class);
        CommonTestBean service2CommonBean = client.getInstance("service2", CommonTestBean.class);
        assertNotNull(service1CommonBean);
        assertNotNull(service2CommonBean);
        assertEquals(service1CommonBean.name, "service1");
        assertEquals(service2CommonBean.name, "service2");

        // 4. 验证子上下文与默认配置出现冲突时的场景
        // 1. 默认的配置优先级高于自定义的，也就是说对于同一个bean来说，DefaultConfig中的定义的bean的优先级会高于子上下文中定义的bean，即获取到bean会是DefaultConfig中定义的bean
        // 2. 增加@ConditionOnMissBean 注解时，由于子上下文中已经定义了bean，那么DefaultConfig就不会定义bean，获取到的bean就是子上下文中定义的bean
    }

    @Configuration
    static class ParentConfig {
        @Bean
        public ParentBean parentBean() {
            return new ParentBean();
        }
    }

    static class ParentBean {
    }



    static class CommonTestBean {

        private final String name;

        public CommonTestBean(String name) {
            this.name = name;
        }
    }

    static class Service1ConfigBean {}
    static class Service2ConfigBean {}
    static class ChildBean {
        private final String name;

        public ChildBean(String name) {
            this.name = name;
        }
    }

    @Configuration
    static class Service1Config {

        @Bean
        public Service1ConfigBean service1ConfigBean() {
            return new Service1ConfigBean();
        }

        @Bean
        public ChildBean childBean() {
            return new ChildBean("service1");
        }

    }

    @Configuration
    static class Service2Config {

        @Bean
        public Service2ConfigBean service1ConfigBean() {
            return new Service2ConfigBean();
        }

   /*     @Bean
        public ChildBean childBean() {
            return new ChildBean("service2");
        }*/

    }

    @Configuration
    static class DefaultTestClientConfig {
        @Bean
        public CommonTestBean commonTestBean(Environment environment) {
            // 获取到当前子上下文中的名称
            String property = environment.getProperty(PROPERTY_NAME);
            // 构建bean
            return new CommonTestBean(property);
        }

        @Bean
        @ConditionalOnMissingBean
        public ChildBean childBean() {
            return new ChildBean("test");
        }
    }


    // 配置文件 针对单个客户端子上下文的配置
    static class TestSpec implements NamedContextFactory.Specification {

        private final String name;
        private final Class<?>[] configurations;

        public TestSpec(String name, Class<?>[] configurations) {
            this.name = name;
            this.configurations = configurations;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public Class<?>[] getConfiguration() {
            return configurations;
        }
    }

    // 子上下文
    static class TestClient extends NamedContextFactory<TestSpec> {

        public TestClient(Class<?> defaultConfigType) {
            /*
                defaultConfigType 表示默认的configuration，此configuration也会register到子上下文中
                关于 defaultConfigType 与其他PROPERTY_NAME对应的TestSpec的顺序问题？
                -- 结论 如果是配置同一个bean，默认的配置文件的优先级高于子上下文中配置的，所以我们在定义默认的配置文件时
                需要尽可能留出扩展点，好让外部的子上下文能更好的扩展
             */
            // testClient -> 这个主要是加载的环境变量的key，也就是 MapPropertySource 中的key，业务上没有实际的意义
            // PROPERTY_NAME 表示子上下文的名称，也就是构造TestSpec中返回的名称
            super(defaultConfigType, "testClient", PROPERTY_NAME);
        }
    }

}
