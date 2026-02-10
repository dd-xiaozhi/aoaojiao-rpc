package com.aoaojiao.rpc.common.serialization;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class SerializerFactory {
    private static final Map<Byte, Serializer> SERIALIZERS = new ConcurrentHashMap<>();

    static {
        Serializer hessian = new HessianSerializer();
        SERIALIZERS.put(hessian.type().getCode(), hessian);
    }

    private SerializerFactory() {
    }

    public static Serializer get(byte code) {
        Serializer serializer = SERIALIZERS.get(code);
        if (serializer == null) {
            throw new IllegalArgumentException("Unknown serializer code: " + code);
        }
        return serializer;
    }
}
