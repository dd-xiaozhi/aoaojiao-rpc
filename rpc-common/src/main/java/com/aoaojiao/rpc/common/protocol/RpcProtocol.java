package com.aoaojiao.rpc.common.protocol;

public class RpcProtocol {
    private byte version;
    private RpcMessageType messageType;
    private byte serialization;
    private RpcStatus status;
    private long requestId;
    private byte[] body;

    public RpcProtocol() {
    }

    public RpcProtocol(byte version, RpcMessageType messageType, byte serialization, RpcStatus status, long requestId, byte[] body) {
        this.version = version;
        this.messageType = messageType;
        this.serialization = serialization;
        this.status = status;
        this.requestId = requestId;
        this.body = body;
    }

    public byte getVersion() {
        return version;
    }

    public void setVersion(byte version) {
        this.version = version;
    }

    public RpcMessageType getMessageType() {
        return messageType;
    }

    public void setMessageType(RpcMessageType messageType) {
        this.messageType = messageType;
    }

    public byte getSerialization() {
        return serialization;
    }

    public void setSerialization(byte serialization) {
        this.serialization = serialization;
    }

    public RpcStatus getStatus() {
        return status;
    }

    public void setStatus(RpcStatus status) {
        this.status = status;
    }

    public long getRequestId() {
        return requestId;
    }

    public void setRequestId(long requestId) {
        this.requestId = requestId;
    }

    public byte[] getBody() {
        return body;
    }

    public void setBody(byte[] body) {
        this.body = body;
    }

    public int getBodyLength() {
        return body == null ? 0 : body.length;
    }
}
