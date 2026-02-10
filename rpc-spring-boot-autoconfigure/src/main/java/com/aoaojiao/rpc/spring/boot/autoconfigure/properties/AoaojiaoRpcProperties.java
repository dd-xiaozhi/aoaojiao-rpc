package com.aoaojiao.rpc.spring.boot.autoconfigure.properties;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "aoaojiao.rpc")
public class AoaojiaoRpcProperties {
    private final Registry registry = new Registry();
    private final Server server = new Server();
    private final Client client = new Client();

    public Registry getRegistry() {
        return registry;
    }

    public Server getServer() {
        return server;
    }

    public Client getClient() {
        return client;
    }

    public static class Registry {
        private boolean enabled = true;
        private String serverAddr = "127.0.0.1:8848";
        private String namespace;
        private String group = "DEFAULT_GROUP";
        private String username;
        private String password;
        private String mode = "auto";
        private final Monitor monitor = new Monitor();

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getServerAddr() {
            return serverAddr;
        }

        public void setServerAddr(String serverAddr) {
            this.serverAddr = serverAddr;
        }

        public String getNamespace() {
            return namespace;
        }

        public void setNamespace(String namespace) {
            this.namespace = namespace;
        }

        public String getGroup() {
            return group;
        }

        public void setGroup(String group) {
            this.group = group;
        }

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }

        public String getMode() {
            return mode;
        }

        public void setMode(String mode) {
            this.mode = mode;
        }

        public Monitor getMonitor() {
            return monitor;
        }

        @AssertTrue(message = "registry.serverAddr and registry.group must be set when registry.enabled=true")
        public boolean isValidWhenEnabled() {
            if (!enabled) {
                return true;
            }
            return serverAddr != null && !serverAddr.isBlank()
                    && group != null && !group.isBlank();
        }
    }

    public static class Monitor {
        private boolean enabled = true;

        @Min(1)
        private int port = 18080;

        @Min(1)
        private int intervalSeconds = 5;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public int getPort() {
            return port;
        }

        public void setPort(int port) {
            this.port = port;
        }

        public int getIntervalSeconds() {
            return intervalSeconds;
        }

        public void setIntervalSeconds(int intervalSeconds) {
            this.intervalSeconds = intervalSeconds;
        }
    }

    public static class Server {
        @NotBlank
        private String host = "127.0.0.1";

        @Min(1)
        private int port = 9000;

        @Min(1)
        private int businessThreads = 8;

        public String getHost() {
            return host;
        }

        public void setHost(String host) {
            this.host = host;
        }

        public int getPort() {
            return port;
        }

        public void setPort(int port) {
            this.port = port;
        }

        public int getBusinessThreads() {
            return businessThreads;
        }

        public void setBusinessThreads(int businessThreads) {
            this.businessThreads = businessThreads;
        }
    }

    public static class Client {
        @Min(1)
        private long timeoutMillis = 3000;

        private String loadBalancer = "roundRobin";
        private String faultTolerance = "failFast";

        @Min(0)
        private int retryTimes = 0;

        @Min(0)
        private long rateLimit = 0;

        @Min(1)
        private int circuitFailureThreshold = 3;

        @Min(100)
        private long circuitOpenMillis = 3000;

        public long getTimeoutMillis() {
            return timeoutMillis;
        }

        public void setTimeoutMillis(long timeoutMillis) {
            this.timeoutMillis = timeoutMillis;
        }

        public String getLoadBalancer() {
            return loadBalancer;
        }

        public void setLoadBalancer(String loadBalancer) {
            this.loadBalancer = loadBalancer;
        }

        public String getFaultTolerance() {
            return faultTolerance;
        }

        public void setFaultTolerance(String faultTolerance) {
            this.faultTolerance = faultTolerance;
        }

        public int getRetryTimes() {
            return retryTimes;
        }

        public void setRetryTimes(int retryTimes) {
            this.retryTimes = retryTimes;
        }

        public long getRateLimit() {
            return rateLimit;
        }

        public void setRateLimit(long rateLimit) {
            this.rateLimit = rateLimit;
        }

        public int getCircuitFailureThreshold() {
            return circuitFailureThreshold;
        }

        public void setCircuitFailureThreshold(int circuitFailureThreshold) {
            this.circuitFailureThreshold = circuitFailureThreshold;
        }

        public long getCircuitOpenMillis() {
            return circuitOpenMillis;
        }

        public void setCircuitOpenMillis(long circuitOpenMillis) {
            this.circuitOpenMillis = circuitOpenMillis;
        }
    }
}
