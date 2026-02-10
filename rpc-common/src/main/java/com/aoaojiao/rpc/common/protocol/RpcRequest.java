package com.aoaojiao.rpc.common.protocol;

import com.aoaojiao.rpc.common.service.RpcInvocation;

import java.io.Serializable;

public class RpcRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    private String traceId;
    private RpcInvocation invocation;

    public RpcRequest() {
    }

    public RpcRequest(String traceId, RpcInvocation invocation) {
        this.traceId = traceId;
        this.invocation = invocation;
    }

    public String getTraceId() {
        return traceId;
    }

    public void setTraceId(String traceId) {
        this.traceId = traceId;
    }

    public RpcInvocation getInvocation() {
        return invocation;
    }

    public void setInvocation(RpcInvocation invocation) {
        this.invocation = invocation;
    }
}
