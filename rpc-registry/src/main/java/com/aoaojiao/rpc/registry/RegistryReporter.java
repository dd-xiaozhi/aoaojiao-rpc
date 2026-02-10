package com.aoaojiao.rpc.registry;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class RegistryReporter {
    private final RegistryService registryService;
    private final long intervalSeconds;
    private ScheduledExecutorService scheduler;

    public RegistryReporter(RegistryService registryService, long intervalSeconds) {
        this.registryService = registryService;
        this.intervalSeconds = intervalSeconds;
    }

    public void start() {
        if (scheduler != null) {
            return;
        }
        scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "registry-reporter");
            t.setDaemon(true);
            return t;
        });
        scheduler.scheduleAtFixedRate(() -> {
            String health = RegistryDiagnostics.health(registryService);
            String snapshot = RegistryDiagnostics.snapshot(registryService);
            System.out.println("[registry] " + health + ", " + snapshot);
        }, intervalSeconds, intervalSeconds, TimeUnit.SECONDS);
    }

    public void stop() {
        if (scheduler != null) {
            scheduler.shutdownNow();
            scheduler = null;
        }
    }
}
