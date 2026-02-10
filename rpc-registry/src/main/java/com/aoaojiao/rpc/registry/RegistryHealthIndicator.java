package com.aoaojiao.rpc.registry;

public interface RegistryHealthIndicator {
    boolean isHealthy();

    String report();
}
