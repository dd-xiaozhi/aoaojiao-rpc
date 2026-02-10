package com.aoaojiao.rpc.client.cluster;

import com.aoaojiao.rpc.client.cluster.circuit.CircuitBreaker;
import com.aoaojiao.rpc.client.cluster.fault.FaultTolerance;
import com.aoaojiao.rpc.client.cluster.limit.RateLimiter;
import com.aoaojiao.rpc.common.service.RpcInvocation;
import com.aoaojiao.rpc.common.service.ServiceKey;
import com.aoaojiao.rpc.registry.ServiceDiscovery;

public class ClusterClient {
    private final ServiceDiscovery discovery;
    private final LoadBalancer loadBalancer;
    private final FaultTolerance faultTolerance;
    private final RateLimiter rateLimiter;
    private final CircuitBreaker circuitBreaker;
    private final long timeoutMillis;

    public ClusterClient(ServiceDiscovery discovery,
                         LoadBalancer loadBalancer,
                         FaultTolerance faultTolerance,
                         RateLimiter rateLimiter,
                         CircuitBreaker circuitBreaker,
                         long timeoutMillis) {
        this.discovery = discovery;
        this.loadBalancer = loadBalancer;
        this.faultTolerance = faultTolerance;
        this.rateLimiter = rateLimiter;
        this.circuitBreaker = circuitBreaker;
        this.timeoutMillis = timeoutMillis;
    }

    public Object invoke(ServiceKey key, RpcInvocation invocation) throws Exception {
        if (rateLimiter != null && !rateLimiter.tryAcquire()) {
            throw new IllegalStateException("Rate limit exceeded");
        }
        if (circuitBreaker != null && !circuitBreaker.allowRequest()) {
            throw new IllegalStateException("Circuit breaker open");
        }
        try {
            Object result = faultTolerance.invoke(discovery, loadBalancer, key, invocation, timeoutMillis);
            if (circuitBreaker != null) {
                circuitBreaker.onSuccess();
            }
            return result;
        } catch (Exception ex) {
            if (circuitBreaker != null) {
                circuitBreaker.onFailure();
            }
            throw ex;
        }
    }
}
