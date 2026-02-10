package com.aoaojiao.rpc.core.server;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class BusinessExecutor {
    private final ExecutorService executor;

    public BusinessExecutor(int threads, int queueSize, RejectedExecutionHandler handler) {
        this.executor = new ThreadPoolExecutor(
                threads,
                threads,
                60L,
                TimeUnit.SECONDS,
                new ArrayBlockingQueue<>(queueSize),
                handler
        );
    }

    public ExecutorService getExecutor() {
        return executor;
    }

    public void shutdown() {
        executor.shutdown();
    }
}
