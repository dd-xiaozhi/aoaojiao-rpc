package com.aoaojiao.rpc.client.cluster.fault;

import com.aoaojiao.rpc.client.cluster.LoadBalancer;
import com.aoaojiao.rpc.common.service.RpcInvocation;
import com.aoaojiao.rpc.common.service.ServiceKey;
import com.aoaojiao.rpc.registry.ServiceDiscovery;

public interface FaultTolerance {
    Object invoke(ServiceDiscovery discovery,
                  LoadBalancer loadBalancer,
                  ServiceKey key,
                  RpcInvocation invocation,
                  long timeoutMillis) throws Exception;
}
