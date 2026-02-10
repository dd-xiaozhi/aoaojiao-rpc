package com.aoaojiao.rpc.common.protocol;

import java.io.Serializable;

public class RpcResponse implements Serializable {
    private static final long serialVersionUID = 1L;

    private String traceId;
    private boolean success;
    private Object data;
    private String errorMessage;

    public RpcResponse() {
    }

    public static RpcResponse ok(String traceId, Object data) {
        RpcResponse response = new RpcResponse();
        response.traceId = traceId;
        response.success = true;
        response.data = data;
        return response;
    }

    public static RpcResponse fail(String traceId, String errorMessage) {
        RpcResponse response = new RpcResponse();
        response.traceId = traceId;
        response.success = false;
        response.errorMessage = errorMessage;
        return response;
    }

    public String getTraceId() {
        return traceId;
    }

    public void setTraceId(String traceId) {
        this.traceId = traceId;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public Object getData() {
        return data;
    }

    public void setData(Object data) {
        this.data = data;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
}
