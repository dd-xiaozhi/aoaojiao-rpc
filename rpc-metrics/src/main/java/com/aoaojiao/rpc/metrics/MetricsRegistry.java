package com.aoaojiao.rpc.metrics;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.LongAdder;
import java.util.function.Supplier;

/**
 * 指标注册表
 * 支持 Prometheus 格式输出、Histogram 统计和 Gauge
 */
public class MetricsRegistry {

    private final ConcurrentMap<String, LongAdder> counters = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, LongAdder> timers = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, Histogram> histograms = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, GaugeHolder> gauges = new ConcurrentHashMap<>();

    /**
     * 增加计数器
     */
    public void increment(String name) {
        increment(name, 1);
    }

    /**
     * 增加计数器（带增量）
     */
    public void increment(String name, long delta) {
        counters.computeIfAbsent(name, k -> new LongAdder()).add(delta);
    }

    /**
     * 记录时间（纳秒）
     */
    public void recordTime(String name, long nanos) {
        timers.computeIfAbsent(name, k -> new LongAdder()).add(nanos);
        // 同时记录到 Histogram
        getHistogram(name).recordValue(nanos / 1_000_000); // 转换为毫秒
    }

    /**
     * 获取计数器值
     */
    public long counter(String name) {
        LongAdder adder = counters.get(name);
        return adder == null ? 0L : adder.sum();
    }

    /**
     * 获取定时器累计值（纳秒）
     */
    public long timer(String name) {
        LongAdder adder = timers.get(name);
        return adder == null ? 0L : adder.sum();
    }

    /**
     * 获取 Histogram
     */
    public Histogram getHistogram(String name) {
        return histograms.computeIfAbsent(name, k -> new Histogram());
    }

    /**
     * 记录 Histogram 值
     */
    public void recordHistogram(String name, long value) {
        getHistogram(name).recordValue(value);
    }

    /**
     * 注册 Gauge
     */
    public void registerGauge(String name, Supplier<Double> supplier) {
        gauges.put(name, new GaugeHolder(supplier));
    }

    /**
     * 注销 Gauge
     */
    public void removeGauge(String name) {
        gauges.remove(name);
    }

    /**
     * 获取 Gauge 值
     */
    public double gauge(String name) {
        GaugeHolder holder = gauges.get(name);
        return holder == null ? 0.0 : holder.getValue();
    }

    /**
     * 获取所有计数器名称
     */
    public Set<String> getCounterNames() {
        return new HashSet<>(counters.keySet());
    }

    /**
     * 获取所有 Histogram 名称
     */
    public Set<String> getHistogramNames() {
        return new HashSet<>(histograms.keySet());
    }

    /**
     * 获取所有 Gauge 名称
     */
    public Set<String> getGaugeNames() {
        return new HashSet<>(gauges.keySet());
    }

    /**
     * 清除所有指标
     */
    public void clear() {
        counters.clear();
        timers.clear();
        histograms.clear();
        // 不清除 Gauge，因为它们是注册的
    }

    /**
     * 重置所有计数器
     */
    public void resetCounters() {
        counters.clear();
    }

    /**
     * 生成 Prometheus 格式输出
     */
    public String toPrometheusFormat() {
        StringBuilder sb = new StringBuilder();

        // 输出计数器
        for (Map.Entry<String, LongAdder> entry : counters.entrySet()) {
            String name = sanitizeMetricName(entry.getKey());
            long value = entry.getValue().sum();
            sb.append("# TYPE ").append(name).append(" counter\n");
            sb.append(name).append(" ").append(value).append("\n");
        }

        // 输出 Histogram（以毫秒为单位）
        for (Map.Entry<String, Histogram> entry : histograms.entrySet()) {
            String name = sanitizeMetricName(entry.getKey());
            Histogram histogram = entry.getValue();
            HistogramSnapshot snapshot = histogram.getSnapshot();

            sb.append("# TYPE ").append(name).append(" histogram\n");
            sb.append(name).append("_count ").append(histogram.getCount()).append("\n");
            sb.append(name).append("_sum ").append(snapshot.getSum()).append("\n");
            sb.append(name).append("_mean ").append(snapshot.getMean()).append("\n");
            sb.append(name).append("_p50 ").append(snapshot.getMedian()).append("\n");
            sb.append(name).append("_p90 ").append(snapshot.get90thPercentile()).append("\n");
            sb.append(name).append("_p99 ").append(snapshot.get99thPercentile()).append("\n");
            sb.append(name).append("_p999 ").append(snapshot.get999thPercentile()).append("\n");
        }

        // 输出 Gauge
        for (Map.Entry<String, GaugeHolder> entry : gauges.entrySet()) {
            String name = sanitizeMetricName(entry.getKey());
            double value = entry.getValue().getValue();
            sb.append("# TYPE ").append(name).append(" gauge\n");
            sb.append(name).append(" ").append(String.format("%.2f", value)).append("\n");
        }

        return sb.toString();
    }

    /**
     * 生成简单快照（用于兼容旧代码）
     */
    public String snapshot() {
        return "counters=" + counters + ", timers=" + timers;
    }

    /**
     * 转换为 Map
     */
    public Map<String, Object> toMap() {
        Map<String, Object> result = new HashMap<>();

        // 计数器
        Map<String, Long> counterMap = new HashMap<>();
        for (Map.Entry<String, LongAdder> entry : counters.entrySet()) {
            counterMap.put(entry.getKey(), entry.getValue().sum());
        }
        result.put("counters", counterMap);

        // Histogram
        Map<String, Map<String, Double>> histogramMap = new HashMap<>();
        for (Map.Entry<String, Histogram> entry : histograms.entrySet()) {
            HistogramSnapshot s = entry.getValue().getSnapshot();
            Map<String, Double> stats = new HashMap<>();
            stats.put("count", (double) entry.getValue().getCount());
            stats.put("mean", s.getMean());
            stats.put("p50", s.getMedian());
            stats.put("p90", s.get90thPercentile());
            stats.put("p99", s.get99thPercentile());
            histogramMap.put(entry.getKey(), stats);
        }
        result.put("histograms", histogramMap);

        // Gauge
        Map<String, Double> gaugeMap = new HashMap<>();
        for (Map.Entry<String, GaugeHolder> entry : gauges.entrySet()) {
            gaugeMap.put(entry.getKey(), entry.getValue().getValue());
        }
        result.put("gauges", gaugeMap);

        return result;
    }

    /**
     * 清理指标名中的非法字符
     */
    private String sanitizeMetricName(String name) {
        return name.replace('.', '_').replace('-', '_').replace(' ', '_');
    }

    /**
     * Gauge 持有器
     */
    private static class GaugeHolder {
        private final Supplier<Double> supplier;

        GaugeHolder(Supplier<Double> supplier) {
            this.supplier = supplier;
        }

        double getValue() {
            try {
                Double value = supplier.get();
                return value == null ? 0.0 : value;
            } catch (Exception e) {
                return 0.0;
            }
        }
    }

    /**
     * Histogram 快照（独立类，避免在 Histogram 内部引用自身）
     */
    public static class HistogramSnapshot {
        private final long sum;
        private final double mean;
        private final double median;
        private final double p90;
        private final double p99;
        private final double p999;
        private final double max;

        public HistogramSnapshot(long sum, double mean, double median,
                                double p90, double p99, double p999, double max) {
            this.sum = sum;
            this.mean = mean;
            this.median = median;
            this.p90 = p90;
            this.p99 = p99;
            this.p999 = p999;
            this.max = max;
        }

        public long getSum() {
            return sum;
        }

        public double getMean() {
            return mean;
        }

        public double getMedian() {
            return median;
        }

        public double get90thPercentile() {
            return p90;
        }

        public double get99thPercentile() {
            return p99;
        }

        public double get999thPercentile() {
            return p999;
        }

        public double getMax() {
            return max;
        }
    }

    /**
     * Histogram 实现
     * 使用 Reservoir Sampling 算法近似计算百分位数
     */
    public static class Histogram {
        private static final int DATA_STRUCTURE_SIZE = 2048;
        private final double[] values = new double[DATA_STRUCTURE_SIZE];
        private int count = 0;
        private long sum = 0;
        private double mean = 0;

        public synchronized void recordValue(double value) {
            if (count < DATA_STRUCTURE_SIZE) {
                values[count++] = value;
            } else {
                // 满了，随机替换（保持近似均匀采样）
                int index = (int) (Math.random() * DATA_STRUCTURE_SIZE);
                values[index] = value;
            }
            sum += (long) value;
            mean = (double) sum / count;
        }

        public int getCount() {
            return count;
        }

        public long getSum() {
            return sum;
        }

        public double getMean() {
            return mean;
        }

        public synchronized HistogramSnapshot getSnapshot() {
            if (count == 0) {
                return new HistogramSnapshot(0, 0, 0, 0, 0, 0, 0);
            }

            // 复制并排序
            double[] sorted = new double[count];
            System.arraycopy(values, 0, sorted, 0, count);
            Arrays.sort(sorted);

            return new HistogramSnapshot(
                    sum,
                    mean,
                    getPercentile(sorted, 0.5),
                    getPercentile(sorted, 0.9),
                    getPercentile(sorted, 0.99),
                    getPercentile(sorted, 0.999),
                    sorted[count - 1]
            );
        }

        private double getPercentile(double[] sorted, double quantile) {
            if (sorted.length == 0) return 0;
            int index = (int) Math.ceil(quantile * sorted.length) - 1;
            index = Math.max(0, Math.min(index, sorted.length - 1));
            return sorted[index];
        }
    }
}