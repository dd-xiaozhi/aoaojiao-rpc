package com.aoaojiao.rpc.server.invoker;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 服务方法缓存
 * 按 serviceKey + methodName + paramTypes 缓存 Method 对象
 */
public class MethodCache {

    /**
     * 方法缓存
     * key = "className#methodName#paramType1,paramType2,..."
     */
    private final ConcurrentHashMap<String, MethodEntry> methodCache = new ConcurrentHashMap<>();

    /**
     * Class 缓存
     * className -> Class<?>
     */
    private final ConcurrentHashMap<String, Class<?>> classCache = new ConcurrentHashMap<>();

    /**
     * 获取或创建 Method 对象
     * @param serviceClass 服务类
     * @param methodName 方法名
     * @param paramTypes 参数类型数组
     * @return Method 对象
     */
    public Method getMethod(Class<?> serviceClass, String methodName, Class<?>[] paramTypes) {
        String key = buildKey(serviceClass, methodName, paramTypes);
        return methodCache.computeIfAbsent(key, k -> createMethod(serviceClass, methodName, paramTypes))
                .method;
    }

    /**
     * 获取类对象
     * @param className 类名
     * @return Class 对象
     */
    public Class<?> getClass(String className) {
        return classCache.computeIfAbsent(className, name -> {
            try {
                return ClassUtils.forName(name);
            } catch (ClassNotFoundException e) {
                throw new RuntimeException("Class not found: " + name, e);
            }
        });
    }

    /**
     * 构建缓存键
     */
    private String buildKey(Class<?> serviceClass, String methodName, Class<?>[] paramTypes) {
        StringBuilder sb = new StringBuilder();
        sb.append(serviceClass.getName())
          .append('#')
          .append(methodName);
        if (paramTypes != null && paramTypes.length > 0) {
            sb.append('#');
            for (int i = 0; i < paramTypes.length; i++) {
                if (i > 0) sb.append(',');
                sb.append(paramTypes[i].getName());
            }
        }
        return sb.toString();
    }

    /**
     * 创建 Method 对象
     * 使用 getDeclaredMethod 确保精确匹配当前类的方法
     */
    private MethodEntry createMethod(Class<?> serviceClass, String methodName, Class<?>[] paramTypes) {
        try {
            // 使用 getDeclaredMethod 而非 getMethod，确保精确匹配
            Method method = serviceClass.getDeclaredMethod(methodName, paramTypes);

            // 确保方法可访问
            if (!Modifier.isPublic(method.getModifiers())) {
                method.setAccessible(true);
            }

            return new MethodEntry(method);
        } catch (NoSuchMethodException e) {
            throw new IllegalStateException(
                    "Method not found: " + serviceClass.getName() + "#" + methodName +
                    "(" + paramTypesToString(paramTypes) + ")", e);
        }
    }

    /**
     * 参数类型转字符串
     */
    private String paramTypesToString(Class<?>[] paramTypes) {
        if (paramTypes == null || paramTypes.length == 0) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < paramTypes.length; i++) {
            if (i > 0) sb.append(", ");
            sb.append(paramTypes[i].getName());
        }
        return sb.toString();
    }

    /**
     * 清空缓存
     */
    public void clear() {
        methodCache.clear();
    }

    /**
     * 获取缓存的方法数量
     */
    public int size() {
        return methodCache.size();
    }

    /**
     * 获取缓存的类数量
     */
    public int classCount() {
        return classCache.size();
    }

    /**
     * 方法条目
     */
    private static class MethodEntry {
        final Method method;

        MethodEntry(Method method) {
            this.method = method;
        }
    }

    /**
     * 简化版 Class 工具类
     * 避免引入其他模块的依赖
     */
    public static class ClassUtils {
        private static final ConcurrentHashMap<String, Class<?>> PRIMITIVE_TYPES = new ConcurrentHashMap<>();

        static {
            PRIMITIVE_TYPES.put("boolean", boolean.class);
            PRIMITIVE_TYPES.put("byte", byte.class);
            PRIMITIVE_TYPES.put("char", char.class);
            PRIMITIVE_TYPES.put("short", short.class);
            PRIMITIVE_TYPES.put("int", int.class);
            PRIMITIVE_TYPES.put("long", long.class);
            PRIMITIVE_TYPES.put("float", float.class);
            PRIMITIVE_TYPES.put("double", double.class);
            PRIMITIVE_TYPES.put("void", void.class);
        }

        public static Class<?> forName(String className) throws ClassNotFoundException {
            // 检查基本类型
            Class<?> primitive = PRIMITIVE_TYPES.get(className);
            if (primitive != null) {
                return primitive;
            }

            // 尝试加载类
            return Class.forName(className);
        }
    }
}