package com.aoaojiao.rpc.example.boot.provider;

import com.aoaojiao.rpc.registry.RegistryHttpServer;
import com.aoaojiao.rpc.registry.RegistryReporter;
import com.aoaojiao.rpc.registry.RegistryService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Component
public class RegistryReporterRunner implements CommandLineRunner {
    private final RegistryService registryService;
    private final Environment env;

    public RegistryReporterRunner(RegistryService registryService, Environment env) {
        this.registryService = registryService;
        this.env = env;
    }

    @Override
    public void run(String... args) throws Exception {
        boolean enabled = Boolean.parseBoolean(env.getProperty("aoaojiao.rpc.registry.monitor.enabled", "true"));
        if (!enabled) {
            return;
        }
        int interval = Integer.parseInt(env.getProperty("aoaojiao.rpc.registry.monitor.intervalSeconds", "5"));
        int port = Integer.parseInt(env.getProperty("aoaojiao.rpc.registry.monitor.port", "18080"));

        RegistryReporter reporter = new RegistryReporter(registryService, interval);
        reporter.start();

        RegistryHttpServer httpServer = new RegistryHttpServer(registryService, port);
        httpServer.start();

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            reporter.stop();
            httpServer.stop();
        }));
    }
}
