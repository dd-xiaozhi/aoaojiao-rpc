package com.aoaojiao.rpc.common.serialization;

public enum SerializationType {
    HESSIAN((byte) 1);

    private final byte code;

    SerializationType(byte code) {
        this.code = code;
    }

    public byte getCode() {
        return code;
    }

    public static SerializationType fromCode(byte code) {
        for (SerializationType type : values()) {
            if (type.code == code) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown serialization type: " + code);
    }
}
