package com.aoaojiao.rpc.common.serialization;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 序列化器工厂类
 * 支持通过 SPI 自动发现和手动注册序列化器
 */
public final class SerializerFactory {

    private static final Map<Byte, Serializer> SERIALIZERS = new ConcurrentHashMap<>();
    private static final List<SerializationType> REGISTERED_TYPES = new CopyOnWriteArrayList<>();

    static {
        // 注册默认序列化器
        register(new HessianSerializer());
        // 注册 JSON 序列化器
        register(new JsonSerializer());
        // 注册 Kryo 序列化器
        register(new KryoSerializer());
    }

    private SerializerFactory() {
    }

    /**
     * 注册序列化器
     * @param serializer 序列化器实例
     */
    public static void register(Serializer serializer) {
        if (serializer == null) {
            throw new IllegalArgumentException("Serializer cannot be null");
        }
        SerializationType type = serializer.type();
        SERIALIZERS.put(type.getCode(), serializer);
        if (!REGISTERED_TYPES.contains(type)) {
            REGISTERED_TYPES.add(type);
        }
    }

    /**
     * 获取序列化器
     * @param code 序列化类型码
     * @return 序列化器实例
     */
    public static Serializer get(byte code) {
        Serializer serializer = SERIALIZERS.get(code);
        if (serializer == null) {
            throw new IllegalArgumentException("Unknown serializer code: " + code + ". Available: " + getAvailableTypes());
        }
        return serializer;
    }

    /**
     * 获取序列化器
     * @param type 序列化类型
     * @return 序列化器实例
     */
    public static Serializer get(SerializationType type) {
        if (type == null) {
            throw new IllegalArgumentException("SerializationType cannot be null");
        }
        return get(type.getCode());
    }

    /**
     * 获取已注册的所有序列化类型
     */
    public static List<SerializationType> getRegisteredTypes() {
        return List.copyOf(REGISTERED_TYPES);
    }

    /**
     * 检查指定类型是否已注册
     */
    public static boolean isRegistered(SerializationType type) {
        return SERIALIZERS.containsKey(type.getCode());
    }

    /**
     * 获取默认序列化器（Hessian）
     */
    public static Serializer getDefault() {
        return SERIALIZERS.get(SerializationType.HESSIAN.getCode());
    }

    /**
     * 获取可用类型列表字符串
     */
    private static String getAvailableTypes() {
        StringBuilder sb = new StringBuilder();
        for (SerializationType type : REGISTERED_TYPES) {
            if (sb.length() > 0) {
                sb.append(", ");
            }
            sb.append(type.getName()).append("(").append(type.getCode()).append(")");
        }
        return sb.toString();
    }
}