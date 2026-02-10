package com.aoaojiao.rpc.client;

import com.aoaojiao.rpc.common.service.RpcInvocation;
import com.aoaojiao.rpc.common.service.ServiceKey;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

public class RpcClientProxyFactory {
    private final SimpleRpcClient client;
    private final long timeoutMillis;

    public RpcClientProxyFactory(SimpleRpcClient client, long timeoutMillis) {
        this.client = client;
        this.timeoutMillis = timeoutMillis;
    }

    @SuppressWarnings("unchecked")
    public <T> T create(Class<T> interfaceClass, String version, String group) {
        ServiceKey serviceKey = ServiceKey.of(interfaceClass.getName(), version, group);
        InvocationHandler handler = new ClientInvocationHandler(client, serviceKey, timeoutMillis);
        return (T) Proxy.newProxyInstance(interfaceClass.getClassLoader(), new Class<?>[]{interfaceClass}, handler);
    }

    private static class ClientInvocationHandler implements InvocationHandler {
        private final SimpleRpcClient client;
        private final ServiceKey serviceKey;
        private final long timeoutMillis;

        private ClientInvocationHandler(SimpleRpcClient client, ServiceKey serviceKey, long timeoutMillis) {
            this.client = client;
            this.serviceKey = serviceKey;
            this.timeoutMillis = timeoutMillis;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            if (method.getDeclaringClass() == Object.class) {
                return method.invoke(this, args);
            }
            String[] parameterTypes = new String[method.getParameterTypes().length];
            for (int i = 0; i < method.getParameterTypes().length; i++) {
                parameterTypes[i] = method.getParameterTypes()[i].getName();
            }
            RpcInvocation invocation = new RpcInvocation(serviceKey, method.getName(), parameterTypes, args);
            return client.invoke(invocation, timeoutMillis);
        }
    }
}
