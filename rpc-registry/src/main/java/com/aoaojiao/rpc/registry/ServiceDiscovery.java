package com.aoaojiao.rpc.registry;

import com.aoaojiao.rpc.common.service.ServiceKey;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.lang.Iterable;

/**
 * 服务发现管理器
 * 支持缓存 TTL 和主动刷新机制
 */
public class ServiceDiscovery implements AutoCloseable {

    private final RegistryService registryService;
    private final Map<String, CacheEntry<List<ServiceInstance>>> cache = new ConcurrentHashMap<>();
    private final ScheduledExecutorService cleaner;
    private final long cacheTtlMillis;
    private final long refreshIntervalMillis;

    /**
     * 缓存条目
     */
    private static class CacheEntry<T> {
        final T value;
        final long createTime;
        final AtomicLong lastAccessTime;

        CacheEntry(T value) {
            this.value = value;
            this.createTime = System.currentTimeMillis();
            this.lastAccessTime = new AtomicLong(createTime);
        }

        boolean isExpired(long ttlMillis) {
            return System.currentTimeMillis() - createTime > ttlMillis;
        }

        void touch() {
            lastAccessTime.set(System.currentTimeMillis());
        }
    }

    /**
     * 创建服务发现实例（默认配置）
     */
    public ServiceDiscovery(RegistryService registryService) {
        this(registryService, 30000, 10000); // 默认 30s TTL，10s 刷新间隔
    }

    /**
     * 创建服务发现实例（自定义配置）
     */
    public ServiceDiscovery(RegistryService registryService, long cacheTtlMillis, long refreshIntervalMillis) {
        this.registryService = registryService;
        this.cacheTtlMillis = cacheTtlMillis;
        this.refreshIntervalMillis = refreshIntervalMillis;
        this.cleaner = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "service-discovery-cleaner");
            t.setDaemon(true);
            return t;
        });

        // 启动定时清理任务
        cleaner.scheduleWithFixedDelay(
                this::cleanExpiredCache,
                refreshIntervalMillis,
                refreshIntervalMillis,
                TimeUnit.MILLISECONDS
        );
    }

    /**
     * 获取服务实例列表
     * @param key 服务键
     * @return 服务实例列表
     */
    public List<ServiceInstance> getInstances(ServiceKey key) throws Exception {
        String keyStr = key.key();
        CacheEntry<List<ServiceInstance>> entry = cache.get(keyStr);

        // 检查缓存是否存在且未过期
        if (entry != null) {
            entry.touch();
            if (!entry.isExpired(cacheTtlMillis)) {
                return entry.value;
            }
        }

        // 缓存过期或不存在，从注册中心获取
        List<ServiceInstance> instances = registryService.discover(key);

        // 更新缓存
        if (instances != null && !instances.isEmpty()) {
            cache.put(keyStr, new CacheEntry<>(instances));
        }

        return instances;
    }

    /**
     * 获取缓存的服务实例（不触发刷新）
     */
    public List<ServiceInstance> getCachedInstances(ServiceKey key) {
        CacheEntry<List<ServiceInstance>> entry = cache.get(key.key());
        if (entry != null) {
            entry.touch();
            return entry.value;
        }
        return null;
    }

    /**
     * 订阅服务变更
     */
    public void subscribe(ServiceKey key) throws Exception {
        String keyStr = key.key();
        registryService.subscribe(key, instances -> {
            if (instances != null) {
                cache.put(keyStr, new CacheEntry<>(instances));
            } else {
                cache.remove(keyStr);
            }
        });
    }

    /**
     * 订阅服务变更（带回调）
     */
    public void subscribe(ServiceKey key, ServiceChangeListener listener) throws Exception {
        String keyStr = key.key();
        registryService.subscribe(key, instances -> {
            cache.put(keyStr, new CacheEntry<>(instances));
            if (listener != null) {
                listener.onChange(key, instances);
            }
        });
    }

    /**
     * 移除服务缓存
     */
    public void remove(ServiceKey key) {
        cache.remove(key.key());
    }

    /**
     * 手动刷新指定服务的缓存
     */
    public void refresh(ServiceKey key) throws Exception {
        List<ServiceInstance> instances = registryService.discover(key);
        if (instances != null && !instances.isEmpty()) {
            cache.put(key.key(), new CacheEntry<>(instances));
        }
    }

    /**
     * 刷新所有缓存
     * 注意：需要外部提供 ServiceKey 列表
     */
    public void refreshAll(Iterable<ServiceKey> keys) throws Exception {
        for (ServiceKey key : keys) {
            try {
                refresh(key);
            } catch (Exception e) {
                // 忽略单个服务的刷新失败
            }
        }
    }

    /**
     * 清理过期缓存
     */
    private void cleanExpiredCache() {
        long now = System.currentTimeMillis();
        for (Map.Entry<String, CacheEntry<List<ServiceInstance>>> entry : cache.entrySet()) {
            if (entry.getValue().isExpired(cacheTtlMillis)) {
                cache.remove(entry.getKey());
            }
        }
    }

    /**
     * 获取缓存大小
     */
    public int getCacheSize() {
        return cache.size();
    }

    /**
     * 检查缓存是否存在
     */
    public boolean hasCached(ServiceKey key) {
        return cache.containsKey(key.key());
    }

    /**
     * 清空所有缓存
     */
    public void clearCache() {
        cache.clear();
    }

    /**
     * 关闭服务发现
     */
    @Override
    public void close() {
        cleaner.shutdown();
        try {
            if (!cleaner.awaitTermination(5, TimeUnit.SECONDS)) {
                cleaner.shutdownNow();
            }
        } catch (InterruptedException e) {
            cleaner.shutdownNow();
            Thread.currentThread().interrupt();
        }
        cache.clear();
        registryService.close();
    }

    /**
     * 服务变更监听器
     */
    public interface ServiceChangeListener {
        void onChange(ServiceKey key, List<ServiceInstance> instances);
    }
}