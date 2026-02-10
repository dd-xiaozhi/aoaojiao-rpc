package com.aoaojiao.rpc.registry;

import com.aoaojiao.rpc.common.service.ServiceKey;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RegistryDiagnosticsTest {
    @Test
    void healthAndSnapshotJson() {
        LocalRegistryService registry = new LocalRegistryService();
        ServiceKey key = ServiceKey.of("com.test.HealthService", "v1", "default");
        registry.register(key, new ServiceInstance("127.0.0.1", 9003));

        String health = RegistryDiagnostics.health(registry);
        String healthJson = RegistryDiagnostics.healthJson(registry);
        String snapshotJson = RegistryDiagnostics.snapshotJson(registry);

        assertEquals("health=UP", health);
        assertEquals("{\"status\":\"UP\"}", healthJson);
        assertTrue(snapshotJson.contains(key.key()));
    }
}
