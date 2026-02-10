package com.aoaojiao.rpc.client.cluster.circuit;

public class CircuitBreaker {
    private final int failureThreshold;
    private final long openMillis;

    private int consecutiveFailures;
    private State state = State.CLOSED;
    private long openUntil;
    private boolean halfOpenInFlight;

    public CircuitBreaker(int failureThreshold, long openMillis) {
        this.failureThreshold = failureThreshold;
        this.openMillis = openMillis;
    }

    public synchronized boolean allowRequest() {
        long now = System.currentTimeMillis();
        if (state == State.OPEN) {
            if (now >= openUntil) {
                state = State.HALF_OPEN;
                halfOpenInFlight = false;
            } else {
                return false;
            }
        }
        if (state == State.HALF_OPEN) {
            if (halfOpenInFlight) {
                return false;
            }
            halfOpenInFlight = true;
            return true;
        }
        return true;
    }

    public synchronized void onSuccess() {
        consecutiveFailures = 0;
        if (state == State.HALF_OPEN) {
            state = State.CLOSED;
            halfOpenInFlight = false;
        }
    }

    public synchronized void onFailure() {
        if (state == State.HALF_OPEN) {
            open();
            return;
        }
        consecutiveFailures++;
        if (consecutiveFailures >= failureThreshold) {
            open();
        }
    }

    private void open() {
        state = State.OPEN;
        openUntil = System.currentTimeMillis() + openMillis;
        halfOpenInFlight = false;
        consecutiveFailures = 0;
    }

    public enum State {
        CLOSED,
        OPEN,
        HALF_OPEN
    }
}
