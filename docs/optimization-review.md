# aoaojiao-rpc 项目优化建议文档

> 生成时间: 2026-04-27  
> 项目版本: 基于代码分析

---

## 一、RPC Core 模块

### 1.1 连接管理问题 ⚠️ 严重

| 文件 | 问题描述 | 影响 | 优先级 |
|------|----------|------|--------|
| [SimpleRpcClient.java](rpc-client/src/main/java/com/aoaojiao/rpc/client/SimpleRpcClient.java) | 每次调用都创建新的 RpcClient 连接 | 高并发下连接创建开销大，性能差 | 🔴 高 |
| [FailRetry.java](rpc-client/src/main/java/com/aoaojiao/rpc/client/cluster/fault/FailRetry.java) | 每次重试都新建连接，完全没有连接复用 | 重试效率低，雪崩风险高 | 🔴 高 |
| [RpcClient.java](rpc-core/src/main/java/com/aoaojiao/rpc/core/netty/RpcClient.java) | 缺少连接池机制 | 无法复用连接，高并发瓶颈 | 🔴 高 |

**建议**: 实现连接池管理，支持连接复用、心跳检测和自动重连。

### 1.2 序列化扩展性

| 文件 | 问题描述 | 影响 | 优先级 |
|------|----------|------|--------|
| [SerializerFactory.java](rpc-common/src/main/java/com/aoaojiao/rpc/common/serialization/SerializerFactory.java) | 只支持 Hessian 序列化，无法运行时扩展 | 不支持 JSON/Protobuf 等常见序列化协议 | 🟡 中 |

**建议**: 
- 添加 JSON/FastJSON、Kryo 等序列化支持
- 实现 SPI 机制支持运行时扩展

### 1.3 资源泄漏风险 ⚠️ 严重

| 文件 | 问题描述 | 影响 | 优先级 |
|------|----------|------|--------|
| [PendingRequests.java](rpc-core/src/main/java/com/aoaojiao/rpc/core/transport/PendingRequests.java) | 缺少请求超时自动清理机制 | 超时请求占用内存，可能导致 OOM | 🔴 高 |
| [RpcServerHandler.java](rpc-core/src/main/java/com/aoaojiao/rpc/core/netty/RpcServerHandler.java) | exceptionCaught 方法没有记录日志 | 异常信息丢失，难以排查问题 | 🟡 中 |

### 1.4 服务调用

| 文件 | 问题描述 | 影响 | 优先级 |
|------|----------|------|--------|
| [DefaultServiceInvoker.java](rpc-server/src/main/java/com/aoaojiao/rpc/server/invoker/DefaultServiceInvoker.java) | 使用 getMethod 而非 getDeclaredMethod | 可能匹配到父类方法，导致意外行为 | 🟡 中 |

**建议**: 使用 `service.getClass().getDeclaredMethod()` 确保精确匹配。

---

## 二、RPC Client 模块

### 2.1 负载均衡

| 文件 | 问题描述 | 影响 | 优先级 |
|------|----------|------|--------|
| [RoundRobinLoadBalancer.java](rpc-client/src/main/java/com/aoaojiao/rpc/client/cluster/impl/RoundRobinLoadBalancer.java) | AtomicInteger 存在 Integer.MAX_VALUE 溢出风险 | 长期运行后轮询算法失效 | 🟡 中 |

**建议**: 使用 `LongAdder` 或在溢出时重置计数器。

### 2.2 限流器

| 文件 | 问题描述 | 影响 | 优先级 |
|------|----------|------|--------|
| [FixedWindowRateLimiter.java](rpc-client/src/main/java/com/aoaojiao/rpc/client/cluster/limit/FixedWindowRateLimiter.java) | 固定窗口算法存在临界突刺问题 | 临界时刻请求量可能翻倍 | 🟡 中 |

**建议**: 改用滑动窗口算法或令牌桶算法（如 Google Guava RateLimiter）。

### 2.3 熔断器

| 文件 | 问题描述 | 影响 | 优先级 |
|------|----------|------|--------|
| [CircuitBreaker.java](rpc-client/src/main/java/com/aoaojiao/rpc/client/cluster/circuit/CircuitBreaker.java) | 只支持计数模式，无滑动窗口统计 | 无法基于错误率动态调整 | 🟡 中 |

**建议**: 实现滑动窗口统计模式，记录时间窗口内的错误率。

### 2.4 故障处理

| 文件 | 问题描述 | 影响 | 优先级 |
|------|----------|------|--------|
| [FailRetry.java](rpc-client/src/main/java/com/aoaojiao/rpc/client/cluster/fault/FailRetry.java) | 重试时每次都调用 loadBalancer 重新选择节点 | 可能重复选择同一节点，重试效果差 | 🟡 中 |
| [FailRetry.java](rpc-client/src/main/java/com/aoaojiao/rpc/client/cluster/fault/FailRetry.java) | 重试间隔无延迟，可能导致雪崩 | 快速重试加剧服务端压力 | 🟡 中 |

**建议**: 
- 实现指数退避策略
- 重试时排除上次失败的节点

---

## 三、RPC Registry 模块

### 3.1 服务发现缓存

| 文件 | 问题描述 | 影响 | 优先级 |
|------|----------|------|--------|
| [ServiceDiscovery.java](rpc-registry/src/main/java/com/aoaojiao/rpc/registry/ServiceDiscovery.java) | 缺少缓存 TTL 和主动刷新机制 | 服务变更感知延迟 | 🟡 中 |
| [LocalRegistryService.java](rpc-registry/src/main/java/com/aoaojiao/rpc/registry/LocalRegistryService.java) | 缺少服务实例健康检查能力 | 无法自动摘除不健康实例 | 🟡 中 |

### 3.2 Nacos 集成

| 文件 | 问题描述 | 影响 | 优先级 |
|------|----------|------|--------|
| [NacosRegistryService.java](rpc-registry/src/main/java/com/aoaojiao/rpc/registry/NacosRegistryService.java) | 缺少连接失败重试机制 | Nacos 不可用时无法恢复 | 🟡 中 |

---

## 四、Spring 集成

### 4.1 注解和配置

| 文件 | 问题描述 | 影响 | 优先级 |
|------|----------|------|--------|
| [RpcReferenceInjector.java](rpc-spring/src/main/java/com/aoaojiao/rpc/spring/support/RpcReferenceInjector.java) | BeanPostProcessor 对所有 Bean 遍历扫描，效率低 | Bean 数量多时启动慢 | 🟡 中 |
| [RpcSpringConfiguration.java](rpc-spring/src/main/java/com/aoaojiao/rpc/spring/config/RpcSpringConfiguration.java) | 配置项硬编码，无默认值提示 | 用户配置不友好 | 🟡 中 |

**建议**: 
- 使用 `@Autowired` + `ObjectProvider` 懒加载
- 提供完整的 application.yml 配置示例

### 4.2 Spring Boot Starter 完善

| 问题 | 建议 | 优先级 |
|------|------|--------|
| 缺少自动配置暴露指标端点 | 实现 `/actuator/metrics/rpc.*` 端点 | 🟡 中 |
| 缺少健康检查端点 | 实现 `/actuator/health/rpc` 端点 | 🟡 中 |

---

## 五、可观测性

### 5.1 指标体系

| 文件 | 问题描述 | 影响 | 优先级 |
|------|----------|------|--------|
| [MetricsRegistry.java](rpc-metrics/src/main/java/com/aoaojiao/rpc/metrics/MetricsRegistry.java) | 只有聚合值，缺少平均值/P99/P999 | 无法分析耗时分布 | 🟡 中 |
| [MetricsRegistry.java](rpc-metrics/src/main/java/com/aoaojiao/rpc/metrics/MetricsRegistry.java) | 输出格式简单，不兼容 Prometheus | 无法接入常见监控系统 | 🟡 中 |

**建议**: 
- 实现 Histogram 统计分布
- 输出 Prometheus/OpenTelemetry 格式

### 5.2 链路追踪

| 问题 | 建议 | 优先级 |
|------|------|--------|
| TraceId 生成但未使用 | 集成 SkyWalking/OpenTelemetry | 🟡 中 |

---

## 六、安全性

| 问题 | 建议 | 优先级 |
|------|------|--------|
| 传输层无 TLS 加密 | 支持 SSL/TLS | 🟡 中 |
| 无认证机制 | 实现 token/API Key 认证 | 🟡 中 |
| 无请求签名 | 实现请求签名防篡改 | 🟡 中 |

---

## 七、测试覆盖

| 模块 | 当前状态 | 建议 |
|------|----------|------|
| rpc-registry | 有部分单元测试 | 补充集成测试 |
| rpc-core | 无单元测试 | 补充 Netty Handler 测试 |
| rpc-client | 无单元测试 | 补充集群策略测试 |

---

## 八、优化优先级汇总

### 🔴 高优先级（影响核心功能）
1. **连接池实现** - 高并发性能瓶颈
2. **PendingRequests 超时清理** - 内存泄漏风险
3. **DefaultServiceInvoker 方法匹配** - 功能正确性

### 🟡 中优先级（影响稳定性和可维护性）
1. 序列化扩展（JSON/Protobuf）
2. 重试策略优化（指数退避）
3. 限流器算法改进
4. 可观测性增强
5. 单元测试覆盖

### 🟢 低优先级（长期改进）
1. 安全机制（TLS/认证）
2. 链路追踪集成
3. Spring Boot 3 兼容性

---

## 九、建议的后续工作

1. **短期**: 修复高优先级问题，特别是连接管理和内存泄漏
2. **中期**: 完善可观测性、测试覆盖、重试策略
3. **长期**: 安全机制、链路追踪、多协议支持

---

*文档由代码分析自动生成，如有必要可进一步补充详细方案。*
