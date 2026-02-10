package com.aoaojiao.rpc.registry;

import com.aoaojiao.rpc.common.serialization.HessianSerializer;

import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;

public class RegistryHttpServer {
    private final RegistryService registryService;
    private final int port;
    private HttpServer server;

    public RegistryHttpServer(RegistryService registryService, int port) {
        this.registryService = registryService;
        this.port = port;
    }

    public void start() throws IOException {
        if (server != null) {
            return;
        }
        server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/health", exchange -> {
            String body = RegistryDiagnostics.health(registryService);
            byte[] bytes = body.getBytes();
            exchange.getResponseHeaders().add("Content-Type", "text/plain;charset=utf-8");
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        });
        server.createContext("/snapshot", exchange -> {
            String body = RegistryDiagnostics.snapshot(registryService);
            byte[] bytes = body.getBytes();
            exchange.getResponseHeaders().add("Content-Type", "text/plain;charset=utf-8");
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        });
        server.createContext("/health.json", exchange -> {
            String body = RegistryDiagnostics.healthJson(registryService);
            byte[] bytes = body.getBytes();
            exchange.getResponseHeaders().add("Content-Type", "application/json;charset=utf-8");
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        });
        server.createContext("/snapshot.json", exchange -> {
            String body = RegistryDiagnostics.snapshotJson(registryService);
            byte[] bytes = body.getBytes();
            exchange.getResponseHeaders().add("Content-Type", "application/json;charset=utf-8");
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        });
        server.start();
    }

    public void stop() {
        if (server != null) {
            server.stop(0);
            server = null;
        }
    }
}
