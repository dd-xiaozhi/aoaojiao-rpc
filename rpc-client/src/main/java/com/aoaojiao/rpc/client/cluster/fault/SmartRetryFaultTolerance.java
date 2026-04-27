package com.aoaojiao.rpc.client.cluster.fault;

import com.aoaojiao.rpc.client.SimpleRpcClient;
import com.aoaojiao.rpc.client.cluster.LoadBalancer;
import com.aoaojiao.rpc.common.service.RpcInvocation;
import com.aoaojiao.rpc.common.service.ServiceKey;
import com.aoaojiao.rpc.registry.ServiceDiscovery;
import com.aoaojiao.rpc.registry.ServiceInstance;
import com.aoaojiao.rpc.metrics.MetricsHolder;
import com.aoaojiao.rpc.metrics.MetricsRegistry;

import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeoutException;

/**
 * 智能重试容错策略
 * 实现失败节点排除、指数退避和异常分类重试
 */
public class SmartRetryFaultTolerance implements FaultTolerance {

    private final int maxRetry;
    private final long baseIntervalMillis;
    private final long maxIntervalMillis;
    private final long jitterMillis;
    private final boolean excludeFailedNodes;
    private final MetricsRegistry metrics;

    // 线程安全的失败节点记录
    private final ConcurrentHashMap<String, Long> failedNodes = new ConcurrentHashMap<>();

    // 随机数生成器
    private final Random random = new Random();

    // 配置：哪些异常可以重试
    private final Set<Class<? extends Throwable>> retryableExceptions;

    public SmartRetryFaultTolerance(int maxRetry) {
        this(maxRetry, 100, 5000, 50, true);
    }

    public SmartRetryFaultTolerance(int maxRetry, long baseIntervalMillis,
                                    long maxIntervalMillis, long jitterMillis,
                                    boolean excludeFailedNodes) {
        if (maxRetry < 0 || maxRetry > 3) {
            throw new IllegalArgumentException("maxRetry must be between 0 and 3");
        }
        this.maxRetry = maxRetry;
        this.baseIntervalMillis = baseIntervalMillis;
        this.maxIntervalMillis = maxIntervalMillis;
        this.jitterMillis = jitterMillis;
        this.excludeFailedNodes = excludeFailedNodes;
        this.metrics = MetricsHolder.registry();

        // 初始化可重试异常类型
        this.retryableExceptions = new HashSet<>();
        this.retryableExceptions.add(SocketTimeoutException.class);
        this.retryableExceptions.add(ConnectException.class);
        this.retryableExceptions.add(TimeoutException.class);
        this.retryableExceptions.add(java.io.IOException.class);
        this.retryableExceptions.add(io.netty.channel.ConnectTimeoutException.class);
    }

    @Override
    public Object invoke(ServiceDiscovery discovery,
                         LoadBalancer loadBalancer,
                         ServiceKey key,
                         RpcInvocation invocation,
                         long timeoutMillis) throws Exception {

        Exception lastException = null;
        Set<ServiceInstance> failedSet = new HashSet<>();

        for (int attempt = 0; attempt <= maxRetry; attempt++) {
            // 获取可用实例列表
            List<ServiceInstance> instances = discovery.getInstances(key);
            if (instances == null || instances.isEmpty()) {
                throw new IllegalStateException("No provider available for: " + key);
            }

            // 排除失败的节点
            if (excludeFailedNodes && !failedSet.isEmpty()) {
                instances = filterFailedNodes(instances, failedSet);
                if (instances.isEmpty()) {
                    // 所有节点都失败了，清空失败记录重试
                    failedSet.clear();
                    instances = discovery.getInstances(key);
                }
            }

            ServiceInstance selected = loadBalancer.select(instances);
            if (selected == null) {
                throw new IllegalStateException("No available instance after filtering");
            }

            try {
                // 执行调用
                Object result = doInvoke(selected.getHost(), selected.getPort(), invocation, timeoutMillis);

                // 成功时清除该节点的失败记录
                clearFailedNode(selected);

                // 记录重试成功指标
                if (attempt > 0) {
                    metrics.increment("client.retry.success");
                }

                return result;
            } catch (Exception ex) {
                lastException = ex;

                // 记录失败节点
                failedSet.add(selected);
                markFailedNode(selected);

                // 检查异常是否可重试
                if (!isRetryable(ex)) {
                    // 不可重试的异常，立即失败
                    metrics.increment("client.retry.nonRetryable");
                    throw ex;
                }

                // 记录重试失败指标
                metrics.increment("client.retry.failed");

                // 如果不是最后一次尝试，等待后再重试
                if (attempt < maxRetry) {
                    long waitTime = calculateWaitTime(attempt);
                    try {
                        Thread.sleep(waitTime);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException("Retry interrupted", ie);
                    }
                }
            }
        }

        // 所有重试都失败了
        metrics.increment("client.retry.exhausted");
        throw lastException == null ? new IllegalStateException("Retry failed") : lastException;
    }

    /**
     * 执行 RPC 调用
     */
    private Object doInvoke(String host, int port, RpcInvocation invocation, long timeoutMillis) throws Exception {
        // 使用现有的 SimpleRpcClient
        SimpleRpcClient client = new SimpleRpcClient(host, port);
        try {
            client.start();
            return client.invoke(invocation, timeoutMillis);
        } finally {
            client.close();
        }
    }

    /**
     * 计算等待时间（指数退避 + 抖动）
     */
    private long calculateWaitTime(int attempt) {
        // 指数退避: baseInterval * 2^attempt
        long interval = baseIntervalMillis * (1L << attempt);

        // 不超过最大间隔
        interval = Math.min(interval, maxIntervalMillis);

        // 添加抖动
        if (jitterMillis > 0) {
            interval += random.nextLong() % jitterMillis;
        }

        // 确保非负
        return Math.max(0, interval);
    }

    /**
     * 检查异常是否可重试
     */
    private boolean isRetryable(Throwable ex) {
        if (ex == null) {
            return false;
        }

        // 检查异常类型是否可重试
        for (Class<? extends Throwable> clazz : retryableExceptions) {
            if (clazz.isInstance(ex)) {
                return true;
            }
        }

        // 检查异常消息是否包含重试关键字
        String message = ex.getMessage();
        if (message != null) {
            String lowerMessage = message.toLowerCase();
            if (lowerMessage.contains("timeout") ||
                lowerMessage.contains("connection") ||
                lowerMessage.contains("unavailable")) {
                return true;
            }
        }

        return false;
    }

    /**
     * 过滤掉失败的节点
     */
    private List<ServiceInstance> filterFailedNodes(List<ServiceInstance> instances,
                                                     Set<ServiceInstance> failedSet) {
        List<ServiceInstance> filtered = new ArrayList<>();
        long now = System.currentTimeMillis();

        for (ServiceInstance instance : instances) {
            String nodeKey = instance.getHost() + ":" + instance.getPort();
            Long failedTime = failedNodes.get(nodeKey);

            // 如果节点在隔离期外（5秒），可以再次使用
            if (failedTime == null || (now - failedTime) > 5000) {
                filtered.add(instance);
            }
        }

        return filtered;
    }

    /**
     * 标记节点失败
     */
    private void markFailedNode(ServiceInstance instance) {
        String nodeKey = instance.getHost() + ":" + instance.getPort();
        failedNodes.put(nodeKey, System.currentTimeMillis());
    }

    /**
     * 清除节点失败记录
     */
    private void clearFailedNode(ServiceInstance instance) {
        String nodeKey = instance.getHost() + ":" + instance.getPort();
        failedNodes.remove(nodeKey);
    }

    /**
     * 添加可重试异常类型
     */
    public void addRetryableException(Class<? extends Throwable> exceptionClass) {
        retryableExceptions.add(exceptionClass);
    }

    /**
     * 清除所有失败节点记录
     */
    public void clearFailedNodes() {
        failedNodes.clear();
    }

    /**
     * 获取当前失败节点数量
     */
    public int getFailedNodeCount() {
        return failedNodes.size();
    }

    /**
     * 获取失败节点键列表
     */
    public Set<String> getFailedNodeKeys() {
        return new HashSet<>(failedNodes.keySet());
    }
}