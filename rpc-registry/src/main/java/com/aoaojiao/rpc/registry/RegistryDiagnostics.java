package com.aoaojiao.rpc.registry;

import java.util.List;

public final class RegistryDiagnostics {
    private RegistryDiagnostics() {
    }

    public static String health(RegistryService registryService) {
        if (registryService instanceof RegistryHealthIndicator indicator) {
            return indicator.report();
        }
        return "health=UNKNOWN";
    }

    public static String snapshot(RegistryService registryService) {
        if (registryService instanceof LocalRegistryService local) {
            return local.snapshot();
        }
        return "snapshot=UNSUPPORTED";
    }

    public static String healthJson(RegistryService registryService) {
        String status = "UNKNOWN";
        if (registryService instanceof RegistryHealthIndicator indicator) {
            status = indicator.isHealthy() ? "UP" : "DOWN";
        }
        return "{\"status\":\"" + status + "\"}";
    }

    public static String snapshotJson(RegistryService registryService) {
        if (registryService instanceof LocalRegistryService local) {
            return local.snapshotJson();
        }
        return "{\"snapshot\":\"UNSUPPORTED\"}";
    }

    static String instancesJson(List<ServiceInstance> instances) {
        StringBuilder sb = new StringBuilder();
        sb.append('[');
        for (int i = 0; i < instances.size(); i++) {
            ServiceInstance si = instances.get(i);
            sb.append("{\"host\":\"").append(si.getHost()).append("\",")
              .append("\"port\":").append(si.getPort())
              .append("}");
            if (i < instances.size() - 1) {
                sb.append(',');
            }
        }
        sb.append(']');
        return sb.toString();
    }
}
