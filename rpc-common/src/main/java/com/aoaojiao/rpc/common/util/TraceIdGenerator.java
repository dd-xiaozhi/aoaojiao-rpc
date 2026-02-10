package com.aoaojiao.rpc.common.util;

import java.util.UUID;

public final class TraceIdGenerator {
    private TraceIdGenerator() {
    }

    public static String newTraceId() {
        return UUID.randomUUID().toString().replace("-", "");
    }
}
