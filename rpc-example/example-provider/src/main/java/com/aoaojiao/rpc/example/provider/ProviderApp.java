package com.aoaojiao.rpc.example.provider;

import com.aoaojiao.rpc.common.service.ServiceKey;
import com.aoaojiao.rpc.example.api.UserService;
import com.aoaojiao.rpc.registry.NacosRegistryService;
import com.aoaojiao.rpc.registry.RegistryConfig;
import com.aoaojiao.rpc.registry.RegistryService;
import com.aoaojiao.rpc.registry.ServiceInstance;
import com.aoaojiao.rpc.server.RpcServerBootstrap;

public class ProviderApp {
    public static void main(String[] args) throws Exception {
        RegistryConfig config = new RegistryConfig();
        config.setServerAddr("127.0.0.1:8848");

        RegistryService registryService = new NacosRegistryService(config);
        ServiceInstance instance = new ServiceInstance("127.0.0.1", 9000);

        RpcServerBootstrap bootstrap = new RpcServerBootstrap(9000, 8, registryService, instance);
        bootstrap.register(ServiceKey.of(UserService.class.getName(), "v1", "default"), new UserServiceImpl());
        bootstrap.start();
        Runtime.getRuntime().addShutdownHook(new Thread(bootstrap::stop));
        Thread.currentThread().join();
    }
}
