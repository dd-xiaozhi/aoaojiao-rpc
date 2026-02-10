package com.aoaojiao.rpc.core.transport;

import java.util.concurrent.atomic.AtomicLong;

public final class RequestIdGenerator {
    private static final AtomicLong ID = new AtomicLong(1);

    private RequestIdGenerator() {
    }

    public static long nextId() {
        return ID.getAndIncrement();
    }
}
