package com.aoaojiao.rpc.registry;

import com.aoaojiao.rpc.common.service.ServiceKey;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.ServerSocket;
import java.net.URL;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertTrue;

class RegistryHttpServerTest {
    private LocalRegistryService registry;
    private RegistryHttpServer server;
    private int port;

    @BeforeEach
    void setUp() throws Exception {
        registry = new LocalRegistryService();
        ServiceKey key = ServiceKey.of("com.test.HttpService", "v1", "default");
        registry.register(key, new ServiceInstance("127.0.0.1", 9004));
        port = findFreePort();
        server = new RegistryHttpServer(registry, port);
        server.start();
    }

    @AfterEach
    void tearDown() {
        server.stop();
        registry.close();
    }

    @Test
    void healthAndSnapshotEndpoints() throws Exception {
        String health = httpGet("http://localhost:" + port + "/health");
        String snapshot = httpGet("http://localhost:" + port + "/snapshot");
        String healthJson = httpGet("http://localhost:" + port + "/health.json");
        String snapshotJson = httpGet("http://localhost:" + port + "/snapshot.json");

        assertTrue(health.contains("health"));
        assertTrue(snapshot.contains("services"));
        assertTrue(healthJson.contains("status"));
        assertTrue(snapshotJson.contains("HttpService"));
    }

    private static String httpGet(String url) throws Exception {
        HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
        conn.setRequestMethod("GET");
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            return sb.toString();
        } finally {
            conn.disconnect();
        }
    }

    private static int findFreePort() throws Exception {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        }
    }
}
