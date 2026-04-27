# AGENTS.md

This file provides guidance to Qoder (qoder.com) when working with code in this repository.

## 构建命令

```bash
# 全量构建（跳过测试）
mvn clean package -DskipTests

# 构建指定模块及其依赖
mvn -pl rpc-example/example-boot-provider,rpc-example/example-boot-consumer -am -DskipTests package

# 单模块构建
mvn -pl <module> -am package

# 运行单个测试
mvn test -Dtest=ClassName -pl <module>
```

## 模块架构

```
rpc-common          # 协议模型、序列化、工具类（无外部依赖）
    ├── protocol    # RpcProtocol（自定义二进制协议）、RpcRequest/Response
    ├── serialization # Serializer 接口、Hessian/JSON/Kryo 实现
    └── service    # RpcInvocation、ServiceKey

rpc-core            # Netty 通信层
    ├── codec      # RpcEncoder/RpcDecoder（基于 LengthFieldBasedFrameDecoder）
    ├── netty      # RpcClient/RpcServer（底层 Netty Bootstrap）
    ├── pool       # 连接池管理（PerNodeConnectionPool、ReusableRpcConnection）
    └── transport  # PendingRequests（请求-响应Future映射）

rpc-client          # 客户端代理与服务治理
    ├── SimpleRpcClient    # 简单单连接客户端
    └── cluster    # ClusterClient（整合以下治理组件）
        ├── LoadBalancer   # 负载均衡策略
        ├── fault/         # 容错策略（FailFast、FailRetry、SmartRetry）
        ├── limit/         # 限流器（FixedWindow、SlidingWindow、TokenBucket）
        └── circuit/       # 熔断器（SlidingWindowStats、CircuitBreaker）

rpc-server          # 服务端
    ├── provider   # ServiceProvider 接口
    └── invoker   # MethodCache、ServiceInvoker（方法调用）

rpc-registry        # 注册中心
    ├── RegistryService 接口（Nacos / Local 实现）
    ├── ServiceDiscovery 服务发现
    └── RegistryHttpServer 监控端点（/health、/snapshot）

rpc-spring          # Spring 注解集成
    ├── annotation  # @EnableAoaojiaoRpc、@RpcService、@RpcReference
    └── support     # RpcReferenceInjector（注入@RpcReference代理）

rpc-spring-boot-autoconfigure  # Spring Boot 自动配置
    └── properties  # AoaojiaoRpcProperties（配置前缀 aoaojiao.rpc）

rpc-example         # 示例工程
    ├── example-provider/consumer      # 纯 Spring 集成示例
    └── example-boot-provider/consumer # Spring Boot Starter 示例
```

## 协议格式

RpcProtocol 基于自定义二进制协议，使用 LengthFieldBasedFrameDecoder 解决粘包/拆包：
- Header: version(1) + messageType(1) + serialization(1) + status(1) + requestId(8) + bodyLength(4)
- Body: 序列化后的字节数组

## 注册中心模式

- **Nacos 模式**: `aoaojiao.rpc.registry.enabled=true`（默认连接 127.0.0.1:8848）
- **本地模式**: `aoaojiao.rpc.registry.enabled=false` 或 `registry.mode=local`，自动降级为 LocalRegistryService（内存注册）

## Boot 使用

1. 引入 starter: `rpc-spring-boot-starter`
2. 配置前缀: `aoaojiao.rpc.*`
3. 服务端注解: `@RpcService`
4. 客户端注解: `@RpcReference`

## 关键配置

| 配置路径 | 说明 | 默认值 |
|---------|------|--------|
| `aoaojiao.rpc.server.port` | 服务端口 | 9000 |
| `aoaojiao.rpc.client.timeoutMillis` | 调用超时 | 3000ms |
| `aoaojiao.rpc.client.pool.enabled` | 连接池开关 | false |
| `aoaojiao.rpc.client.rateLimitType` | 限流器类型 | fixedWindow |

## 示例运行

```bash
# 启动 Nacos 后运行 Boot 示例
scripts/run-boot-examples.sh

# 本地注册中心模式（无需 Nacos）
scripts/run-boot-examples-local.sh
```

## 监控端点

- `/health` - 健康检查
- `/snapshot` - 服务快照
- Provider 默认端口 18080，Consumer 默认端口 18081