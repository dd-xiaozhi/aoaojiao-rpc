package com.aoaojiao.rpc.core.transport;

import com.aoaojiao.rpc.common.protocol.RpcProtocol;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

/**
 * 待处理请求管理器
 * 支持超时自动清理
 */
public class PendingRequests implements AutoCloseable {

    /**
     * 待处理请求映射
     */
    private final Map<Long, PendingRequest> pending = new ConcurrentHashMap<>();

    /**
     * 超时清理调度器
     */
    private final ScheduledExecutorService cleaner;

    /**
     * 清理间隔配置（毫秒）
     */
    private volatile long cleanerIntervalMillis = 5000;

    /**
     * 默认请求超时时间（毫秒）
     */
    private volatile long defaultTimeoutMillis = 30000;

    /**
     * 清理次数统计
     */
    private final AtomicLong cleanedCount = new AtomicLong(0);

    /**
     * 超时次数统计
     */
    private final AtomicLong timeoutCount = new AtomicLong(0);

    /**
     * 超时监听器列表
     */
    private final List<TimeoutListener> timeoutListeners = new ArrayList<>();

    /**
     * 清理任务标识
     */
    private volatile boolean cleaning = true;

    public PendingRequests() {
        this.cleaner = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "pending-requests-cleaner");
            t.setDaemon(true);
            return t;
        });
        startCleaner();
    }

    /**
     * 注册一个待处理的请求
     * @param requestId 请求ID
     * @return 关联的 CompletableFuture
     */
    public CompletableFuture<RpcProtocol> register(long requestId) {
        return register(requestId, defaultTimeoutMillis);
    }

    /**
     * 注册一个带超时的待处理请求
     * @param requestId 请求ID
     * @param timeoutMillis 超时时间（毫秒）
     * @return 关联的 CompletableFuture
     */
    public CompletableFuture<RpcProtocol> register(long requestId, long timeoutMillis) {
        PendingRequest pendingRequest = new PendingRequest(timeoutMillis);
        pending.put(requestId, pendingRequest);
        return pendingRequest.future;
    }

    /**
     * 完成请求
     * @param requestId 请求ID
     * @param response 响应
     */
    public void complete(long requestId, RpcProtocol response) {
        PendingRequest pendingRequest = pending.remove(requestId);
        if (pendingRequest != null) {
            pendingRequest.future.complete(response);
        }
    }

    /**
     * 请求失败
     * @param requestId 请求ID
     * @param ex 异常
     */
    public void fail(long requestId, Throwable ex) {
        PendingRequest pendingRequest = pending.remove(requestId);
        if (pendingRequest != null) {
            pendingRequest.future.completeExceptionally(ex);
        }
    }

    /**
     * 获取当前待处理请求数量
     */
    public int size() {
        return pending.size();
    }

    /**
     * 检查是否有待处理请求
     */
    public boolean isEmpty() {
        return pending.isEmpty();
    }

    /**
     * 清空所有待处理请求
     */
    public void clear() {
        pending.clear();
    }

    /**
     * 添加超时监听器
     * @param listener 超时监听器
     */
    public void addTimeoutListener(TimeoutListener listener) {
        if (listener != null) {
            timeoutListeners.add(listener);
        }
    }

    /**
     * 移除超时监听器
     * @param listener 超时监听器
     */
    public void removeTimeoutListener(TimeoutListener listener) {
        timeoutListeners.remove(listener);
    }

    /**
     * 获取清理次数统计
     */
    public long getCleanedCount() {
        return cleanedCount.get();
    }

    /**
     * 获取超时次数统计
     */
    public long getTimeoutCount() {
        return timeoutCount.get();
    }

    /**
     * 启动定时清理线程
     */
    private void startCleaner() {
        cleaner.scheduleWithFixedDelay(() -> {
            if (!cleaning) {
                return;
            }
            cleanTimeoutRequests();
        }, cleanerIntervalMillis, cleanerIntervalMillis, TimeUnit.MILLISECONDS);
    }

    /**
     * 清理超时的请求
     */
    private void cleanTimeoutRequests() {
        long now = System.currentTimeMillis();
        List<Long> timeoutIds = new ArrayList<>();

        // 收集超时的请求
        for (Map.Entry<Long, PendingRequest> entry : pending.entrySet()) {
            if (now > entry.getValue().deadline) {
                timeoutIds.add(entry.getKey());
            }
        }

        // 处理超时的请求
        for (Long requestId : timeoutIds) {
            PendingRequest pendingRequest = pending.remove(requestId);
            if (pendingRequest != null) {
                // 完成异常
                pendingRequest.future.completeExceptionally(
                        new TimeoutException("Request timeout: " + requestId));
                timeoutCount.incrementAndGet();
                cleanedCount.incrementAndGet();

                // 通知监听器
                notifyTimeout(requestId, pendingRequest);
            }
        }
    }

    /**
     * 通知超时监听器
     */
    private void notifyTimeout(long requestId, PendingRequest pendingRequest) {
        for (TimeoutListener listener : timeoutListeners) {
            try {
                listener.onTimeout(requestId);
            } catch (Exception e) {
                // 忽略监听器异常
            }
        }
    }

    @Override
    public void close() {
        cleaning = false;
        cleaner.shutdown();

        // 取消所有待处理请求
        for (Map.Entry<Long, PendingRequest> entry : pending.entrySet()) {
            entry.getValue().future.cancel(false);
        }
        pending.clear();

        try {
            if (!cleaner.awaitTermination(1, TimeUnit.SECONDS)) {
                cleaner.shutdownNow();
            }
        } catch (InterruptedException e) {
            cleaner.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    /**
     * 设置清理间隔
     * @param intervalMillis 清理间隔（毫秒）
     */
    public void setCleanerInterval(long intervalMillis) {
        this.cleanerIntervalMillis = intervalMillis;
    }

    /**
     * 设置默认超时时间
     * @param timeoutMillis 超时时间（毫秒）
     */
    public void setDefaultTimeout(long timeoutMillis) {
        this.defaultTimeoutMillis = timeoutMillis;
    }

    /**
     * 内部类：待处理请求
     */
    private static class PendingRequest {
        final CompletableFuture<RpcProtocol> future;
        final long deadline;

        PendingRequest(long timeoutMillis) {
            this.future = new CompletableFuture<>();
            this.deadline = System.currentTimeMillis() + timeoutMillis;
        }
    }

    /**
     * 超时监听器接口
     */
    public interface TimeoutListener {
        /**
         * 请求超时时回调
         * @param requestId 请求ID
         */
        void onTimeout(long requestId);
    }
}
