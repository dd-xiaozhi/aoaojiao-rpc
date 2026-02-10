package com.aoaojiao.rpc.client;

import com.aoaojiao.rpc.client.cluster.ClusterClient;
import com.aoaojiao.rpc.common.service.RpcInvocation;
import com.aoaojiao.rpc.common.service.ServiceKey;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

public class ClusterRpcClientProxyFactory {
    private final ClusterClient clusterClient;

    public ClusterRpcClientProxyFactory(ClusterClient clusterClient) {
        this.clusterClient = clusterClient;
    }

    @SuppressWarnings("unchecked")
    public <T> T create(Class<T> interfaceClass, String version, String group) {
        ServiceKey serviceKey = ServiceKey.of(interfaceClass.getName(), version, group);
        InvocationHandler handler = new ClientInvocationHandler(clusterClient, serviceKey);
        return (T) Proxy.newProxyInstance(interfaceClass.getClassLoader(), new Class<?>[]{interfaceClass}, handler);
    }

    private static class ClientInvocationHandler implements InvocationHandler {
        private final ClusterClient clusterClient;
        private final ServiceKey serviceKey;

        private ClientInvocationHandler(ClusterClient clusterClient, ServiceKey serviceKey) {
            this.clusterClient = clusterClient;
            this.serviceKey = serviceKey;
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
            return clusterClient.invoke(serviceKey, invocation);
        }
    }
}
