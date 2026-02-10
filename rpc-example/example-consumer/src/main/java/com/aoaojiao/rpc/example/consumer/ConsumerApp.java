package com.aoaojiao.rpc.example.consumer;

import com.aoaojiao.rpc.client.RpcClientProxyFactory;
import com.aoaojiao.rpc.client.SimpleRpcClient;
import com.aoaojiao.rpc.client.cluster.ClusterClient;
import com.aoaojiao.rpc.client.cluster.LoadBalancer;
import com.aoaojiao.rpc.client.cluster.circuit.CircuitBreaker;
import com.aoaojiao.rpc.client.cluster.fault.FailFast;
import com.aoaojiao.rpc.client.cluster.fault.FaultTolerance;
import com.aoaojiao.rpc.client.cluster.impl.RoundRobinLoadBalancer;
import com.aoaojiao.rpc.client.cluster.limit.FixedWindowRateLimiter;
import com.aoaojiao.rpc.client.cluster.limit.RateLimiter;
import com.aoaojiao.rpc.common.service.ServiceKey;
import com.aoaojiao.rpc.example.api.User;
import com.aoaojiao.rpc.example.api.UserService;
import com.aoaojiao.rpc.registry.NacosRegistryService;
import com.aoaojiao.rpc.registry.RegistryConfig;
import com.aoaojiao.rpc.registry.ServiceDiscovery;

public class ConsumerApp {
    public static void main(String[] args) throws Exception {
        RegistryConfig config = new RegistryConfig();
        config.setServerAddr("127.0.0.1:8848");

        ServiceDiscovery discovery = new ServiceDiscovery(new NacosRegistryService(config));
        ServiceKey key = ServiceKey.of(UserService.class.getName(), "v1", "default");
        discovery.subscribe(key);

        LoadBalancer lb = new RoundRobinLoadBalancer();
        FaultTolerance ft = new FailFast();
        RateLimiter limiter = new FixedWindowRateLimiter(1000);
        CircuitBreaker breaker = new CircuitBreaker(3, 3000);

        ClusterClient clusterClient = new ClusterClient(discovery, lb, ft, limiter, breaker, 3000);

        SimpleRpcClient client = new SimpleRpcClient("127.0.0.1", 9000) {
            @Override
            public Object invoke(com.aoaojiao.rpc.common.service.RpcInvocation invocation, long timeoutMillis) throws Exception {
                return clusterClient.invoke(key, invocation);
            }
        };
        client.start();

        RpcClientProxyFactory factory = new RpcClientProxyFactory(client, 3000);
        UserService userService = factory.create(UserService.class, "v1", "default");
        User user = userService.getUser(1L);
        System.out.println("user=" + user.getId() + ", name=" + user.getName());

        client.close();
        discovery.close();
    }
}
