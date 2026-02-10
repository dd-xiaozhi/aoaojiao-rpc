package com.aoaojiao.rpc.server;

import com.aoaojiao.rpc.common.service.ServiceKey;
import com.aoaojiao.rpc.core.netty.RpcServer;
import com.aoaojiao.rpc.registry.RegistryService;
import com.aoaojiao.rpc.registry.ServiceInstance;
import com.aoaojiao.rpc.server.invoker.DefaultServiceInvoker;
import com.aoaojiao.rpc.server.provider.DefaultServiceProvider;
import com.aoaojiao.rpc.server.provider.ServiceProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class RpcServerBootstrap {
    private static final Logger log = LoggerFactory.getLogger(RpcServerBootstrap.class);

    private final ServiceProvider provider;
    private final RpcServer server;
    private final List<ServiceKey> registeredKeys = new ArrayList<>();
    private final RegistryService registryService;
    private final ServiceInstance instance;

    public RpcServerBootstrap(int port, int businessThreads) {
        this(port, businessThreads, null, null);
    }

    public RpcServerBootstrap(int port, int businessThreads, RegistryService registryService, ServiceInstance instance) {
        this.provider = new DefaultServiceProvider();
        this.server = new RpcServer(port, businessThreads, new DefaultServiceInvoker(provider));
        this.registryService = registryService;
        this.instance = instance;
    }

    public void register(ServiceKey key, Object service) {
        provider.register(key, service);
        registeredKeys.add(key);
        if (registryService != null && instance != null) {
            try {
                registryService.register(key, instance);
                log.info("service registered: {} -> {}", key, instance);
            } catch (Exception ex) {
                throw new IllegalStateException("Register service failed: " + key, ex);
            }
        }
    }

    public void start() throws InterruptedException {
        server.start();
        log.info("rpc server started");
    }

    public void stop() {
        if (registryService != null && instance != null) {
            for (ServiceKey key : registeredKeys) {
                try {
                    registryService.unregister(key, instance);
                    log.info("service unregistered: {} -> {}", key, instance);
                } catch (Exception ignored) {
                }
            }
        }
        server.stopGracefully(5000);
        if (registryService != null) {
            registryService.close();
        }
        log.info("rpc server stopped");
    }
}
