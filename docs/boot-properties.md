# aoaojiao RPC Boot Properties

These properties are mapped via `@ConfigurationProperties(prefix = "aoaojiao.rpc")`.

## Registry
- `aoaojiao.rpc.registry.enabled` (boolean, default: true)
- `aoaojiao.rpc.registry.mode` (string, default: auto)  // auto|local|nacos
- `aoaojiao.rpc.registry.serverAddr` (string, default: 127.0.0.1:8848)
- `aoaojiao.rpc.registry.namespace` (string)
- `aoaojiao.rpc.registry.group` (string, default: DEFAULT_GROUP)
- `aoaojiao.rpc.registry.username` (string)
- `aoaojiao.rpc.registry.password` (string)

### Monitor
- `aoaojiao.rpc.registry.monitor.enabled` (boolean, default: true)
- `aoaojiao.rpc.registry.monitor.port` (int, default: 18080)
- `aoaojiao.rpc.registry.monitor.intervalSeconds` (int, default: 5)

## Server
- `aoaojiao.rpc.server.host` (string, default: 127.0.0.1)
- `aoaojiao.rpc.server.port` (int, default: 9000)
- `aoaojiao.rpc.server.businessThreads` (int, default: 8)

## Client
- `aoaojiao.rpc.client.timeoutMillis` (long, default: 3000)
- `aoaojiao.rpc.client.loadBalancer` (string, default: roundRobin)
- `aoaojiao.rpc.client.faultTolerance` (string, default: failFast)
- `aoaojiao.rpc.client.retryTimes` (int, default: 0)
- `aoaojiao.rpc.client.rateLimit` (long, default: 0)
- `aoaojiao.rpc.client.circuitFailureThreshold` (int, default: 3)
- `aoaojiao.rpc.client.circuitOpenMillis` (long, default: 3000)

## Validation
- `registry.serverAddr` and `registry.group` are required when `registry.enabled=true`
- `server.port`, `server.businessThreads`, `client.timeoutMillis`, `client.circuitFailureThreshold` must be >= 1

## Local Registry (No Nacos)
If `registry.enabled=false` or `registry.serverAddr` is blank, the framework falls back to the in-memory registry.
You can also force local via `registry.mode=local`.
