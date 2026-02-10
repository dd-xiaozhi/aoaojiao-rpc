package com.aoaojiao.rpc.registry;

import com.aoaojiao.rpc.common.service.ServiceKey;

import java.util.List;

public interface RegistryService {
    void register(ServiceKey key, ServiceInstance instance) throws Exception;

    void unregister(ServiceKey key, ServiceInstance instance) throws Exception;

    List<ServiceInstance> discover(ServiceKey key) throws Exception;

    void subscribe(ServiceKey key, ServiceChangeListener listener) throws Exception;

    void close();
}
