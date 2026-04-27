package com.aoaojiao.rpc.core.pool;

/**
 * 连接池配置类
 * 用于配置连接池的各项参数
 */
public class PoolConfig {

    /**
     * 最大连接数
     * 每个节点允许的最大连接数
     */
    private int maxConnections = 8;

    /**
     * 最小空闲连接数
     * 连接池保持的最小空闲连接数
     */
    private int minIdle = 2;

    /**
     * 空闲连接超时时间（毫秒）
     * 超过此时间未使用的空闲连接将被关闭
     */
    private long idleTimeoutMillis = 60000;

    /**
     * 连接获取超时时间（毫秒）
     * 从连接池获取连接的最大等待时间
     */
    private long acquireTimeoutMillis = 5000;

    /**
     * 心跳间隔时间（毫秒）
     * 定期发送心跳检测连接活性
     */
    private long heartbeatIntervalMillis = 30000;

    /**
     * 最大心跳丢失次数
     * 超过此次数未收到心跳响应则判定连接不可用
     */
    private int maxHeartbeatMisses = 3;

    /**
     * 连接池总开关
     * 设置为 false 时将使用传统的每次新建连接方式
     */
    private boolean enabled = false;

    /**
     * 连接创建超时时间（毫秒）
     * 建立 TCP 连接的最大超时时间
     */
    private long connectTimeoutMillis = 5000;

    /**
     * 清理器运行间隔（毫秒）
     * 后台清理线程扫描过期连接的间隔
     */
    private long cleanerIntervalMillis = 5000;

    public PoolConfig() {
    }

    public PoolConfig(boolean enabled) {
        this.enabled = enabled;
    }

    public int getMaxConnections() {
        return maxConnections;
    }

    public void setMaxConnections(int maxConnections) {
        this.maxConnections = maxConnections;
    }

    public int getMinIdle() {
        return minIdle;
    }

    public void setMinIdle(int minIdle) {
        this.minIdle = minIdle;
    }

    public long getIdleTimeoutMillis() {
        return idleTimeoutMillis;
    }

    public void setIdleTimeoutMillis(long idleTimeoutMillis) {
        this.idleTimeoutMillis = idleTimeoutMillis;
    }

    public long getAcquireTimeoutMillis() {
        return acquireTimeoutMillis;
    }

    public void setAcquireTimeoutMillis(long acquireTimeoutMillis) {
        this.acquireTimeoutMillis = acquireTimeoutMillis;
    }

    public long getHeartbeatIntervalMillis() {
        return heartbeatIntervalMillis;
    }

    public void setHeartbeatIntervalMillis(long heartbeatIntervalMillis) {
        this.heartbeatIntervalMillis = heartbeatIntervalMillis;
    }

    public int getMaxHeartbeatMisses() {
        return maxHeartbeatMisses;
    }

    public void setMaxHeartbeatMisses(int maxHeartbeatMisses) {
        this.maxHeartbeatMisses = maxHeartbeatMisses;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public long getConnectTimeoutMillis() {
        return connectTimeoutMillis;
    }

    public void setConnectTimeoutMillis(long connectTimeoutMillis) {
        this.connectTimeoutMillis = connectTimeoutMillis;
    }

    public long getCleanerIntervalMillis() {
        return cleanerIntervalMillis;
    }

    public void setCleanerIntervalMillis(long cleanerIntervalMillis) {
        this.cleanerIntervalMillis = cleanerIntervalMillis;
    }

    /**
     * 验证配置的有效性
     * @throws IllegalArgumentException 如果配置无效
     */
    public void validate() {
        if (maxConnections < 1) {
            throw new IllegalArgumentException("maxConnections must be >= 1");
        }
        if (minIdle < 0) {
            throw new IllegalArgumentException("minIdle must be >= 0");
        }
        if (minIdle > maxConnections) {
            throw new IllegalArgumentException("minIdle must be <= maxConnections");
        }
        if (idleTimeoutMillis < 1000) {
            throw new IllegalArgumentException("idleTimeoutMillis must be >= 1000");
        }
        if (acquireTimeoutMillis < 100) {
            throw new IllegalArgumentException("acquireTimeoutMillis must be >= 100");
        }
        if (heartbeatIntervalMillis < 1000) {
            throw new IllegalArgumentException("heartbeatIntervalMillis must be >= 1000");
        }
        if (maxHeartbeatMisses < 1) {
            throw new IllegalArgumentException("maxHeartbeatMisses must be >= 1");
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private final PoolConfig config = new PoolConfig();

        public Builder maxConnections(int maxConnections) {
            config.setMaxConnections(maxConnections);
            return this;
        }

        public Builder minIdle(int minIdle) {
            config.setMinIdle(minIdle);
            return this;
        }

        public Builder idleTimeoutMillis(long idleTimeoutMillis) {
            config.setIdleTimeoutMillis(idleTimeoutMillis);
            return this;
        }

        public Builder acquireTimeoutMillis(long acquireTimeoutMillis) {
            config.setAcquireTimeoutMillis(acquireTimeoutMillis);
            return this;
        }

        public Builder heartbeatIntervalMillis(long heartbeatIntervalMillis) {
            config.setHeartbeatIntervalMillis(heartbeatIntervalMillis);
            return this;
        }

        public Builder maxHeartbeatMisses(int maxHeartbeatMisses) {
            config.setMaxHeartbeatMisses(maxHeartbeatMisses);
            return this;
        }

        public Builder enabled(boolean enabled) {
            config.setEnabled(enabled);
            return this;
        }

        public Builder connectTimeoutMillis(long connectTimeoutMillis) {
            config.setConnectTimeoutMillis(connectTimeoutMillis);
            return this;
        }

        public Builder cleanerIntervalMillis(long cleanerIntervalMillis) {
            config.setCleanerIntervalMillis(cleanerIntervalMillis);
            return this;
        }

        public PoolConfig build() {
            return config;
        }
    }
}