package com.aoaojiao.rpc.common.serialization;

import java.util.List;
import java.util.ServiceLoader;

/**
 * 序列化器 SPI 服务加载器
 * 通过 Java SPI 机制自动发现和注册序列化器
 */
public final class SerializerLoader {

    private static volatile boolean initialized = false;

    private SerializerLoader() {
    }

    /**
     * 加载所有通过 SPI 注册的序列化器
     * 必须在 SerializerFactory 使用前调用
     */
    public static synchronized void loadSerializers() {
        if (initialized) {
            return;
        }

        ServiceLoader<Serializer> loader = ServiceLoader.load(Serializer.class);
        for (Serializer serializer : loader) {
            SerializerFactory.register(serializer);
        }

        initialized = true;
    }

    /**
     * 获取所有已注册的序列化器类型
     */
    public static List<SerializationType> getRegisteredTypes() {
        return SerializerFactory.getRegisteredTypes();
    }

    /**
     * 检查指定序列化类型是否已注册
     */
    public static boolean isRegistered(SerializationType type) {
        return SerializerFactory.isRegistered(type);
    }

    /**
     * 重置初始化状态（用于测试）
     */
    static void reset() {
        initialized = false;
    }
}