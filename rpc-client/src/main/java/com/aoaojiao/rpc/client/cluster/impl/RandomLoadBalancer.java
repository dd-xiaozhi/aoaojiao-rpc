package com.aoaojiao.rpc.client.cluster.impl;

import com.aoaojiao.rpc.client.cluster.LoadBalancer;
import com.aoaojiao.rpc.registry.ServiceInstance;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class RandomLoadBalancer implements LoadBalancer {
    @Override
    public ServiceInstance select(List<ServiceInstance> instances) {
        if (instances == null || instances.isEmpty()) {
            return null;
        }
        int index = ThreadLocalRandom.current().nextInt(instances.size());
        return instances.get(index);
    }
}
