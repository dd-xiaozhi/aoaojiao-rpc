package com.aoaojiao.rpc.registry;

import com.aoaojiao.rpc.common.service.ServiceKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class LocalRegistryService implements RegistryService, RegistryHealthIndicator {
    private static final Logger log = LoggerFactory.getLogger(LocalRegistryService.class);

    private final Map<String, List<ServiceInstance>> services = new ConcurrentHashMap<>();
    private final Map<String, List<ServiceChangeListener>> listeners = new ConcurrentHashMap<>();

    @Override
    public void register(ServiceKey key, ServiceInstance instance) {
        services.compute(key.key(), (k, v) -> {
            List<ServiceInstance> list = v == null ? new CopyOnWriteArrayList<>() : v;
            list.removeIf(si -> si.getHost().equals(instance.getHost()) && si.getPort() == instance.getPort());
            list.add(instance);
            return list;
        });
        notifyListeners(key);
        log.debug("local registry register: {} -> {}", key, instance);
    }

    @Override
    public void unregister(ServiceKey key, ServiceInstance instance) {
        services.computeIfPresent(key.key(), (k, v) -> {
            v.removeIf(si -> si.getHost().equals(instance.getHost()) && si.getPort() == instance.getPort());
            return v.isEmpty() ? null : v;
        });
        notifyListeners(key);
        log.debug("local registry unregister: {} -> {}", key, instance);
    }

    @Override
    public List<ServiceInstance> discover(ServiceKey key) {
        List<ServiceInstance> list = services.get(key.key());
        if (list == null) {
            return Collections.emptyList();
        }
        return new ArrayList<>(list);
    }

    @Override
    public void subscribe(ServiceKey key, ServiceChangeListener listener) {
        listeners.compute(key.key(), (k, v) -> {
            List<ServiceChangeListener> list = v == null ? new CopyOnWriteArrayList<>() : v;
            list.add(listener);
            return list;
        });
        listener.onChange(discover(key));
    }

    @Override
    public void close() {
        services.clear();
        listeners.clear();
    }

    @Override
    public boolean isHealthy() {
        return true;
    }

    @Override
    public String report() {
        return "health=UP";
    }

    public String snapshot() {
        return "services=" + services;
    }

    public String snapshotJson() {
        StringBuilder sb = new StringBuilder();
        sb.append('{');
        int i = 0;
        for (Map.Entry<String, List<ServiceInstance>> entry : services.entrySet()) {
            sb.append("\"").append(entry.getKey()).append("\":");
            sb.append(RegistryDiagnostics.instancesJson(entry.getValue()));
            if (i < services.size() - 1) {
                sb.append(',');
            }
            i++;
        }
        sb.append('}');
        return sb.toString();
    }

    private void notifyListeners(ServiceKey key) {
        List<ServiceChangeListener> ls = listeners.get(key.key());
        if (ls == null || ls.isEmpty()) {
            return;
        }
        List<ServiceInstance> snapshot = discover(key);
        for (ServiceChangeListener listener : ls) {
            listener.onChange(snapshot);
        }
    }
}
