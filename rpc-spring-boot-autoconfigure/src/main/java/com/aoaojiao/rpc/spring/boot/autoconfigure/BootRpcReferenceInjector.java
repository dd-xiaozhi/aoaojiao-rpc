package com.aoaojiao.rpc.spring.boot.autoconfigure;

import com.aoaojiao.rpc.client.ClusterRpcClientProxyFactory;
import com.aoaojiao.rpc.client.cluster.ClusterClient;
import com.aoaojiao.rpc.client.cluster.LoadBalancer;
import com.aoaojiao.rpc.client.cluster.circuit.CircuitBreaker;
import com.aoaojiao.rpc.client.cluster.fault.FailFast;
import com.aoaojiao.rpc.client.cluster.fault.FailRetry;
import com.aoaojiao.rpc.client.cluster.fault.FaultTolerance;
import com.aoaojiao.rpc.client.cluster.impl.RandomLoadBalancer;
import com.aoaojiao.rpc.client.cluster.impl.RoundRobinLoadBalancer;
import com.aoaojiao.rpc.client.cluster.limit.FixedWindowRateLimiter;
import com.aoaojiao.rpc.client.cluster.limit.RateLimiter;
import com.aoaojiao.rpc.common.service.ServiceKey;
import com.aoaojiao.rpc.registry.ServiceDiscovery;
import com.aoaojiao.rpc.spring.annotation.RpcReference;
import com.aoaojiao.rpc.spring.boot.autoconfigure.properties.AoaojiaoRpcProperties;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;

import java.lang.reflect.Field;

public class BootRpcReferenceInjector implements BeanPostProcessor {
    private final AoaojiaoRpcProperties properties;
    private final ServiceDiscovery discovery;

    public BootRpcReferenceInjector(AoaojiaoRpcProperties properties, ServiceDiscovery discovery) {
        this.properties = properties;
        this.discovery = discovery;
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        Field[] fields = bean.getClass().getDeclaredFields();
        for (Field field : fields) {
            RpcReference reference = field.getAnnotation(RpcReference.class);
            if (reference == null) {
                continue;
            }
            if (discovery == null) {
                throw new IllegalStateException("Registry is disabled, cannot inject @RpcReference");
            }
            Object proxy = buildProxy(field.getType(), reference);
            try {
                field.setAccessible(true);
                field.set(bean, proxy);
            } catch (IllegalAccessException ex) {
                throw new IllegalStateException(ex);
            }
        }
        return bean;
    }

    private Object buildProxy(Class<?> interfaceClass, RpcReference ref) {
        ServiceKey key = ServiceKey.of(interfaceClass.getName(), ref.version(), ref.group());
        try {
            discovery.subscribe(key);
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }

        String lbName = ref.loadBalancer();
        String ftName = ref.faultTolerance();
        int retryTimes = ref.retryTimes();
        long rateLimit = ref.rateLimit();
        int circuitFailures = ref.circuitFailureThreshold();
        long circuitOpen = ref.circuitOpenMillis();
        long timeout = ref.timeoutMillis();

        if (lbName == null || lbName.isBlank()) {
            lbName = properties.getClient().getLoadBalancer();
        }
        if (ftName == null || ftName.isBlank()) {
            ftName = properties.getClient().getFaultTolerance();
        }
        if (retryTimes == 0) {
            retryTimes = properties.getClient().getRetryTimes();
        }
        if (rateLimit == 0) {
            rateLimit = properties.getClient().getRateLimit();
        }
        if (circuitFailures == 3) {
            circuitFailures = properties.getClient().getCircuitFailureThreshold();
        }
        if (circuitOpen == 3000) {
            circuitOpen = properties.getClient().getCircuitOpenMillis();
        }
        if (timeout == 3000) {
            timeout = properties.getClient().getTimeoutMillis();
        }

        LoadBalancer lb = createLoadBalancer(lbName);
        FaultTolerance ft = createFaultTolerance(ftName, retryTimes);
        RateLimiter limiter = rateLimit > 0 ? new FixedWindowRateLimiter(rateLimit) : null;
        CircuitBreaker breaker = new CircuitBreaker(circuitFailures, circuitOpen);

        ClusterClient clusterClient = new ClusterClient(discovery, lb, ft, limiter, breaker, timeout);
        ClusterRpcClientProxyFactory factory = new ClusterRpcClientProxyFactory(clusterClient);
        return factory.create(interfaceClass, ref.version(), ref.group());
    }

    private LoadBalancer createLoadBalancer(String name) {
        if ("random".equalsIgnoreCase(name)) {
            return new RandomLoadBalancer();
        }
        return new RoundRobinLoadBalancer();
    }

    private FaultTolerance createFaultTolerance(String name, int retryTimes) {
        if ("failRetry".equalsIgnoreCase(name)) {
            return new FailRetry(retryTimes);
        }
        return new FailFast();
    }
}
