package com.aoaojiao.rpc.core.pool;

import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 默认连接池管理器实现
 * 管理到所有服务端节点的连接池
 */
public class DefaultConnectionPoolManager implements ConnectionPoolManager {

    private final PoolConfig config;
    private final boolean enabled;
    private final EventLoopGroup sharedGroup;

    // 节点连接池映射: nodeKey -> PerNodeConnectionPool
    private final ConcurrentHashMap<String, PerNodeConnectionPool> pools = new ConcurrentHashMap<>();

    // 总统计
    private final AtomicLong totalCreatedConnections = new AtomicLong(0);
    private final AtomicLong totalDestroyedConnections = new AtomicLong(0);
    private final AtomicLong totalReusedConnections = new AtomicLong(0);
    private final AtomicLong totalFailedConnections = new AtomicLong(0);

    public DefaultConnectionPoolManager() {
        this(new PoolConfig());
    }

    public DefaultConnectionPoolManager(PoolConfig config) {
        this.config = config;
        this.enabled = config.isEnabled();

        // 创建共享的 EventLoopGroup
        if (enabled) {
            this.sharedGroup = new NioEventLoopGroup(Runtime.getRuntime().availableProcessors() * 2);
        } else {
            this.sharedGroup = null;
        }
    }

    @Override
    public RpcConnection getConnection(String host, int port) throws Exception {
        return getConnection(host, port, config.getAcquireTimeoutMillis());
    }

    @Override
    public RpcConnection getConnection(String host, int port, long timeoutMillis) throws Exception {
        if (!enabled) {
            // 如果连接池未启用，创建临时连接（兼容原有逻辑）
            return createTemporaryConnection(host, port);
        }

        String nodeKey = getNodeKey(host, port);
        PerNodeConnectionPool pool = getOrCreatePool(nodeKey, host, port);
        return pool.acquire(timeoutMillis);
    }

    @Override
    public void returnConnection(String host, int port, RpcConnection connection) {
        if (!enabled || connection == null) {
            return;
        }

        String nodeKey = getNodeKey(host, port);
        PerNodeConnectionPool pool = pools.get(nodeKey);
        if (pool != null) {
            pool.returnConnection(connection);
        }
    }

    @Override
    public void closeNodeConnections(String host, int port) {
        String nodeKey = getNodeKey(host, port);
        PerNodeConnectionPool pool = pools.remove(nodeKey);
        if (pool != null) {
            pool.shutdown();
        }
    }

    @Override
    public void markConnectionUnusable(String host, int port, RpcConnection connection) {
        if (!(connection instanceof ReusableRpcConnection)) {
            return;
        }

        String nodeKey = getNodeKey(host, port);
        PerNodeConnectionPool pool = pools.get(nodeKey);
        if (pool != null) {
            pool.markConnectionUnusable((ReusableRpcConnection) connection);
        }
    }

    @Override
    public PoolStats getStats(String host, int port) {
        if (!enabled) {
            return PoolStats.builder()
                    .nodeKey(getNodeKey(host, port))
                    .totalConnections(0)
                    .idleConnections(0)
                    .activeConnections(0)
                    .waitingRequests(0)
                    .build();
        }

        String nodeKey = getNodeKey(host, port);
        PerNodeConnectionPool pool = pools.get(nodeKey);
        if (pool != null) {
            return pool.getStats();
        }
        return PoolStats.builder()
                .nodeKey(nodeKey)
                .totalConnections(0)
                .idleConnections(0)
                .activeConnections(0)
                .waitingRequests(0)
                .build();
    }

    @Override
    public PoolStats getTotalStats() {
        if (!enabled) {
            return PoolStats.builder()
                    .nodeKey("all")
                    .totalConnections(0)
                    .idleConnections(0)
                    .activeConnections(0)
                    .waitingRequests(0)
                    .build();
        }

        int totalIdle = 0;
        int totalActive = 0;
        long created = 0;
        long destroyed = 0;
        long reused = 0;
        long failed = 0;

        for (PerNodeConnectionPool pool : pools.values()) {
            PoolStats stats = pool.getStats();
            totalIdle += stats.getIdleConnections();
            totalActive += stats.getActiveConnections();
            created += stats.getCreatedConnections();
            destroyed += stats.getDestroyedConnections();
            reused += stats.getReusedConnections();
            failed += stats.getFailedConnections();
        }

        return PoolStats.builder()
                .nodeKey("all")
                .totalConnections(totalIdle + totalActive)
                .idleConnections(totalIdle)
                .activeConnections(totalActive)
                .waitingRequests(0)
                .createdConnections(created)
                .destroyedConnections(destroyed)
                .reusedConnections(reused)
                .failedConnections(failed)
                .build();
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public void warmUp() {
        if (!enabled) {
            return;
        }

        // 预热连接池：为每个已有连接池创建最小空闲连接
        for (PerNodeConnectionPool pool : pools.values()) {
            try {
                // 触发最小空闲连接创建
                pool.getStats();
            } catch (Exception e) {
                // 忽略预热失败
            }
        }
    }

    @Override
    public void close() {
        // 关闭所有节点连接池
        for (PerNodeConnectionPool pool : pools.values()) {
            try {
                pool.shutdown();
            } catch (Exception e) {
                // 忽略关闭异常
            }
        }
        pools.clear();

        // 关闭共享 EventLoopGroup
        if (sharedGroup != null) {
            sharedGroup.shutdownGracefully();
        }
    }

    /**
     * 获取或创建指定节点的连接池
     */
    private PerNodeConnectionPool getOrCreatePool(String nodeKey, String host, int port) {
        PerNodeConnectionPool pool = pools.get(nodeKey);
        if (pool != null) {
            return pool;
        }

        // 创建新的连接池
        PerNodeConnectionPool newPool = new PerNodeConnectionPool(
                host, port, config,
                () -> sharedGroup
        );

        // 尝试放入，如果已存在则使用已有的
        PerNodeConnectionPool existing = pools.putIfAbsent(nodeKey, newPool);
        return existing != null ? existing : newPool;
    }

    /**
     * 创建临时连接（连接池未启用时使用）
     */
    private RpcConnection createTemporaryConnection(String host, int port) throws Exception {
        ReusableRpcConnection connection = new ReusableRpcConnection(
                host, port, config, sharedGroup, null);
        connection.connect(config.getConnectTimeoutMillis());
        return connection;
    }

    /**
     * 获取连接池数量
     */
    public int getPoolCount() {
        return pools.size();
    }

    /**
     * 检查指定节点是否有连接池
     */
    public boolean hasPool(String host, int port) {
        return pools.containsKey(getNodeKey(host, port));
    }
}