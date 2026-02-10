package com.aoaojiao.rpc.example.consumer;

import com.aoaojiao.rpc.client.RpcClientProxyFactory;
import com.aoaojiao.rpc.client.SimpleRpcClient;
import com.aoaojiao.rpc.example.api.UserService;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

public class LoadTestApp {
    public static void main(String[] args) throws Exception {
        int concurrency = args.length > 0 ? Integer.parseInt(args[0]) : 64;
        int totalRequests = args.length > 1 ? Integer.parseInt(args[1]) : 10000;
        long timeoutMillis = args.length > 2 ? Long.parseLong(args[2]) : 3000;

        SimpleRpcClient client = new SimpleRpcClient("127.0.0.1", 9000);
        client.start();

        RpcClientProxyFactory factory = new RpcClientProxyFactory(client, timeoutMillis);
        UserService userService = factory.create(UserService.class, "v1", "default");

        ExecutorService pool = Executors.newFixedThreadPool(concurrency);
        CountDownLatch latch = new CountDownLatch(totalRequests);
        AtomicLong success = new AtomicLong();
        AtomicLong failure = new AtomicLong();
        AtomicLong totalLatency = new AtomicLong();
        List<Future<?>> futures = new ArrayList<>(totalRequests);

        long start = System.nanoTime();
        for (int i = 0; i < totalRequests; i++) {
            futures.add(pool.submit(() -> {
                long begin = System.nanoTime();
                try {
                    userService.getUser(1L);
                    success.incrementAndGet();
                } catch (Exception ex) {
                    failure.incrementAndGet();
                } finally {
                    long cost = System.nanoTime() - begin;
                    totalLatency.addAndGet(cost);
                    latch.countDown();
                }
            }));
        }

        latch.await();
        long durationNs = System.nanoTime() - start;
        double durationSec = durationNs / 1_000_000_000.0;
        double qps = totalRequests / durationSec;
        double avgMs = (totalLatency.get() / 1_000_000.0) / totalRequests;

        System.out.println("concurrency=" + concurrency + ", totalRequests=" + totalRequests
                + ", success=" + success.get() + ", failure=" + failure.get());
        System.out.println("durationSec=" + String.format("%.3f", durationSec)
                + ", qps=" + String.format("%.2f", qps)
                + ", avgMs=" + String.format("%.3f", avgMs));

        pool.shutdown();
        pool.awaitTermination(5, TimeUnit.SECONDS);
        client.close();
    }
}
