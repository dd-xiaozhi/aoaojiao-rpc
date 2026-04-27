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

        private String rateLimitType = "fixedWindow";

        @Min(1)
        private int circuitFailureThreshold = 3;

        @Min(100)
        private long circuitOpenMillis = 3000;

        private String circuitBreakerMode = "counting";

        private final Pool pool = new Pool();
        private final Retry retry = new Retry();

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

        public String getRateLimitType() {
            return rateLimitType;
        }

        public void setRateLimitType(String rateLimitType) {
            this.rateLimitType = rateLimitType;
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

        public String getCircuitBreakerMode() {
            return circuitBreakerMode;
        }

        public void setCircuitBreakerMode(String circuitBreakerMode) {
            this.circuitBreakerMode = circuitBreakerMode;
        }

        public Pool getPool() {
            return pool;
        }

        public Retry getRetry() {
            return retry;
        }

        /**
         * 连接池配置
         */
        public static class Pool {
            private boolean enabled = false;

            @Min(1)
            private int maxConnections = 8;

            @Min(0)
            private int minIdle = 2;

            @Min(1000)
            private long idleTimeoutMillis = 60000;

            @Min(100)
            private long acquireTimeoutMillis = 5000;

            @Min(1000)
            private long heartbeatIntervalMillis = 30000;

            private int maxHeartbeatMisses = 3;

            public boolean isEnabled() {
                return enabled;
            }

            public void setEnabled(boolean enabled) {
                this.enabled = enabled;
            }

            public int getMaxConnections() {
                return maxConnections;
            }

            public void setMaxConnections(int maxConnections) {
                this.maxConnections = maxConnections;
            }

            public int getMinIdle() {
                return minIdle;
            }

            public void setMinIdle(int minIdle) {
                this.minIdle = minIdle;
            }

            public long getIdleTimeoutMillis() {
                return idleTimeoutMillis;
            }

            public void setIdleTimeoutMillis(long idleTimeoutMillis) {
                this.idleTimeoutMillis = idleTimeoutMillis;
            }

            public long getAcquireTimeoutMillis() {
                return acquireTimeoutMillis;
            }

            public void setAcquireTimeoutMillis(long acquireTimeoutMillis) {
                this.acquireTimeoutMillis = acquireTimeoutMillis;
            }

            public long getHeartbeatIntervalMillis() {
                return heartbeatIntervalMillis;
            }

            public void setHeartbeatIntervalMillis(long heartbeatIntervalMillis) {
                this.heartbeatIntervalMillis = heartbeatIntervalMillis;
            }

            public int getMaxHeartbeatMisses() {
                return maxHeartbeatMisses;
            }

            public void setMaxHeartbeatMisses(int maxHeartbeatMisses) {
                this.maxHeartbeatMisses = maxHeartbeatMisses;
            }
        }

        /**
         * 重试策略配置
         */
        public static class Retry {
            private boolean enabled = true;
            private boolean excludeFailedNodes = true;

            @Min(0)
            private long baseIntervalMillis = 100;

            @Min(100)
            private long maxIntervalMillis = 5000;

            @Min(0)
            private long jitterMillis = 50;

            public boolean isEnabled() {
                return enabled;
            }

            public void setEnabled(boolean enabled) {
                this.enabled = enabled;
            }

            public boolean isExcludeFailedNodes() {
                return excludeFailedNodes;
            }

            public void setExcludeFailedNodes(boolean excludeFailedNodes) {
                this.excludeFailedNodes = excludeFailedNodes;
            }

            public long getBaseIntervalMillis() {
                return baseIntervalMillis;
            }

            public void setBaseIntervalMillis(long baseIntervalMillis) {
                this.baseIntervalMillis = baseIntervalMillis;
            }

            public long getMaxIntervalMillis() {
                return maxIntervalMillis;
            }

            public void setMaxIntervalMillis(long maxIntervalMillis) {
                this.maxIntervalMillis = maxIntervalMillis;
            }

            public long getJitterMillis() {
                return jitterMillis;
            }

            public void setJitterMillis(long jitterMillis) {
                this.jitterMillis = jitterMillis;
            }
        }
    }
}
