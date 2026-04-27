package com.aoaojiao.rpc.spring.boot.autoconfigure;

import com.aoaojiao.rpc.client.ClusterRpcClientProxyFactory;
import com.aoaojiao.rpc.client.cluster.ClusterClient;
import com.aoaojiao.rpc.client.cluster.LoadBalancer;
import com.aoaojiao.rpc.client.cluster.circuit.CircuitBreaker;
import com.aoaojiao.rpc.client.cluster.fault.FailFast;
import com.aoaojiao.rpc.client.cluster.fault.FailRetry;
import com.aoaojiao.rpc.client.cluster.fault.FaultTolerance;
import com.aoaojiao.rpc.client.cluster.fault.SmartRetryFaultTolerance;
import com.aoaojiao.rpc.client.cluster.impl.RandomLoadBalancer;
import com.aoaojiao.rpc.client.cluster.impl.RoundRobinLoadBalancer;
import com.aoaojiao.rpc.client.cluster.limit.FixedWindowRateLimiter;
import com.aoaojiao.rpc.client.cluster.limit.SlidingWindowRateLimiter;
import com.aoaojiao.rpc.client.cluster.limit.TokenBucketRateLimiter;
import com.aoaojiao.rpc.client.cluster.limit.RateLimiter;
import com.aoaojiao.rpc.common.service.ServiceKey;
import com.aoaojiao.rpc.registry.ServiceDiscovery;
import com.aoaojiao.rpc.spring.annotation.RpcReference;
import com.aoaojiao.rpc.spring.boot.autoconfigure.properties.AoaojiaoRpcProperties;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;

import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 优化的 RPC 引用注入器
 * 使用类索引避免遍历所有 Bean 的所有字段
 */
public class OptimizedRpcReferenceInjector implements BeanPostProcessor {

    private final AoaojiaoRpcProperties properties;
    private final ServiceDiscovery discovery;

    // 类索引：Class -> 包含 @RpcReference 字段的列表
    private final Map<Class<?>, List<RpcFieldInfo>> classIndex = new ConcurrentHashMap<>();

    // 代理缓存：接口名 -> 代理对象
    private final Map<String, Object> proxyCache = new ConcurrentHashMap<>();

    // 代理工厂缓存
    private final Map<String, ClusterRpcClientProxyFactory> factoryCache = new ConcurrentHashMap<>();

    public OptimizedRpcReferenceInjector(AoaojiaoRpcProperties properties, ServiceDiscovery discovery) {
        this.properties = properties;
        this.discovery = discovery;
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        Class<?> beanClass = bean.getClass();

        // 检查是否在索引中
        List<RpcFieldInfo> fieldInfos = classIndex.get(beanClass);
        if (fieldInfos == null) {
            return bean;
        }

        // 只处理索引中的类
        for (RpcFieldInfo info : fieldInfos) {
            try {
                Object proxy = getOrCreateProxy(info);
                info.field.setAccessible(true);
                info.field.set(bean, proxy);
            } catch (IllegalAccessException ex) {
                throw new IllegalStateException("Failed to inject RPC reference: " + info.field, ex);
            }
        }

        return bean;
    }

    /**
     * 注册类到索引中
     */
    public void registerClass(Class<?> clazz) {
        if (classIndex.containsKey(clazz)) {
            return;
        }

        List<RpcFieldInfo> fieldInfos = new ArrayList<>();
        Class<?> current = clazz;

        // 遍历类及其父类
        while (current != null && current != Object.class) {
            for (Field field : current.getDeclaredFields()) {
                RpcReference annotation = field.getAnnotation(RpcReference.class);
                if (annotation != null) {
                    fieldInfos.add(new RpcFieldInfo(field, annotation));
                }
            }
            current = current.getSuperclass();
        }

        if (!fieldInfos.isEmpty()) {
            classIndex.put(clazz, fieldInfos);
        }
    }

    /**
     * 获取或创建代理
     */
    private Object getOrCreateProxy(RpcFieldInfo info) {
        Class<?> interfaceClass = info.field.getType();
        String cacheKey = buildCacheKey(interfaceClass, info.annotation.version(), info.annotation.group());

        return proxyCache.computeIfAbsent(cacheKey, k -> {
            ServiceKey key = ServiceKey.of(interfaceClass.getName(), info.annotation.version(), info.annotation.group());
            try {
                discovery.subscribe(key);
            } catch (Exception ex) {
                throw new IllegalStateException("Failed to subscribe service: " + key, ex);
            }

            LoadBalancer lb = createLoadBalancer(info);
            FaultTolerance ft = createFaultTolerance(info);
            RateLimiter limiter = createRateLimiter(info);
            CircuitBreaker breaker = createCircuitBreaker(info);

            ClusterClient clusterClient = new ClusterClient(discovery, lb, ft, limiter, breaker, getTimeout(info));
            return getFactory(cacheKey, clusterClient).create(interfaceClass, info.annotation.version(), info.annotation.group());
        });
    }

    private ClusterRpcClientProxyFactory getFactory(String cacheKey, ClusterClient clusterClient) {
        return factoryCache.computeIfAbsent(cacheKey, k -> new ClusterRpcClientProxyFactory(clusterClient));
    }

    private LoadBalancer createLoadBalancer(RpcFieldInfo info) {
        String lbName = info.annotation.loadBalancer();
        if (lbName == null || lbName.isBlank()) {
            lbName = properties.getClient().getLoadBalancer();
        }
        if ("random".equalsIgnoreCase(lbName)) {
            return new RandomLoadBalancer();
        }
        return new RoundRobinLoadBalancer();
    }

    private FaultTolerance createFaultTolerance(RpcFieldInfo info) {
        String ftName = info.annotation.faultTolerance();
        int retryTimes = info.annotation.retryTimes();

        if (ftName == null || ftName.isBlank()) {
            ftName = properties.getClient().getFaultTolerance();
        }
        if (retryTimes == 0) {
            retryTimes = properties.getClient().getRetryTimes();
        }

        if ("smartRetry".equalsIgnoreCase(ftName)) {
            return new SmartRetryFaultTolerance(retryTimes);
        }
        if ("failRetry".equalsIgnoreCase(ftName)) {
            return new FailRetry(retryTimes);
        }
        return new FailFast();
    }

    private RateLimiter createRateLimiter(RpcFieldInfo info) {
        long rateLimit = info.annotation.rateLimit();
        if (rateLimit == 0) {
            rateLimit = properties.getClient().getRateLimit();
        }
        if (rateLimit <= 0) {
            return null;
        }

        String rateLimitType = properties.getClient().getRateLimitType();
        if ("slidingWindow".equalsIgnoreCase(rateLimitType)) {
            return new SlidingWindowRateLimiter(rateLimit);
        } else if ("tokenBucket".equalsIgnoreCase(rateLimitType)) {
            return new TokenBucketRateLimiter(rateLimit);
        }
        return new FixedWindowRateLimiter(rateLimit);
    }

    private CircuitBreaker createCircuitBreaker(RpcFieldInfo info) {
        int circuitFailures = info.annotation.circuitFailureThreshold();
        long circuitOpen = info.annotation.circuitOpenMillis();

        if (circuitFailures == 3) {
            circuitFailures = properties.getClient().getCircuitFailureThreshold();
        }
        if (circuitOpen == 3000) {
            circuitOpen = properties.getClient().getCircuitOpenMillis();
        }

        String mode = properties.getClient().getCircuitBreakerMode();
        if ("failureRate".equalsIgnoreCase(mode)) {
            return CircuitBreaker.createFailureRateBreaker(circuitFailures, 0.5, circuitOpen);
        }
        return CircuitBreaker.createCountingBreaker(circuitFailures, circuitOpen);
    }

    private long getTimeout(RpcFieldInfo info) {
        long timeout = info.annotation.timeoutMillis();
        if (timeout == 3000) {
            timeout = properties.getClient().getTimeoutMillis();
        }
        return timeout;
    }

    private String buildCacheKey(Class<?> interfaceClass, String version, String group) {
        return interfaceClass.getName() + ":" + version + ":" + group;
    }

    /**
     * 获取已注册的类数量
     */
    public int getRegisteredClassCount() {
        return classIndex.size();
    }

    /**
     * 获取代理缓存大小
     */
    public int getProxyCacheSize() {
        return proxyCache.size();
    }

    /**
     * 清理缓存
     */
    public void clearCache() {
        proxyCache.clear();
        factoryCache.clear();
    }

    /**
     * RPC 字段信息
     */
    private static class RpcFieldInfo {
        final Field field;
        final RpcReference annotation;

        RpcFieldInfo(Field field, RpcReference annotation) {
            this.field = field;
            this.annotation = annotation;
        }
    }
}