package com.aoaojiao.rpc.example.boot.provider;

import com.aoaojiao.rpc.registry.RegistryDiagnostics;
import com.aoaojiao.rpc.registry.RegistryService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class RegistryDiagnosticsRunner implements CommandLineRunner {
    private final RegistryService registryService;

    public RegistryDiagnosticsRunner(RegistryService registryService) {
        this.registryService = registryService;
    }

    @Override
    public void run(String... args) {
        System.out.println("registry health: " + RegistryDiagnostics.health(registryService));
        System.out.println("registry snapshot: " + RegistryDiagnostics.snapshot(registryService));
    }
}
