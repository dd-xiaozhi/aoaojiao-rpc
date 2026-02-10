package com.aoaojiao.rpc.metrics;

public final class MetricsHolder {
    private static final MetricsRegistry REGISTRY = new MetricsRegistry();

    private MetricsHolder() {
    }

    public static MetricsRegistry registry() {
        return REGISTRY;
    }
}
