package com.aoaojiao.rpc.spring.support;

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
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.core.env.Environment;

import java.lang.reflect.Field;

public class RpcReferenceInjector implements BeanPostProcessor {
    private final Environment env;
    private final ServiceDiscovery discovery;

    public RpcReferenceInjector(Environment env, ServiceDiscovery discovery) {
        this.env = env;
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

        LoadBalancer lb = createLoadBalancer(ref.loadBalancer());
        FaultTolerance ft = createFaultTolerance(ref.faultTolerance(), ref.retryTimes());
        RateLimiter limiter = ref.rateLimit() > 0 ? new FixedWindowRateLimiter(ref.rateLimit()) : null;
        CircuitBreaker breaker = new CircuitBreaker(ref.circuitFailureThreshold(), ref.circuitOpenMillis());

        long timeout = ref.timeoutMillis() > 0 ? ref.timeoutMillis()
                : Long.parseLong(env.getProperty("aoaojiao.rpc.client.timeoutMillis", "3000"));

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
