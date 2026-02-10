package com.aoaojiao.rpc.client.cluster.impl;

import com.aoaojiao.rpc.client.cluster.LoadBalancer;
import com.aoaojiao.rpc.registry.ServiceInstance;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class RoundRobinLoadBalancer implements LoadBalancer {
    private final AtomicInteger index = new AtomicInteger(0);

    @Override
    public ServiceInstance select(List<ServiceInstance> instances) {
        if (instances == null || instances.isEmpty()) {
            return null;
        }
        int i = Math.abs(index.getAndIncrement());
        return instances.get(i % instances.size());
    }
}
