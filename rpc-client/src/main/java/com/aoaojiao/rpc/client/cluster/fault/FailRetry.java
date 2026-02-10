package com.aoaojiao.rpc.client.cluster.fault;

import com.aoaojiao.rpc.client.SimpleRpcClient;
import com.aoaojiao.rpc.client.cluster.LoadBalancer;
import com.aoaojiao.rpc.common.service.RpcInvocation;
import com.aoaojiao.rpc.common.service.ServiceKey;
import com.aoaojiao.rpc.registry.ServiceDiscovery;
import com.aoaojiao.rpc.registry.ServiceInstance;

import java.util.List;

public class FailRetry implements FaultTolerance {
    private final int maxRetry;

    public FailRetry(int maxRetry) {
        if (maxRetry < 0 || maxRetry > 2) {
            throw new IllegalArgumentException("maxRetry must be between 0 and 2");
        }
        this.maxRetry = maxRetry;
    }

    @Override
    public Object invoke(ServiceDiscovery discovery,
                         LoadBalancer loadBalancer,
                         ServiceKey key,
                         RpcInvocation invocation,
                         long timeoutMillis) throws Exception {
        Exception last = null;
        for (int i = 0; i <= maxRetry; i++) {
            List<ServiceInstance> instances = discovery.getInstances(key);
            ServiceInstance selected = loadBalancer.select(instances);
            if (selected == null) {
                throw new IllegalStateException("No provider available for: " + key);
            }
            SimpleRpcClient client = new SimpleRpcClient(selected.getHost(), selected.getPort());
            try {
                client.start();
                return client.invoke(invocation, timeoutMillis);
            } catch (Exception ex) {
                last = ex;
            } finally {
                client.close();
            }
        }
        throw last == null ? new IllegalStateException("Retry failed") : last;
    }
}
