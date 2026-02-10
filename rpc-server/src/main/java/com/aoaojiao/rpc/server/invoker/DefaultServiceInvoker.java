package com.aoaojiao.rpc.server.invoker;

import com.aoaojiao.rpc.common.service.RpcInvocation;
import com.aoaojiao.rpc.common.util.ClassUtils;
import com.aoaojiao.rpc.core.server.ServiceInvoker;
import com.aoaojiao.rpc.server.provider.ServiceProvider;

import java.lang.reflect.Method;

public class DefaultServiceInvoker implements ServiceInvoker {
    private final ServiceProvider provider;

    public DefaultServiceInvoker(ServiceProvider provider) {
        this.provider = provider;
    }

    @Override
    public Object invoke(RpcInvocation invocation) throws Exception {
        Object service = provider.get(invocation.getServiceKey());
        if (service == null) {
            throw new IllegalStateException("Service not found: " + invocation.getServiceKey());
        }
        Class<?>[] paramTypes = resolveParameterTypes(invocation.getParameterTypes());
        Method method = service.getClass().getMethod(invocation.getMethodName(), paramTypes);
        return method.invoke(service, invocation.getArgs());
    }

    private Class<?>[] resolveParameterTypes(String[] names) throws ClassNotFoundException {
        if (names == null || names.length == 0) {
            return new Class<?>[0];
        }
        Class<?>[] types = new Class<?>[names.length];
        for (int i = 0; i < names.length; i++) {
            types[i] = ClassUtils.forName(names[i]);
        }
        return types;
    }
}
