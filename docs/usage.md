# 使用文档（aoaojiao-rpc）

本文档覆盖三种使用方式：
- 纯 Java 示例（非 Spring）
- Spring 集成
- Spring Boot Starter

同时提供本地注册中心（无 Nacos）与监控端点的说明。

## 1. 纯 Java 使用（非 Spring）

### 启动 Provider
- 模块：`rpc-example/example-provider`
- 入口类：`ProviderApp`

### 启动 Consumer
- 模块：`rpc-example/example-consumer`
- 入口类：`ConsumerApp`

### 负载压测
- 模块：`rpc-example/example-consumer`
- 入口类：`LoadTestApp`
- 参数：`concurrency totalRequests timeoutMillis`

## 2. Spring 使用

### 开启注解
在 Spring 配置类上添加：
- `@EnableAoaojiaoRpc`

### 服务端注解
- `@RpcService`

### 客户端注解
- `@RpcReference`

## 3. Spring Boot Starter 使用

引入 `rpc-spring-boot-starter` 后自动装配，无需显式 `@EnableAoaojiaoRpc`。

Boot 示例模块：
- `rpc-example/example-boot-provider`
- `rpc-example/example-boot-consumer`

### 配置前缀
- `aoaojiao.rpc.*`

详细配置项：
- `docs/boot-properties.md`

## 4. 本地注册中心（无 Nacos）

当满足以下任一条件时自动回退本地注册中心：
- `aoaojiao.rpc.registry.enabled=false`
- `aoaojiao.rpc.registry.serverAddr` 为空
- `aoaojiao.rpc.registry.mode=local`

详见：
- `docs/local-registry.md`

## 5. 监控与诊断

监控端点：
- `/health` `/snapshot`
- `/health.json` `/snapshot.json`

详见：
- `docs/registry-monitor.md`

## 6. 一键脚本
- 启动 Boot 示例：`scripts/run-boot-examples.sh`
- 停止 Boot 示例：`scripts/stop-boot-examples.sh`
- 本地注册中心启动：`scripts/run-boot-examples-local.sh`
- Dev 启动（spring-boot:run）：`scripts/run-boot-examples-dev.sh`
- 切换注册中心模式：`scripts/switch-registry.sh local|nacos`
