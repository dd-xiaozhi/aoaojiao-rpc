package com.aoaojiao.rpc.registry;

import com.aoaojiao.rpc.common.service.ServiceKey;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class LocalRegistryServiceTest {
    @Test
    void registerDiscoverAndUnregister() {
        LocalRegistryService registry = new LocalRegistryService();
        ServiceKey key = ServiceKey.of("com.test.UserService", "v1", "default");
        ServiceInstance instance = new ServiceInstance("127.0.0.1", 9000);

        registry.register(key, instance);
        List<ServiceInstance> discovered = registry.discover(key);
        assertEquals(1, discovered.size());
        assertEquals("127.0.0.1", discovered.get(0).getHost());
        assertEquals(9000, discovered.get(0).getPort());

        registry.unregister(key, instance);
        List<ServiceInstance> after = registry.discover(key);
        assertTrue(after.isEmpty());
    }

    @Test
    void subscribeReceivesUpdates() {
        LocalRegistryService registry = new LocalRegistryService();
        ServiceKey key = ServiceKey.of("com.test.OrderService", "v1", "default");
        ServiceInstance instance = new ServiceInstance("127.0.0.1", 9001);

        AtomicInteger updates = new AtomicInteger(0);
        registry.subscribe(key, list -> updates.incrementAndGet());
        registry.register(key, instance);

        assertTrue(updates.get() >= 2); // initial + register update
    }

    @Test
    void snapshotJsonContainsServiceKey() {
        LocalRegistryService registry = new LocalRegistryService();
        ServiceKey key = ServiceKey.of("com.test.PaymentService", "v1", "default");
        registry.register(key, new ServiceInstance("127.0.0.1", 9002));

        String json = registry.snapshotJson();
        assertTrue(json.contains(key.key()));
    }
}
