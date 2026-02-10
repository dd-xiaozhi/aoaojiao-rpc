package com.aoaojiao.rpc.common.util;

import java.util.HashMap;
import java.util.Map;

public final class ClassUtils {
    private static final Map<String, Class<?>> PRIMITIVES = new HashMap<>();

    static {
        PRIMITIVES.put("boolean", boolean.class);
        PRIMITIVES.put("byte", byte.class);
        PRIMITIVES.put("short", short.class);
        PRIMITIVES.put("char", char.class);
        PRIMITIVES.put("int", int.class);
        PRIMITIVES.put("long", long.class);
        PRIMITIVES.put("float", float.class);
        PRIMITIVES.put("double", double.class);
        PRIMITIVES.put("void", void.class);
    }

    private ClassUtils() {
    }

    public static Class<?> forName(String name) throws ClassNotFoundException {
        Class<?> primitive = PRIMITIVES.get(name);
        if (primitive != null) {
            return primitive;
        }
        return Class.forName(name);
    }
}
