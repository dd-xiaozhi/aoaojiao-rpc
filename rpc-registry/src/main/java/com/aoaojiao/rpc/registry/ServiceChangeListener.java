package com.aoaojiao.rpc.registry;

import java.util.List;

public interface ServiceChangeListener {
    void onChange(List<ServiceInstance> instances);
}
