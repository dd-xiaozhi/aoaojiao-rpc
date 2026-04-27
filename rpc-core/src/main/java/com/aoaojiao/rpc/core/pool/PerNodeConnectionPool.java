package com.aoaojiao.rpc.core.pool;

import com.aoaojiao.rpc.core.pool.PoolStats;
import com.aoaojiao.rpc.core.pool.PoolConfig;
import com.aoaojiao.rpc.core.pool.ReusableRpcConnection;
import com.aoaojiao.rpc.core.pool.RpcConnection;
import io.netty.channel.EventLoopGroup;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;

/**
 * 单节点连接池
 * 管理到特定节点的多个可复用连接
 */
public class PerNodeConnectionPool {

    private final String host;
    private final int port;
    private final String nodeKey;
    private final PoolConfig config;
    private final Supplier<EventLoopGroup> groupSupplier;

    // 空闲连接队列
    private final ConcurrentHashMap<ReusableRpcConnection, Boolean> idleConnections = new ConcurrentHashMap<>();

    // 活跃连接集合
    private final ConcurrentHashMap<ReusableRpcConnection, Boolean> activeConnections = new ConcurrentHashMap<>();

    // 连接创建计数
    private final AtomicLong createdCount = new AtomicLong(0);
    private final AtomicLong destroyedCount = new AtomicLong(0);
    private final AtomicLong reusedCount = new AtomicLong(0);
    private final AtomicLong failedCount = new AtomicLong(0);

    // 等待获取连接的队列
    private final ConcurrentHashMap<WaitingRequest, Boolean> waitingRequests = new ConcurrentHashMap<>();

    // 统计信息
    private final AtomicLong totalWaitTime = new AtomicLong(0);
    private final AtomicLong maxWaitTime = new AtomicLong(0);
    private final AtomicInteger waitCount = new AtomicInteger(0);

    // 连接池锁（用于创建连接等需要同步的操作）
    private final ReentrantLock poolLock = new ReentrantLock();

    // 后台清理线程
    private volatile boolean cleanerRunning = true;
    private Thread cleanerThread;

    public PerNodeConnectionPool(String host, int port, PoolConfig config,
                                  Supplier<EventLoopGroup> groupSupplier) {
        this.host = host;
        this.port = port;
        this.nodeKey = host + ":" + port;
        this.config = config;
        this.groupSupplier = groupSupplier;

        // 启动后台清理线程
        startCleaner();
    }

    /**
     * 获取连接
     */
    public RpcConnection acquire(long timeoutMillis) throws Exception {
        long startTime = System.currentTimeMillis();

        while (System.currentTimeMillis() - startTime < timeoutMillis) {
            // 1. 先尝试从空闲连接中获取
            RpcConnection connection = pollIdleConnection();
            if (connection != null) {
                if (connection.isConnected() && !connection.isClosing()) {
                    reusedCount.incrementAndGet();
                    recordWaitTime(startTime);
                    return connection;
                } else {
                    // 连接已断开，从空闲列表移除
                    removeConnection(connection);
                    destroyConnection(connection);
                }
            }

            // 2. 检查是否可以创建新连接
            int activeCount = activeConnections.size();
            int idleCount = idleConnections.size();
            int total = activeCount + idleCount;

            if (total < config.getMaxConnections()) {
                // 可以创建新连接
                poolLock.lock();
                try {
                    // 再次检查（双重检查锁定）
                    if (activeConnections.size() < config.getMaxConnections()) {
                        RpcConnection newConnection = createConnection();
                        activeConnections.put((ReusableRpcConnection) newConnection, true);
                        createdCount.incrementAndGet();
                        recordWaitTime(startTime);
                        return newConnection;
                    }
                } finally {
                    poolLock.unlock();
                }
            }

            // 3. 等待空闲连接
            long remainingTime = timeoutMillis - (System.currentTimeMillis() - startTime);
            if (remainingTime <= 0) {
                break;
            }

            // 让出 CPU 一小段时间
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Interrupted while waiting for connection", e);
            }
        }

        throw new TimeoutException("Acquiring connection from " + nodeKey + " timeout");
    }

    /**
     * 归还连接到连接池
     */
    public void returnConnection(RpcConnection connection) {
        if (!(connection instanceof ReusableRpcConnection)) {
            return;
        }

        ReusableRpcConnection rpcConnection = (ReusableRpcConnection) connection;

        // 检查连接是否可用
        if (!rpcConnection.isConnected() || rpcConnection.isClosing() || rpcConnection.isUnusable()) {
            removeConnection(rpcConnection);
            destroyConnection(rpcConnection);
            return;
        }

        // 从活跃连接移除
        activeConnections.remove(rpcConnection);

        // 如果超过最大连接数，销毁连接
        if (idleConnections.size() >= config.getMinIdle()) {
            destroyConnection(rpcConnection);
            return;
        }

        // 添加到空闲连接
        idleConnections.put(rpcConnection, true);

        // 通知等待的请求
        notifyWaitingRequests();
    }

    /**
     * 创建新连接
     */
    private RpcConnection createConnection() throws Exception {
        ReusableRpcConnection connection = new ReusableRpcConnection(
                host, port, config, groupSupplier.get(), this);
        connection.connect(config.getConnectTimeoutMillis());
        return connection;
    }

    /**
     * 从空闲连接队列中获取连接
     */
    private RpcConnection pollIdleConnection() {
        // 遍历获取一个可用连接
        for (Map.Entry<ReusableRpcConnection, Boolean> entry : idleConnections.entrySet()) {
            ReusableRpcConnection connection = entry.getKey();
            if (idleConnections.remove(connection, Boolean.TRUE)) {
                // 将其加入活跃连接
                activeConnections.put(connection, true);
                return connection;
            }
        }
        return null;
    }

    /**
     * 销毁连接
     */
    private void destroyConnection(RpcConnection connection) {
        try {
            connection.close();
            destroyedCount.incrementAndGet();
        } catch (Exception e) {
            // 忽略关闭异常
        }
    }

    /**
     * 移除连接
     */
    private void removeConnection(RpcConnection connection) {
        idleConnections.remove(connection);
        activeConnections.remove(connection);
    }

    /**
     * 连接断开时的回调
     */
    void onConnectionClosed(ReusableRpcConnection connection) {
        removeConnection(connection);
        destroyedCount.incrementAndGet();
        // 尝试创建新连接补充
        ensureMinIdle();
    }

    /**
     * 标记连接不可用
     */
    void markConnectionUnusable(ReusableRpcConnection connection) {
        removeConnection(connection);
        destroyedCount.incrementAndGet();
    }

    /**
     * 通知等待的请求
     */
    private void notifyWaitingRequests() {
        // 简化实现：只通知一个
        for (Map.Entry<WaitingRequest, Boolean> entry : waitingRequests.entrySet()) {
            if (waitingRequests.remove(entry.getKey(), Boolean.TRUE)) {
                entry.getKey().countDown();
                break;
            }
        }
    }

    /**
     * 确保最小空闲连接数
     */
    private void ensureMinIdle() {
        if (idleConnections.size() < config.getMinIdle()) {
            poolLock.lock();
            try {
                while (idleConnections.size() < config.getMinIdle()) {
                    try {
                        RpcConnection connection = createConnection();
                        idleConnections.put((ReusableRpcConnection) connection, true);
                        createdCount.incrementAndGet();
                    } catch (Exception e) {
                        failedCount.incrementAndGet();
                        break;
                    }
                }
            } finally {
                poolLock.unlock();
            }
        }
    }

    /**
     * 记录等待时间
     */
    private void recordWaitTime(long startTime) {
        long waitTime = System.currentTimeMillis() - startTime;
        totalWaitTime.addAndGet(waitTime);
        waitCount.incrementAndGet();

        // 更新最大等待时间
        long currentMax = maxWaitTime.get();
        while (waitTime > currentMax) {
            if (maxWaitTime.compareAndSet(currentMax, waitTime)) {
                break;
            }
            currentMax = maxWaitTime.get();
        }
    }

    /**
     * 启动后台清理线程
     */
    private void startCleaner() {
        cleanerThread = new Thread(() -> {
            while (cleanerRunning) {
                try {
                    Thread.sleep(config.getCleanerIntervalMillis());
                    cleanIdleConnections();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                } catch (Exception e) {
                    // 忽略异常，继续执行
                }
            }
        }, "connection-pool-cleaner-" + nodeKey);
        cleanerThread.setDaemon(true);
        cleanerThread.start();
    }

    /**
     * 清理空闲连接
     */
    private void cleanIdleConnections() {
        long now = System.currentTimeMillis();
        long idleTimeout = config.getIdleTimeoutMillis();

        Iterator<Map.Entry<ReusableRpcConnection, Boolean>> iterator = idleConnections.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<ReusableRpcConnection, Boolean> entry = iterator.next();
            ReusableRpcConnection connection = entry.getKey();

            // 检查是否超时
            if (now - connection.getLastActiveTime() > idleTimeout) {
                if (idleConnections.remove(connection, Boolean.TRUE)) {
                    destroyConnection(connection);
                }
            }
        }

        // 确保最小空闲连接数
        ensureMinIdle();
    }

    /**
     * 获取统计信息
     */
    public PoolStats getStats() {
        long avgWaitTime = waitCount.get() > 0 ? totalWaitTime.get() / waitCount.get() : 0;

        return PoolStats.builder()
                .nodeKey(nodeKey)
                .totalConnections(activeConnections.size() + idleConnections.size())
                .idleConnections(idleConnections.size())
                .activeConnections(activeConnections.size())
                .waitingRequests(waitingRequests.size())
                .avgWaitTimeMillis(avgWaitTime)
                .maxWaitTimeMillis(maxWaitTime.get())
                .createdConnections(createdCount.get())
                .destroyedConnections(destroyedCount.get())
                .reusedConnections(reusedCount.get())
                .failedConnections(failedCount.get())
                .build();
    }

    /**
     * 关闭连接池
     */
    public void shutdown() {
        cleanerRunning = false;

        // 关闭所有空闲连接
        for (ReusableRpcConnection connection : idleConnections.keySet()) {
            try {
                connection.close();
            } catch (Exception e) {
                // 忽略
            }
        }
        idleConnections.clear();

        // 关闭所有活跃连接
        for (ReusableRpcConnection connection : activeConnections.keySet()) {
            try {
                connection.close();
            } catch (Exception e) {
                // 忽略
            }
        }
        activeConnections.clear();

        // 中断等待的请求
        for (WaitingRequest request : waitingRequests.keySet()) {
            request.countDown();
        }
        waitingRequests.clear();
    }

    /**
     * 等待请求包装器
     */
    private static class WaitingRequest {
        final long deadline;
        final java.util.concurrent.CountDownLatch latch = new java.util.concurrent.CountDownLatch(1);

        WaitingRequest(long timeoutMillis) {
            this.deadline = System.currentTimeMillis() + timeoutMillis;
        }

        void countDown() {
            latch.countDown();
        }

        boolean isTimeout() {
            return System.currentTimeMillis() > deadline;
        }
    }

    // ========== Getter ==========

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public String getNodeKey() {
        return nodeKey;
    }

    public int getIdleCount() {
        return idleConnections.size();
    }

    public int getActiveCount() {
        return activeConnections.size();
    }
}