package com.aoaojiao.rpc.metrics;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.LongAdder;

public class MetricsRegistry {
    private final ConcurrentMap<String, LongAdder> counters = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, LongAdder> timers = new ConcurrentHashMap<>();

    public void increment(String name) {
        counters.computeIfAbsent(name, k -> new LongAdder()).increment();
    }

    public void recordTime(String name, long nanos) {
        timers.computeIfAbsent(name, k -> new LongAdder()).add(nanos);
    }

    public long counter(String name) {
        LongAdder adder = counters.get(name);
        return adder == null ? 0L : adder.sum();
    }

    public long timer(String name) {
        LongAdder adder = timers.get(name);
        return adder == null ? 0L : adder.sum();
    }

    public String snapshot() {
        return "counters=" + counters + ", timers=" + timers;
    }
}
