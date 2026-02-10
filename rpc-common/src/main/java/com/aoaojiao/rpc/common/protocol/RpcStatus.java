package com.aoaojiao.rpc.common.protocol;

public enum RpcStatus {
    OK((byte) 0),
    FAIL((byte) 1);

    private final byte code;

    RpcStatus(byte code) {
        this.code = code;
    }

    public byte getCode() {
        return code;
    }

    public static RpcStatus fromCode(byte code) {
        for (RpcStatus status : values()) {
            if (status.code == code) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown status: " + code);
    }
}
