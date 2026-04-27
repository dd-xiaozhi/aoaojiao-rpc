package com.aoaojiao.rpc.client.cluster.circuit;

/**
 * 多模式熔断器
 * 支持计数模式和失败率模式
 */
public class CircuitBreaker {

    /**
     * 熔断模式
     */
    public enum Mode {
        /**
         * 计数模式：连续失败 N 次触发熔断
         */
        COUNTING,
        /**
         * 失败率模式：滑动窗口内失败率超过阈值触发熔断
         */
        FAILURE_RATE
    }

    private final Mode mode;
    private final int failureThreshold;  // 计数模式下为连续失败次数，失败率模式下为最小请求数
    private final double failureRateThreshold; // 失败率模式下为失败率阈值 (0.0 - 1.0)
    private final long openMillis;

    private int consecutiveFailures;
    private State state = State.CLOSED;
    private long openUntil;
    private boolean halfOpenInFlight;

    // 滑动窗口统计（仅在 FAILURE_RATE 模式下使用）
    private final SlidingWindowStats slidingWindowStats;

    public CircuitBreaker(int failureThreshold, long openMillis) {
        this(Mode.COUNTING, failureThreshold, 0.5, openMillis);
    }

    public CircuitBreaker(Mode mode, int failureThreshold, long openMillis) {
        this(mode, failureThreshold, 0.5, openMillis);
    }

    public CircuitBreaker(Mode mode, int failureThreshold, double failureRateThreshold, long openMillis) {
        if (failureThreshold <= 0) {
            throw new IllegalArgumentException("failureThreshold must be positive");
        }
        if (failureRateThreshold <= 0 || failureRateThreshold > 1) {
            throw new IllegalArgumentException("failureRateThreshold must be between 0 and 1");
        }
        if (openMillis <= 0) {
            throw new IllegalArgumentException("openMillis must be positive");
        }

        this.mode = mode;
        this.failureThreshold = failureThreshold;
        this.failureRateThreshold = failureRateThreshold;
        this.openMillis = openMillis;

        if (mode == Mode.FAILURE_RATE) {
            this.slidingWindowStats = new SlidingWindowStats();
        } else {
            this.slidingWindowStats = null;
        }
    }

    /**
     * 允许请求
     */
    public synchronized boolean allowRequest() {
        long now = System.currentTimeMillis();

        // OPEN -> HALF_OPEN 转换
        if (state == State.OPEN) {
            if (now >= openUntil) {
                state = State.HALF_OPEN;
                halfOpenInFlight = false;
            } else {
                return false;
            }
        }

        // HALF_OPEN 状态：只允许一个试探请求
        if (state == State.HALF_OPEN) {
            if (halfOpenInFlight) {
                return false;
            }
            halfOpenInFlight = true;
            return true;
        }

        // CLOSED 状态：检查是否应该触发熔断
        if (state == State.CLOSED) {
            if (mode == Mode.FAILURE_RATE) {
                // 失败率模式：检查失败率是否超过阈值
                if (slidingWindowStats.getTotalRequests() >= failureThreshold) {
                    if (slidingWindowStats.getFailureRate() >= failureRateThreshold) {
                        open();
                        return false;
                    }
                }
            }
        }

        return true;
    }

    /**
     * 记录成功
     */
    public synchronized void onSuccess() {
        consecutiveFailures = 0;

        if (mode == Mode.FAILURE_RATE) {
            slidingWindowStats.recordSuccess();
        }

        if (state == State.HALF_OPEN) {
            // 试探成功，关闭熔断器
            state = State.CLOSED;
            halfOpenInFlight = false;
            if (mode == Mode.FAILURE_RATE) {
                slidingWindowStats.reset();
            }
        }
    }

    /**
     * 记录失败
     */
    public synchronized void onFailure() {
        if (mode == Mode.FAILURE_RATE) {
            slidingWindowStats.recordFailure();
        }

        if (state == State.HALF_OPEN) {
            // 试探失败，重新打开熔断器
            open();
            return;
        }

        if (mode == Mode.COUNTING) {
            consecutiveFailures++;
            if (consecutiveFailures >= failureThreshold) {
                open();
            }
        } else {
            // 失败率模式：直接检查失败率
            if (slidingWindowStats.getTotalRequests() >= failureThreshold) {
                if (slidingWindowStats.getFailureRate() >= failureRateThreshold) {
                    open();
                }
            }
        }
    }

    /**
     * 打开熔断器
     */
    private void open() {
        state = State.OPEN;
        openUntil = System.currentTimeMillis() + openMillis;
        halfOpenInFlight = false;
        consecutiveFailures = 0;
    }

    /**
     * 获取当前状态
     */
    public State getState() {
        long now = System.currentTimeMillis();
        if (state == State.OPEN && now >= openUntil) {
            return State.HALF_OPEN;
        }
        return state;
    }

    /**
     * 获取连续失败次数（仅 COUNTING 模式有效）
     */
    public int getConsecutiveFailures() {
        return consecutiveFailures;
    }

    /**
     * 获取滑动窗口统计（仅 FAILURE_RATE 模式有效）
     */
    public SlidingWindowStats getSlidingWindowStats() {
        return slidingWindowStats;
    }

    /**
     * 获取熔断模式
     */
    public Mode getMode() {
        return mode;
    }

    /**
     * 获取失败阈值
     */
    public int getFailureThreshold() {
        return failureThreshold;
    }

    /**
     * 获取失败率阈值
     */
    public double getFailureRateThreshold() {
        return failureRateThreshold;
    }

    /**
     * 手动重置熔断器
     */
    public synchronized void reset() {
        state = State.CLOSED;
        consecutiveFailures = 0;
        halfOpenInFlight = false;
        if (slidingWindowStats != null) {
            slidingWindowStats.reset();
        }
    }

    /**
     * 强制打开熔断器
     */
    public synchronized void forceOpen() {
        state = State.OPEN;
        openUntil = Long.MAX_VALUE;
        halfOpenInFlight = false;
        consecutiveFailures = 0;
    }

    /**
     * 强制关闭熔断器
     */
    public synchronized void forceClosed() {
        reset();
    }

    /**
     * 熔断器状态
     */
    public enum State {
        /**
         * 关闭状态：正常请求通过
         */
        CLOSED,
        /**
         * 打开状态：所有请求被拒绝
         */
        OPEN,
        /**
         * 半开状态：允许一个试探请求
         */
        HALF_OPEN
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("CircuitBreaker{mode=").append(mode)
          .append(", state=").append(getState())
          .append(", threshold=").append(failureThreshold);

        if (mode == Mode.FAILURE_RATE && slidingWindowStats != null) {
            sb.append(", failureRate=").append(String.format("%.2f%%", slidingWindowStats.getFailureRate() * 100));
        } else {
            sb.append(", consecutiveFailures=").append(consecutiveFailures);
        }

        sb.append("}");
        return sb.toString();
    }

    /**
     * 创建一个使用失败率模式的熔断器
     */
    public static CircuitBreaker createFailureRateBreaker(int minRequests, double failureRateThreshold, long openMillis) {
        return new CircuitBreaker(Mode.FAILURE_RATE, minRequests, failureRateThreshold, openMillis);
    }

    /**
     * 创建一个使用计数模式的熔断器
     */
    public static CircuitBreaker createCountingBreaker(int consecutiveFailures, long openMillis) {
        return new CircuitBreaker(Mode.COUNTING, consecutiveFailures, openMillis);
    }
}