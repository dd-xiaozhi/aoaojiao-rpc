# aoaojiao-rpc

Java RPC 框架与服务治理基础设施
中文 | English（预留）
快速开始 • 主要特性 • 部署 • 文档 • 帮助

---

## 📝 项目说明

> Note
> 
> 本项目为生产级 RPC 框架实现，聚焦稳定通信、服务治理、基础容错与可观测能力。

> Important
> 
> - 本项目提供参考实现与工程化范式，建议在测试环境验证后再用于生产。
> - 如需对外提供服务，请遵循相关法律法规与合规要求。

作者：DD

---

## 🚀 快速开始

### 本地运行（非 Boot）
1. 可选：启动 Nacos（默认 `127.0.0.1:8848`）
2. 启动 Provider：`rpc-example/example-provider` 的 `ProviderApp`
3. 启动 Consumer：`rpc-example/example-consumer` 的 `ConsumerApp`

### Boot 示例（推荐）
- 启动：`scripts/run-boot-examples.sh`
- 停止：`scripts/stop-boot-examples.sh`
- 本地注册中心：`scripts/run-boot-examples-local.sh`
- Dev 模式：`scripts/run-boot-examples-dev.sh`

---

## 📚 文档

快速导航：

分类 | 说明
--- | ---
🚀 使用文档 | `docs/usage.md`
⚙️ Boot 配置 | `docs/boot-properties.md`
📡 注册中心监控 | `docs/registry-monitor.md`
🧪 本地注册中心 | `docs/local-registry.md`

---

## ✨ 主要特性

### 🎨 核心能力

特性 | 说明
--- | ---
📡 通信内核 | 自定义协议 + Netty 长连接
🔒 序列化 | Hessian
⏱ 调用模型 | 同步调用 + 超时控制
🧭 服务治理 | 注册发现 + 负载均衡 + 容错
🛡 稳定性 | 限流 + 简版熔断
📊 可观测 | TraceId 透传 + QPS/RT/错误数
🧩 集成 | Spring 注解 + Boot Starter

### 🧰 治理策略
- 负载均衡：Random / RoundRobin
- 容错策略：FailFast / FailRetry（<=2）

---

## 🧩 模块结构
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

---

## 🚢 部署方式

### Nacos 模式（默认）
通过 `aoaojiao.rpc.registry.serverAddr` 配置注册中心地址。

### 本地注册中心（无 Nacos）
满足以下任一条件会自动回退本地内存注册中心：
- `aoaojiao.rpc.registry.enabled=false`
- `aoaojiao.rpc.registry.serverAddr` 为空
- `aoaojiao.rpc.registry.mode=local`

---

## 🧩 Spring / Boot 使用

### Spring
- 启用注解：`@EnableAoaojiaoRpc`
- 服务端：`@RpcService`
- 客户端：`@RpcReference`

### Spring Boot
引入 `rpc-spring-boot-starter` 即可自动装配，无需显式 `@EnableAoaojiaoRpc`。

配置前缀：`aoaojiao.rpc.*`

---

## 📈 监控与诊断
- 端点：`/health` `/snapshot` `/health.json` `/snapshot.json`
- 示例端口：`18080`（provider）、`18081`（consumer）

---

## 🧪 压测入口
- 运行 `rpc-example/example-consumer` 的 `LoadTestApp`
- 参数：`concurrency totalRequests timeoutMillis`

---

## 🧰 运行环境
- JDK 17
- Maven 3.8+
- 可选：Nacos 2.x

---

## 💬 帮助支持
- 请优先查看 `docs/usage.md`
- 可在项目内提交问题或改进建议

---

## 📄 许可
内部学习与实验用途。若需开源发布，请补充 LICENSE。
