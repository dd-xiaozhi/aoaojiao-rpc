package com.aoaojiao.rpc.spring.support;

import com.aoaojiao.rpc.common.service.ServiceKey;
import com.aoaojiao.rpc.spring.annotation.RpcService;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;

public class RpcServiceRegistrar implements BeanPostProcessor {
    private final RpcServerLifecycle lifecycle;

    public RpcServiceRegistrar(RpcServerLifecycle lifecycle) {
        this.lifecycle = lifecycle;
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        RpcService rpcService = bean.getClass().getAnnotation(RpcService.class);
        if (rpcService == null) {
            return bean;
        }
        Class<?>[] interfaces = bean.getClass().getInterfaces();
        if (interfaces.length == 0) {
            throw new IllegalStateException("@RpcService must implement interface: " + bean.getClass());
        }
        Class<?> interfaceClass = interfaces[0];
        ServiceKey key = ServiceKey.of(interfaceClass.getName(), rpcService.version(), rpcService.group());
        lifecycle.bootstrap().register(key, bean);
        return bean;
    }
}
