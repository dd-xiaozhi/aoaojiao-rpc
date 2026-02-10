package com.aoaojiao.rpc.server.provider;

import com.aoaojiao.rpc.common.service.ServiceKey;

public interface ServiceProvider {
    void register(ServiceKey key, Object service);

    void unregister(ServiceKey key);

    Object get(ServiceKey key);
}
