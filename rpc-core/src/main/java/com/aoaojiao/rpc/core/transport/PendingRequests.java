package com.aoaojiao.rpc.core.transport;

import com.aoaojiao.rpc.common.protocol.RpcProtocol;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public class PendingRequests {
    private final Map<Long, CompletableFuture<RpcProtocol>> pending = new ConcurrentHashMap<>();

    public CompletableFuture<RpcProtocol> register(long requestId) {
        CompletableFuture<RpcProtocol> future = new CompletableFuture<>();
        pending.put(requestId, future);
        return future;
    }

    public void complete(long requestId, RpcProtocol response) {
        CompletableFuture<RpcProtocol> future = pending.remove(requestId);
        if (future != null) {
            future.complete(response);
        }
    }

    public void fail(long requestId, Throwable ex) {
        CompletableFuture<RpcProtocol> future = pending.remove(requestId);
        if (future != null) {
            future.completeExceptionally(ex);
        }
    }
}
