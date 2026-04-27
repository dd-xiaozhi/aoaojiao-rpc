package com.aoaojiao.rpc.client.cluster.limit;

import java.util.concurrent.atomic.AtomicLong;

/**
 * 滑动窗口限流器
 * 消除固定窗口算法的临界突刺问题
 *
 * 算法原理:
 * 1. 将时间窗口划分为多个小桶
 * 2. 统计当前时刻往前一个完整窗口内的请求数
 * 3. 超出阈值则拒绝请求
 */
public class SlidingWindowRateLimiter implements RateLimiter {

    private final long windowSizeMillis;
    private final int bucketCount;
    private final long maxRequests;
    private final Bucket[] buckets;

    // 当前桶索引
    private final AtomicLong currentBucketIndex = new AtomicLong(0);

    public SlidingWindowRateLimiter(long maxQps) {
        this(1000, Math.max(10, (int) (maxQps / 10)), maxQps);
    }

    public SlidingWindowRateLimiter(long windowSizeMillis, int bucketCount, long maxRequests) {
        if (windowSizeMillis <= 0) {
            throw new IllegalArgumentException("windowSizeMillis must be positive");
        }
        if (bucketCount <= 0) {
            throw new IllegalArgumentException("bucketCount must be positive");
        }
        if (maxRequests <= 0) {
            throw new IllegalArgumentException("maxRequests must be positive");
        }

        this.windowSizeMillis = windowSizeMillis;
        this.bucketCount = bucketCount;
        this.maxRequests = maxRequests;
        this.buckets = new Bucket[bucketCount];

        // 初始化桶
        long now = System.currentTimeMillis();
        for (int i = 0; i < bucketCount; i++) {
            buckets[i] = new Bucket(now);
        }
    }

    @Override
    public boolean tryAcquire() {
        return tryAcquire(1);
    }

    @Override
    public boolean tryAcquire(int permits) {
        long now = System.currentTimeMillis();
        long currentBucket = now / (windowSizeMillis / bucketCount);

        // 清理过期桶
        cleanExpiredBuckets(now, currentBucket);

        // 计算滑动窗口内的总请求数
        long totalCount = 0;
        for (Bucket bucket : buckets) {
            totalCount += bucket.count.get();
        }

        // 检查是否可以接受请求
        if (totalCount + permits <= maxRequests) {
            // 获取当前桶并增加计数
            int bucketIndex = (int) (currentBucket % bucketCount);
            buckets[bucketIndex].count.addAndGet(permits);
            buckets[bucketIndex].timestamp.set(now);
            return true;
        }

        return false;
    }

    /**
     * 清理过期的桶
     */
    private void cleanExpiredBuckets(long now, long currentBucket) {
        for (int i = 0; i < bucketCount; i++) {
            long bucketTime = buckets[i].timestamp.get();
            long bucketIndex = bucketTime / (windowSizeMillis / bucketCount);

            // 如果桶的时间戳太旧，重置计数
            if (currentBucket - bucketIndex > 1) {
                buckets[i].count.set(0);
            }
        }
    }

    /**
     * 获取当前窗口内的请求数
     */
    public long getCurrentCount() {
        long now = System.currentTimeMillis();
        long currentBucket = now / (windowSizeMillis / bucketCount);

        long totalCount = 0;
        for (Bucket bucket : buckets) {
            long bucketTime = bucket.timestamp.get();
            long bucketIndex = bucketTime / (windowSizeMillis / bucketCount);

            // 只统计当前窗口内的桶
            if (currentBucket - bucketIndex <= 1) {
                totalCount += bucket.count.get();
            }
        }
        return totalCount;
    }

    /**
     * 获取QPS（每秒请求数）
     */
    public double getCurrentQps() {
        return (double) getCurrentCount() * 1000 / windowSizeMillis;
    }

    /**
     * 获取最大QPS
     */
    public long getMaxQps() {
        return maxRequests;
    }

    /**
     * 内部类：桶
     */
    private static class Bucket {
        final AtomicLong count;
        final AtomicLong timestamp;

        Bucket(long timestamp) {
            this.count = new AtomicLong(0);
            this.timestamp = new AtomicLong(timestamp);
        }
    }

    @Override
    public String toString() {
        return String.format("SlidingWindowRateLimiter{maxRequests=%d, currentCount=%d, qps=%.2f}",
                maxRequests, getCurrentCount(), getCurrentQps());
    }
}