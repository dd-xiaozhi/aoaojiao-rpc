package com.aoaojiao.rpc.example.boot.consumer;

import com.aoaojiao.rpc.example.api.User;
import com.aoaojiao.rpc.example.api.UserService;
import com.aoaojiao.rpc.spring.annotation.RpcReference;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class UserCaller implements CommandLineRunner {
    @RpcReference(version = "v1", group = "default", timeoutMillis = 2000,
            loadBalancer = "random", faultTolerance = "failRetry", retryTimes = 1,
            rateLimit = 100, circuitFailureThreshold = 2, circuitOpenMillis = 2000)
    private UserService userService;

    @Override
    public void run(String... args) {
        User user = userService.getUser(1L);
        System.out.println("user=" + user.getId() + ", name=" + user.getName());
    }
}
