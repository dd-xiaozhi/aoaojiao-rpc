package com.aoaojiao.rpc.spring.config;

import com.aoaojiao.rpc.registry.LocalRegistryService;
import com.aoaojiao.rpc.registry.NacosRegistryService;
import com.aoaojiao.rpc.registry.RegistryConfig;
import com.aoaojiao.rpc.registry.RegistryService;
import com.aoaojiao.rpc.registry.RegistrySwitch;
import com.aoaojiao.rpc.registry.ServiceDiscovery;
import com.aoaojiao.rpc.registry.ServiceInstance;
import com.aoaojiao.rpc.spring.support.RpcReferenceInjector;
import com.aoaojiao.rpc.spring.support.RpcServerLifecycle;
import com.aoaojiao.rpc.spring.support.RpcServiceRegistrar;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

@Configuration
public class RpcSpringConfiguration {
    @Bean
    public RegistryConfig registryConfig(Environment env) {
        RegistryConfig config = new RegistryConfig();
        config.setServerAddr(env.getProperty("aoaojiao.rpc.registry.serverAddr", ""));
        config.setNamespace(env.getProperty("aoaojiao.rpc.registry.namespace"));
        config.setGroup(env.getProperty("aoaojiao.rpc.registry.group", "DEFAULT_GROUP"));
        config.setUsername(env.getProperty("aoaojiao.rpc.registry.username"));
        config.setPassword(env.getProperty("aoaojiao.rpc.registry.password"));
        return config;
    }

    @Bean(destroyMethod = "close")
    public RegistryService registryService(RegistryConfig config, Environment env) throws Exception {
        boolean enabled = Boolean.parseBoolean(env.getProperty("aoaojiao.rpc.registry.enabled", "true"));
        String serverAddr = config.getServerAddr();
        String mode = env.getProperty("aoaojiao.rpc.registry.mode", "auto");
        if ("local".equalsIgnoreCase(mode)) {
            return new LocalRegistryService();
        }
        if (!enabled || serverAddr == null || serverAddr.isBlank()) {
            return new LocalRegistryService();
        }
        if (RegistrySwitch.isForceLocal()) {
            return new LocalRegistryService();
        }
        return new NacosRegistryService(config);
    }

    @Bean(destroyMethod = "close")
    public ServiceDiscovery serviceDiscovery(RegistryService registryService) {
        return new ServiceDiscovery(registryService);
    }

    @Bean
    public ServiceInstance serviceInstance(Environment env) {
        String host = env.getProperty("aoaojiao.rpc.server.host", "127.0.0.1");
        int port = Integer.parseInt(env.getProperty("aoaojiao.rpc.server.port", "9000"));
        return new ServiceInstance(host, port);
    }

    @Bean
    public RpcServerLifecycle rpcServerLifecycle(Environment env,
                                                 RegistryService registryService,
                                                 ServiceInstance instance) {
        int port = Integer.parseInt(env.getProperty("aoaojiao.rpc.server.port", "9000"));
        int threads = Integer.parseInt(env.getProperty("aoaojiao.rpc.server.businessThreads", "8"));
        return new RpcServerLifecycle(port, threads, registryService, instance);
    }

    @Bean
    public BeanPostProcessor rpcServiceRegistrar(RpcServerLifecycle lifecycle) {
        return new RpcServiceRegistrar(lifecycle);
    }

    @Bean
    public BeanPostProcessor rpcReferenceInjector(Environment env, ServiceDiscovery discovery) {
        return new RpcReferenceInjector(env, discovery);
    }
}
