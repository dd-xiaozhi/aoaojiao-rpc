package com.aoaojiao.rpc.registry;

import com.aoaojiao.rpc.common.service.ServiceKey;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ServiceDiscovery {
    private final RegistryService registryService;
    private final Map<String, List<ServiceInstance>> cache = new ConcurrentHashMap<>();

    public ServiceDiscovery(RegistryService registryService) {
        this.registryService = registryService;
    }

    public List<ServiceInstance> getInstances(ServiceKey key) throws Exception {
        List<ServiceInstance> cached = cache.get(key.key());
        if (cached != null && !cached.isEmpty()) {
            return cached;
        }
        List<ServiceInstance> instances = registryService.discover(key);
        cache.put(key.key(), instances);
        return instances;
    }

    public void subscribe(ServiceKey key) throws Exception {
        registryService.subscribe(key, instances -> cache.put(key.key(), instances));
    }

    public void remove(ServiceKey key) {
        cache.remove(key.key());
    }

    public void close() {
        registryService.close();
    }
}
