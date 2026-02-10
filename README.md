# aoaojiao-rpc

作者：DD

## 项目简介
`aoaojiao-rpc` 是一个面向生产使用的 RPC 框架，提供稳定通信、服务治理、基础容错与可观测能力。当前实现覆盖自定义协议、Netty 长连接、注册与发现（Nacos 与本地内存注册中心）、负载均衡、容错、限流与熔断、Trace 透传与指标采集，并提供 Spring / Spring Boot 集成与示例工程。

## 功能清单
- 自定义 RPC 协议 + Netty 长连接
- 序列化（Hessian）
- 同步调用 + 超时控制
- 注册中心：Nacos / 本地内存注册中心（无 Nacos 也可用）
- 负载均衡：Random / RoundRobin
- 容错策略：FailFast / FailRetry（<=2）
- 限流与简版熔断
- TraceId 透传 + QPS/RT/错误数指标
- Spring 注解集成
- Spring Boot Starter + 自动配置
- 示例工程与压测入口

## 模块结构
- `rpc-common`：公共模型、协议、工具
- `rpc-core`：协议编解码、Netty 通信
- `rpc-client`：代理、负载均衡、容错
- `rpc-server`：服务暴露、线程模型
- `rpc-registry`：注册中心（Nacos + 本地）
- `rpc-metrics`：指标与 trace（简版）
- `rpc-spring`：Spring 集成
- `rpc-spring-boot-autoconfigure`：Spring Boot 自动配置
- `rpc-spring-boot-starter`：Spring Boot Starter
- `rpc-example`：示例工程聚合模块
  - `example-provider` / `example-consumer`
  - `example-boot-provider` / `example-boot-consumer`

## 使用文档
- `docs/usage.md`

## 快速开始（非 Boot 示例）
1. 可选：启动 Nacos（默认 `127.0.0.1:8848`）
2. 启动 Provider：
   - 运行 `rpc-example/example-provider` 的 `ProviderApp`
3. 启动 Consumer：
   - 运行 `rpc-example/example-consumer` 的 `ConsumerApp`

## 压测入口
- 运行 `rpc-example/example-consumer` 的 `LoadTestApp`
- 参数：`concurrency totalRequests timeoutMillis`

## Spring 使用
在 Spring 配置类上添加 `@EnableAoaojiaoRpc`，并在服务端与客户端使用注解：
- 服务端：`@RpcService`
- 客户端：`@RpcReference`

可配置项（部分）：
- `aoaojiao.rpc.server.host`
- `aoaojiao.rpc.server.port`
- `aoaojiao.rpc.server.businessThreads`
- `aoaojiao.rpc.registry.serverAddr`
- `aoaojiao.rpc.registry.group`
- `aoaojiao.rpc.registry.namespace`
- `aoaojiao.rpc.registry.enabled`
- `aoaojiao.rpc.client.timeoutMillis`

## Spring Boot Starter
引入 `rpc-spring-boot-starter` 即可自动装配，无需显式 `@EnableAoaojiaoRpc`。

Boot 示例模块：
- `rpc-example/example-boot-provider`
- `rpc-example/example-boot-consumer`

配置项由 `@ConfigurationProperties(prefix = "aoaojiao.rpc")` 映射，详见：
- `docs/boot-properties.md`

## 本地注册中心（无 Nacos）
当满足以下任一条件时自动回退本地注册中心：
- `aoaojiao.rpc.registry.enabled=false`
- `aoaojiao.rpc.registry.serverAddr` 为空
- `aoaojiao.rpc.registry.mode=local`

说明：
- `docs/local-registry.md`

## 一键脚本
- 启动 Boot 示例：`scripts/run-boot-examples.sh`
- 停止 Boot 示例：`scripts/stop-boot-examples.sh`
- 本地注册中心启动：`scripts/run-boot-examples-local.sh`
- Dev 启动（spring-boot:run）：`scripts/run-boot-examples-dev.sh`
- 切换注册中心模式：`scripts/switch-registry.sh local|nacos`

## 监控与诊断
- 监控说明：`docs/registry-monitor.md`
- 端点：`/health` `/snapshot` `/health.json` `/snapshot.json`

## 运行环境
- JDK 17
- Maven 3.8+
- 可选：Nacos 2.x

## 许可
内部学习与实验用途。若需开源发布，请补充 LICENSE。
