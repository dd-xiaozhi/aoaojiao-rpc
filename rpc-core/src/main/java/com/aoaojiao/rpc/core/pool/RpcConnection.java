package com.aoaojiao.rpc.core.pool;

import com.aoaojiao.rpc.core.transport.PendingRequests;
import io.netty.channel.Channel;

/**
 * RPC 连接接口
 * 代表一个可复用的 RPC 连接
 */
public interface RpcConnection extends AutoCloseable {

    /**
     * 获取底层 Netty Channel
     */
    Channel getChannel();

    /**
     * 检查连接是否可用
     */
    boolean isConnected();

    /**
     * 检查连接是否正在关闭
     */
    boolean isClosing();

    /**
     * 获取连接创建时间戳
     */
    long getCreatedTime();

    /**
     * 获取最后活跃时间
     */
    long getLastActiveTime();

    /**
     * 更新最后活跃时间
     */
    void touch();

    /**
     * 获取该连接关联的 PendingRequests
     */
    PendingRequests getPendingRequests();

    /**
     * 获取连接的唯一标识
     */
    String getConnectionId();

    /**
     * 增加引用计数
     * @return 新的引用计数
     */
    int retain();

    /**
     * 减少引用计数
     * 当引用计数归零时，连接可以被归还到连接池
     */
    void release();

    /**
     * 获取当前引用计数
     */
    int getRefCount();

    /**
     * 标记连接为不可用
     * 通常在检测到连接异常时调用
     */
    void markUnusable();

    /**
     * 检查连接是否可用（引用计数大于0且已连接）
     */
    default boolean isUsable() {
        return isConnected() && !isClosing() && getRefCount() > 0;
    }

    @Override
    void close();
}