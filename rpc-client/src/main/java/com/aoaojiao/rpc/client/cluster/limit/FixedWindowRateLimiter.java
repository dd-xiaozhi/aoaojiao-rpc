package com.aoaojiao.rpc.client.cluster.limit;

import java.util.concurrent.atomic.AtomicLong;

public class FixedWindowRateLimiter implements RateLimiter {
    private final long maxQps;
    private final AtomicLong windowStart = new AtomicLong(System.currentTimeMillis());
    private final AtomicLong counter = new AtomicLong(0);

    public FixedWindowRateLimiter(long maxQps) {
        this.maxQps = maxQps;
    }

    @Override
    public boolean tryAcquire() {
        long now = System.currentTimeMillis();
        long start = windowStart.get();
        if (now - start >= 1000) {
            if (windowStart.compareAndSet(start, now)) {
                counter.set(0);
            }
        }
        long current = counter.incrementAndGet();
        return current <= maxQps;
    }
}
