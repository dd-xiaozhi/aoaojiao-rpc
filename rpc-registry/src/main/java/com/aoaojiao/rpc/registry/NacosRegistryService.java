package com.aoaojiao.rpc.registry;

import com.aoaojiao.rpc.common.service.ServiceKey;
import com.alibaba.nacos.api.NacosFactory;
import com.alibaba.nacos.api.naming.NamingService;
import com.alibaba.nacos.api.naming.pojo.Instance;
import com.alibaba.nacos.api.naming.listener.EventListener;
import com.alibaba.nacos.api.naming.listener.NamingEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class NacosRegistryService implements RegistryService {
    private final NamingService namingService;
    private final String group;

    public NacosRegistryService(RegistryConfig config) throws Exception {
        Properties properties = new Properties();
        properties.setProperty("serverAddr", config.getServerAddr());
        if (config.getNamespace() != null) {
            properties.setProperty("namespace", config.getNamespace());
        }
        if (config.getUsername() != null) {
            properties.setProperty("username", config.getUsername());
        }
        if (config.getPassword() != null) {
            properties.setProperty("password", config.getPassword());
        }
        this.group = config.getGroup();
        this.namingService = NacosFactory.createNamingService(properties);
    }

    @Override
    public void register(ServiceKey key, ServiceInstance instance) throws Exception {
        Instance nacosInstance = toInstance(instance);
        namingService.registerInstance(key.key(), group, nacosInstance);
    }

    @Override
    public void unregister(ServiceKey key, ServiceInstance instance) throws Exception {
        namingService.deregisterInstance(key.key(), group, instance.getHost(), instance.getPort());
    }

    @Override
    public List<ServiceInstance> discover(ServiceKey key) throws Exception {
        List<Instance> instances = namingService.getAllInstances(key.key(), group);
        List<ServiceInstance> result = new ArrayList<>(instances.size());
        for (Instance instance : instances) {
            result.add(fromInstance(instance));
        }
        return result;
    }

    @Override
    public void subscribe(ServiceKey key, ServiceChangeListener listener) throws Exception {
        EventListener nacosListener = event -> {
            if (event instanceof NamingEvent namingEvent) {
                List<ServiceInstance> instances = new ArrayList<>();
                for (Instance instance : namingEvent.getInstances()) {
                    instances.add(fromInstance(instance));
                }
                listener.onChange(instances);
            }
        };
        namingService.subscribe(key.key(), group, nacosListener);
    }

    @Override
    public void close() {
        try {
            namingService.shutDown();
        } catch (Exception ignored) {
        }
    }

    private Instance toInstance(ServiceInstance instance) {
        Instance nacosInstance = new Instance();
        nacosInstance.setIp(instance.getHost());
        nacosInstance.setPort(instance.getPort());
        nacosInstance.setWeight(instance.getWeight());
        return nacosInstance;
    }

    private ServiceInstance fromInstance(Instance instance) {
        ServiceInstance si = new ServiceInstance(instance.getIp(), instance.getPort());
        si.setWeight(instance.getWeight());
        return si;
    }
}
