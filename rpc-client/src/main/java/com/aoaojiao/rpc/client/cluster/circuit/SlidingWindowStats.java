package com.aoaojiao.rpc.client.cluster.circuit;

import java.util.concurrent.atomic.AtomicLong;

/**
 * 滑动窗口统计
 * 用于熔断器的失败率统计
 */
public class SlidingWindowStats {

    private final int windowSizeSeconds;
    private final int bucketCount;
    private final Bucket[] buckets;

    // 当前桶索引
    private final AtomicLong currentBucketIndex = new AtomicLong(0);

    public SlidingWindowStats() {
        this(60, 60); // 默认 60 秒窗口，每秒一个桶
    }

    public SlidingWindowStats(int windowSizeSeconds, int bucketCount) {
        if (windowSizeSeconds <= 0) {
            throw new IllegalArgumentException("windowSizeSeconds must be positive");
        }
        if (bucketCount <= 0) {
            throw new IllegalArgumentException("bucketCount must be positive");
        }

        this.windowSizeSeconds = windowSizeSeconds;
        this.bucketCount = bucketCount;
        this.buckets = new Bucket[bucketCount];

        long now = System.currentTimeMillis();
        for (int i = 0; i < bucketCount; i++) {
            buckets[i] = new Bucket(now / 1000);
        }
    }

    /**
     * 记录请求成功
     */
    public void recordSuccess() {
        int bucketIndex = getCurrentBucketIndex();
        buckets[bucketIndex].successes.incrementAndGet();
        buckets[bucketIndex].total.incrementAndGet();
        buckets[bucketIndex].timestamp.set(System.currentTimeMillis() / 1000);
    }

    /**
     * 记录请求失败
     */
    public void recordFailure() {
        int bucketIndex = getCurrentBucketIndex();
        buckets[bucketIndex].failures.incrementAndGet();
        buckets[bucketIndex].total.incrementAndGet();
        buckets[bucketIndex].timestamp.set(System.currentTimeMillis() / 1000);
    }

    /**
     * 获取滑动窗口内的总请求数
     */
    public long getTotalRequests() {
        cleanOldBuckets();
        long total = 0;
        for (Bucket bucket : buckets) {
            total += bucket.total.get();
        }
        return total;
    }

    /**
     * 获取滑动窗口内的失败数
     */
    public long getFailedRequests() {
        cleanOldBuckets();
        long failed = 0;
        for (Bucket bucket : buckets) {
            failed += bucket.failures.get();
        }
        return failed;
    }

    /**
     * 获取失败率
     */
    public double getFailureRate() {
        long total = getTotalRequests();
        if (total == 0) {
            return 0.0;
        }
        long failed = getFailedRequests();
        return (double) failed / total;
    }

    /**
     * 获取成功率
     */
    public double getSuccessRate() {
        return 1.0 - getFailureRate();
    }

    /**
     * 清空统计数据
     */
    public void reset() {
        for (Bucket bucket : buckets) {
            bucket.total.set(0);
            bucket.successes.set(0);
            bucket.failures.set(0);
        }
    }

    /**
     * 获取当前桶索引
     */
    private int getCurrentBucketIndex() {
        long currentSecond = System.currentTimeMillis() / 1000;
        long bucketIndex = currentSecond % bucketCount;

        // 检查是否需要轮转桶
        long oldIndex = currentBucketIndex.get();
        if (oldIndex != bucketIndex) {
            if (currentBucketIndex.compareAndSet(oldIndex, bucketIndex)) {
                // 重置新桶
                buckets[(int) bucketIndex].total.set(0);
                buckets[(int) bucketIndex].successes.set(0);
                buckets[(int) bucketIndex].failures.set(0);
            }
        }

        return (int) bucketIndex;
    }

    /**
     * 清理过期的桶
     */
    private void cleanOldBuckets() {
        long currentSecond = System.currentTimeMillis() / 1000;
        int currentIndex = (int) (currentSecond % bucketCount);

        // 只保留最近 windowSizeSeconds 秒内的数据
        for (int i = 0; i < bucketCount; i++) {
            long bucketSecond = buckets[i].timestamp.get();
            if (currentSecond - bucketSecond > windowSizeSeconds) {
                // 数据过期，重置
                buckets[i].total.set(0);
                buckets[i].successes.set(0);
                buckets[i].failures.set(0);
            }
        }
    }

    @Override
    public String toString() {
        return String.format("SlidingWindowStats{total=%d, failed=%d, failureRate=%.2f%%}",
                getTotalRequests(), getFailedRequests(), getFailureRate() * 100);
    }

    /**
     * 内部类：桶
     */
    private static class Bucket {
        final AtomicLong total;
        final AtomicLong successes;
        final AtomicLong failures;
        final AtomicLong timestamp;

        Bucket(long timestamp) {
            this.total = new AtomicLong(0);
            this.successes = new AtomicLong(0);
            this.failures = new AtomicLong(0);
            this.timestamp = new AtomicLong(timestamp);
        }
    }
}