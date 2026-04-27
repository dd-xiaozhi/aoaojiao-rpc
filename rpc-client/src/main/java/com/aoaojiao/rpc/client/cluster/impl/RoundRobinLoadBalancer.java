package com.aoaojiao.rpc.client.cluster.impl;

import com.aoaojiao.rpc.client.cluster.LoadBalancer;
import com.aoaojiao.rpc.registry.ServiceInstance;

import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 轮询负载均衡器
 * 使用 AtomicLong 避免 Integer.MIN_VALUE 溢出的问题
 */
public class RoundRobinLoadBalancer implements LoadBalancer {
    // 使用 AtomicLong + 位运算安全取模，避免 Integer.MIN_VALUE 溢出问题
    private final AtomicLong index = new AtomicLong(0);

    @Override
    public ServiceInstance select(List<ServiceInstance> instances) {
        if (instances == null || instances.isEmpty()) {
            return null;
        }
        // 使用位运算 & 0x7FFFFFFF 确保结果为正数，然后取模
        int i = (int) (index.getAndIncrement() & 0x7FFFFFFF);
        return instances.get(i % instances.size());
    }
}
