package com.aoaojiao.rpc.core.server;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public final class InflightRequestTracker {
    private static final AtomicInteger INFLIGHT = new AtomicInteger(0);

    private InflightRequestTracker() {
    }

    public static void increment() {
        INFLIGHT.incrementAndGet();
    }

    public static void decrement() {
        INFLIGHT.decrementAndGet();
    }

    public static int get() {
        return INFLIGHT.get();
    }

    public static boolean awaitZero(long timeoutMillis) throws InterruptedException {
        long deadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(timeoutMillis);
        while (INFLIGHT.get() > 0) {
            if (System.nanoTime() >= deadline) {
                return false;
            }
            Thread.sleep(10);
        }
        return true;
    }
}
