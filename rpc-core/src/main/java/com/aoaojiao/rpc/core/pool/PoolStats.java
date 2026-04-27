package com.aoaojiao.rpc.core.pool;

import com.aoaojiao.rpc.core.transport.PendingRequests;

/**
 * 连接池统计信息
 * 用于监控连接池的运行状态
 */
public class PoolStats {

    private final String nodeKey;
    private final int totalConnections;
    private final int idleConnections;
    private final int activeConnections;
    private final int waitingRequests;
    private final long avgWaitTimeMillis;
    private final long maxWaitTimeMillis;
    private final long createdConnections;
    private final long destroyedConnections;
    private final long reusedConnections;
    private final long failedConnections;

    public PoolStats(String nodeKey,
                     int totalConnections,
                     int idleConnections,
                     int activeConnections,
                     int waitingRequests,
                     long avgWaitTimeMillis,
                     long maxWaitTimeMillis,
                     long createdConnections,
                     long destroyedConnections,
                     long reusedConnections,
                     long failedConnections) {
        this.nodeKey = nodeKey;
        this.totalConnections = totalConnections;
        this.idleConnections = idleConnections;
        this.activeConnections = activeConnections;
        this.waitingRequests = waitingRequests;
        this.avgWaitTimeMillis = avgWaitTimeMillis;
        this.maxWaitTimeMillis = maxWaitTimeMillis;
        this.createdConnections = createdConnections;
        this.destroyedConnections = destroyedConnections;
        this.reusedConnections = reusedConnections;
        this.failedConnections = failedConnections;
    }

    public String getNodeKey() {
        return nodeKey;
    }

    public int getTotalConnections() {
        return totalConnections;
    }

    public int getIdleConnections() {
        return idleConnections;
    }

    public int getActiveConnections() {
        return activeConnections;
    }

    public int getWaitingRequests() {
        return waitingRequests;
    }

    public long getAvgWaitTimeMillis() {
        return avgWaitTimeMillis;
    }

    public long getMaxWaitTimeMillis() {
        return maxWaitTimeMillis;
    }

    public long getCreatedConnections() {
        return createdConnections;
    }

    public long getDestroyedConnections() {
        return destroyedConnections;
    }

    public long getReusedConnections() {
        return reusedConnections;
    }

    public long getFailedConnections() {
        return failedConnections;
    }

    /**
     * 获取连接利用率
     */
    public double getUtilizationRate() {
        if (totalConnections == 0) {
            return 0.0;
        }
        return (double) activeConnections / totalConnections;
    }

    /**
     * 获取复用率
     */
    public double getReuseRate() {
        long total = createdConnections - destroyedConnections;
        if (total <= 0) {
            return 0.0;
        }
        return (double) reusedConnections / total;
    }

    @Override
    public String toString() {
        return String.format(
            "PoolStats{nodeKey='%s', total=%d, idle=%d, active=%d, waiting=%d, " +
            "avgWait=%dms, maxWait=%dms, created=%d, destroyed=%d, reused=%d, failed=%d}",
            nodeKey, totalConnections, idleConnections, activeConnections, waitingRequests,
            avgWaitTimeMillis, maxWaitTimeMillis, createdConnections, destroyedConnections,
            reusedConnections, failedConnections
        );
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String nodeKey;
        private int totalConnections;
        private int idleConnections;
        private int activeConnections;
        private int waitingRequests;
        private long avgWaitTimeMillis;
        private long maxWaitTimeMillis;
        private long createdConnections;
        private long destroyedConnections;
        private long reusedConnections;
        private long failedConnections;

        public Builder nodeKey(String nodeKey) {
            this.nodeKey = nodeKey;
            return this;
        }

        public Builder totalConnections(int totalConnections) {
            this.totalConnections = totalConnections;
            return this;
        }

        public Builder idleConnections(int idleConnections) {
            this.idleConnections = idleConnections;
            return this;
        }

        public Builder activeConnections(int activeConnections) {
            this.activeConnections = activeConnections;
            return this;
        }

        public Builder waitingRequests(int waitingRequests) {
            this.waitingRequests = waitingRequests;
            return this;
        }

        public Builder avgWaitTimeMillis(long avgWaitTimeMillis) {
            this.avgWaitTimeMillis = avgWaitTimeMillis;
            return this;
        }

        public Builder maxWaitTimeMillis(long maxWaitTimeMillis) {
            this.maxWaitTimeMillis = maxWaitTimeMillis;
            return this;
        }

        public Builder createdConnections(long createdConnections) {
            this.createdConnections = createdConnections;
            return this;
        }

        public Builder destroyedConnections(long destroyedConnections) {
            this.destroyedConnections = destroyedConnections;
            return this;
        }

        public Builder reusedConnections(long reusedConnections) {
            this.reusedConnections = reusedConnections;
            return this;
        }

        public Builder failedConnections(long failedConnections) {
            this.failedConnections = failedConnections;
            return this;
        }

        public PoolStats build() {
            return new PoolStats(
                nodeKey, totalConnections, idleConnections, activeConnections,
                waitingRequests, avgWaitTimeMillis, maxWaitTimeMillis,
                createdConnections, destroyedConnections, reusedConnections, failedConnections
            );
        }
    }
}