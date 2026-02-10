package com.aoaojiao.rpc.spring.support;

import com.aoaojiao.rpc.registry.RegistryService;
import com.aoaojiao.rpc.registry.ServiceInstance;
import com.aoaojiao.rpc.server.RpcServerBootstrap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.SmartLifecycle;

public class RpcServerLifecycle implements SmartLifecycle {
    private static final Logger log = LoggerFactory.getLogger(RpcServerLifecycle.class);

    private final RpcServerBootstrap bootstrap;
    private volatile boolean running;

    public RpcServerLifecycle(int port, int threads, RegistryService registryService, ServiceInstance instance) {
        this.bootstrap = new RpcServerBootstrap(port, threads, registryService, instance);
    }

    public RpcServerBootstrap bootstrap() {
        return bootstrap;
    }

    @Override
    public void start() {
        try {
            bootstrap.start();
            running = true;
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(ex);
        }
    }

    @Override
    public void stop() {
        bootstrap.stop();
        running = false;
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    @Override
    public int getPhase() {
        return 0;
    }

    @Override
    public boolean isAutoStartup() {
        return true;
    }

    @Override
    public void stop(Runnable callback) {
        stop();
        callback.run();
    }
}
