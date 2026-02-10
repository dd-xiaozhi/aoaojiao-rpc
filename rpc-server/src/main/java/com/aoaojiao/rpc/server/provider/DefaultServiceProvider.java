package com.aoaojiao.rpc.server.provider;

import com.aoaojiao.rpc.common.service.ServiceKey;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class DefaultServiceProvider implements ServiceProvider {
    private final Map<String, Object> services = new ConcurrentHashMap<>();

    @Override
    public void register(ServiceKey key, Object service) {
        services.put(key.key(), service);
    }

    @Override
    public void unregister(ServiceKey key) {
        services.remove(key.key());
    }

    @Override
    public Object get(ServiceKey key) {
        return services.get(key.key());
    }
}
