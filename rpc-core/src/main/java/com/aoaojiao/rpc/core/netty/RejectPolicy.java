package com.aoaojiao.rpc.core.netty;

import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;

public enum RejectPolicy {
    ABORT,
    DISCARD;

    public RejectedExecutionHandler toHandler() {
        return switch (this) {
            case ABORT -> new ThreadPoolExecutor.AbortPolicy();
            case DISCARD -> new ThreadPoolExecutor.DiscardPolicy();
        };
    }
}
