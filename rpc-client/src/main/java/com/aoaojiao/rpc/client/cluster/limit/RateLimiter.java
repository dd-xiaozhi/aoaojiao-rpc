package com.aoaojiao.rpc.client.cluster.limit;

public interface RateLimiter {
    /**
     * 尝试获取一个许可
     * @return true 表示获取成功，false 表示被限流
     */
    boolean tryAcquire();

    /**
     * 尝试获取多个许可
     * @param permits 需要的许可数量
     * @return true 表示获取成功，false 表示被限流
     */
    default boolean tryAcquire(int permits) {
        return tryAcquire();
    }
}