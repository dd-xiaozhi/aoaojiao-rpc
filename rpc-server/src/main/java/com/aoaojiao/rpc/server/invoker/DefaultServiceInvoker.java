package com.aoaojiao.rpc.server.invoker;

import com.aoaojiao.rpc.common.service.RpcInvocation;
import com.aoaojiao.rpc.core.server.ServiceInvoker;
import com.aoaojiao.rpc.server.provider.ServiceProvider;

import java.lang.reflect.Method;

/**
 * 默认服务调用器
 * 使用方法缓存优化反射调用性能
 */
public class DefaultServiceInvoker implements ServiceInvoker {

    private final ServiceProvider provider;
    private final MethodCache methodCache;

    public DefaultServiceInvoker(ServiceProvider provider) {
        this.provider = provider;
        this.methodCache = new MethodCache();
    }

    @Override
    public Object invoke(RpcInvocation invocation) throws Exception {
        Object service = provider.get(invocation.getServiceKey());
        if (service == null) {
            throw new IllegalStateException("Service not found: " + invocation.getServiceKey());
        }

        Class<?>[] paramTypes = resolveParameterTypes(invocation.getParameterTypes());

        // 使用方法缓存 + getDeclaredMethod 确保精确匹配
        Method method = methodCache.getMethod(service.getClass(), invocation.getMethodName(), paramTypes);

        return method.invoke(service, invocation.getArgs());
    }

    private Class<?>[] resolveParameterTypes(String[] names) throws ClassNotFoundException {
        if (names == null || names.length == 0) {
            return new Class<?>[0];
        }
        Class<?>[] types = new Class<?>[names.length];
        for (int i = 0; i < names.length; i++) {
            types[i] = methodCache.getClass(names[i]);
        }
        return types;
    }
}
