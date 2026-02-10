package com.aoaojiao.rpc.common.serialization;

public interface Serializer {
    byte[] serialize(Object obj);

    <T> T deserialize(byte[] data, Class<T> clazz);

    SerializationType type();
}
