package com.aoaojiao.rpc.client.cluster.limit;

import java.util.concurrent.atomic.AtomicLong;

/**
 * 令牌桶限流器
 * 支持突发流量，初始令牌数等于桶容量
 */
public class TokenBucketRateLimiter implements RateLimiter {

    /**
     * 桶容量（最大令牌数）
     */
    private final long capacity;

    /**
     * 每秒补充令牌数
     */
    private final double refillRate;

    /**
     * 当前令牌数
     */
    private final AtomicLong tokens;

    /**
     * 最后补充令牌的时间（纳秒）
     */
    private final AtomicLong lastRefillTime;

    /**
     * 最大令牌数（同 capacity）
     */
    private final double maxTokens;

    public TokenBucketRateLimiter(long maxQps) {
        this(maxQps, maxQps, 1.0);
    }

    public TokenBucketRateLimiter(long maxQps, long burstCapacity) {
        this(maxQps, burstCapacity, 1.0);
    }

    public TokenBucketRateLimiter(long maxQps, long burstCapacity, double refillPerSecond) {
        if (maxQps <= 0) {
            throw new IllegalArgumentException("maxQps must be positive");
        }
        if (burstCapacity <= 0) {
            throw new IllegalArgumentException("burstCapacity must be positive");
        }
        if (refillPerSecond <= 0) {
            throw new IllegalArgumentException("refillPerSecond must be positive");
        }

        this.capacity = burstCapacity;
        this.refillRate = refillPerSecond;
        this.maxTokens = burstCapacity;
        this.tokens = new AtomicLong(burstCapacity); // 初始令牌数等于桶容量（支持突发）
        this.lastRefillTime = new AtomicLong(System.nanoTime());
    }

    @Override
    public boolean tryAcquire() {
        return tryAcquire(1);
    }

    @Override
    public boolean tryAcquire(int permits) {
        // 先补充令牌
        refill();

        // 尝试获取令牌
        while (true) {
            long current = tokens.get();
            if (current < permits) {
                // 令牌不足
                return false;
            }

            long newValue = current - permits;
            if (tokens.compareAndSet(current, newValue)) {
                // 成功获取令牌
                return true;
            }

            // CAS 失败，重试
        }
    }

    /**
     * 补充令牌
     */
    private void refill() {
        long now = System.currentTimeMillis();
        long lastTime = lastRefillTime.get();
        long elapsedMillis = now - lastTime;

        if (elapsedMillis <= 0) {
            return;
        }

        // 计算应该补充的令牌数
        double refillAmount = (elapsedMillis / 1000.0) * refillRate;

        // 只在有令牌需要补充时更新
        if (refillAmount >= 1) {
            while (true) {
                long current = tokens.get();
                long newTokens = Math.min((long) maxTokens, current + (long) refillAmount);

                if (tokens.compareAndSet(current, newTokens)) {
                    lastRefillTime.set(now);
                    break;
                }
            }
        }
    }

    /**
     * 获取当前令牌数
     */
    public long getAvailableTokens() {
        refill();
        return tokens.get();
    }

    /**
     * 获取桶容量
     */
    public long getCapacity() {
        return capacity;
    }

    /**
     * 获取每秒补充令牌数
     */
    public double getRefillRate() {
        return refillRate;
    }

    /**
     * 计算令牌补充量
     */
    private double calculateRefill(long elapsedNanos) {
        return (elapsedNanos / 1_000_000_000.0) * refillRate;
    }

    @Override
    public String toString() {
        return String.format("TokenBucketRateLimiter{capacity=%d, availableTokens=%d, refillRate=%.2f/s}",
                capacity, getAvailableTokens(), refillRate);
    }
}