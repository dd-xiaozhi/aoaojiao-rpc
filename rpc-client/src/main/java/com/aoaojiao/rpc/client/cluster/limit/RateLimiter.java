package com.aoaojiao.rpc.client.cluster.limit;

public interface RateLimiter {
    boolean tryAcquire();
}
