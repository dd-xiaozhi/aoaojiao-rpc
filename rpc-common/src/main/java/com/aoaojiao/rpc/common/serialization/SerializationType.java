package com.aoaojiao.rpc.common.serialization;

/**
 * 序列化类型枚举
 * 定义支持的序列化协议类型
 */
public enum SerializationType {
    /**
     * Hessian 二进制序列化
     */
    HESSIAN((byte) 1, "hessian"),

    /**
     * JSON 文本序列化
     */
    JSON((byte) 2, "json"),

    /**
     * Kryo 二进制序列化
     */
    KRYO((byte) 3, "kryo"),

    /**
     * FastJSON 序列化
     */
    FASTJSON((byte) 4, "fastjson"),

    /**
     * Protobuf 序列化
     */
    PROTOBUF((byte) 5, "protobuf");

    private final byte code;
    private final String name;

    SerializationType(byte code, String name) {
        this.code = code;
        this.name = name;
    }

    public byte getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    /**
     * 根据字节码获取序列化类型
     */
    public static SerializationType fromCode(byte code) {
        for (SerializationType type : values()) {
            if (type.code == code) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown serialization type code: " + code);
    }

    /**
     * 根据名称获取序列化类型
     */
    public static SerializationType fromName(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Serialization type name cannot be null or blank");
        }
        for (SerializationType type : values()) {
            if (type.name.equalsIgnoreCase(name)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown serialization type name: " + name);
    }
}