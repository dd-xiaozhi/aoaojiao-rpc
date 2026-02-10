package com.aoaojiao.rpc.spring.boot.autoconfigure;

import com.aoaojiao.rpc.registry.LocalRegistryService;
import com.aoaojiao.rpc.registry.NacosRegistryService;
import com.aoaojiao.rpc.registry.RegistryConfig;
import com.aoaojiao.rpc.registry.RegistryService;
import com.aoaojiao.rpc.registry.RegistrySwitch;
import com.aoaojiao.rpc.registry.ServiceDiscovery;
import com.aoaojiao.rpc.registry.ServiceInstance;
import com.aoaojiao.rpc.spring.boot.autoconfigure.properties.AoaojiaoRpcProperties;
import com.aoaojiao.rpc.spring.config.RpcSpringConfiguration;
import com.aoaojiao.rpc.spring.support.RpcServerLifecycle;
import com.aoaojiao.rpc.spring.support.RpcServiceRegistrar;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

@AutoConfiguration
@ConditionalOnClass(RpcSpringConfiguration.class)
@EnableConfigurationProperties(AoaojiaoRpcProperties.class)
public class AoaojiaoRpcAutoConfiguration {
    @Bean
    @ConditionalOnMissingBean
    public RegistryConfig registryConfig(AoaojiaoRpcProperties properties) {
        RegistryConfig config = new RegistryConfig();
        config.setServerAddr(properties.getRegistry().getServerAddr());
        config.setNamespace(properties.getRegistry().getNamespace());
        config.setGroup(properties.getRegistry().getGroup());
        config.setUsername(properties.getRegistry().getUsername());
        config.setPassword(properties.getRegistry().getPassword());
        return config;
    }

    @Bean(destroyMethod = "close")
    @ConditionalOnMissingBean
    public RegistryService registryService(AoaojiaoRpcProperties properties, RegistryConfig config) throws Exception {
        String mode = properties.getRegistry().getMode();
        if ("local".equalsIgnoreCase(mode)) {
            return new LocalRegistryService();
        }
        if (!properties.getRegistry().isEnabled() || config.getServerAddr() == null || config.getServerAddr().isBlank()) {
            return new LocalRegistryService();
        }
        if (RegistrySwitch.isForceLocal()) {
            return new LocalRegistryService();
        }
        return new NacosRegistryService(config);
    }

    @Bean(destroyMethod = "close")
    @ConditionalOnMissingBean
    public ServiceDiscovery serviceDiscovery(RegistryService registryService) {
        return new ServiceDiscovery(registryService);
    }

    @Bean
    @ConditionalOnMissingBean
    public ServiceInstance serviceInstance(AoaojiaoRpcProperties properties) {
        return new ServiceInstance(properties.getServer().getHost(), properties.getServer().getPort());
    }

    @Bean
    @ConditionalOnMissingBean
    public RpcServerLifecycle rpcServerLifecycle(AoaojiaoRpcProperties properties,
                                                 RegistryService registryService,
                                                 ServiceInstance instance) {
        return new RpcServerLifecycle(properties.getServer().getPort(),
                properties.getServer().getBusinessThreads(),
                registryService,
                instance);
    }

    @Bean
    @ConditionalOnMissingBean
    public RpcServiceRegistrar rpcServiceRegistrar(RpcServerLifecycle lifecycle) {
        return new RpcServiceRegistrar(lifecycle);
    }

    @Bean
    @ConditionalOnMissingBean
    public BootRpcReferenceInjector rpcReferenceInjector(AoaojiaoRpcProperties properties, ServiceDiscovery discovery) {
        return new BootRpcReferenceInjector(properties, discovery);
    }
}
