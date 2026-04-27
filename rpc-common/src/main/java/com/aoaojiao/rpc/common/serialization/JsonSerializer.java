package com.aoaojiao.rpc.common.serialization;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.io.IOException;

/**
 * JSON 序列化器
 * 使用 Jackson 实现高性能 JSON 序列化/反序列化
 */
public class JsonSerializer implements Serializer {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    static {
        // 配置 ObjectMapper
        MAPPER.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
        MAPPER.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        MAPPER.configure(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY, true);
        MAPPER.setSerializationInclusion(JsonInclude.Include.NON_NULL);
    }

    @Override
    public byte[] serialize(Object obj) {
        if (obj == null) {
            return new byte[0];
        }
        try {
            return MAPPER.writeValueAsBytes(obj);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("JSON serialize failed", e);
        }
    }

    @Override
    public <T> T deserialize(byte[] data, Class<T> clazz) {
        if (data == null || data.length == 0) {
            return null;
        }
        try {
            return MAPPER.readValue(data, clazz);
        } catch (IOException e) {
            throw new IllegalStateException("JSON deserialize failed", e);
        }
    }

    @Override
    public SerializationType type() {
        return SerializationType.JSON;
    }

    /**
     * 获取共享的 ObjectMapper
     */
    public static ObjectMapper getObjectMapper() {
        return MAPPER;
    }
}