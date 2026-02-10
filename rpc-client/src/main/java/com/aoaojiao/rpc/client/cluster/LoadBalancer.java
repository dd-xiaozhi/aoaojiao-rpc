package com.aoaojiao.rpc.client.cluster;

import com.aoaojiao.rpc.registry.ServiceInstance;

import java.util.List;

public interface LoadBalancer {
    ServiceInstance select(List<ServiceInstance> instances);
}
