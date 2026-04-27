package com.aoaojiao.rpc.core.pool;

import com.aoaojiao.rpc.common.protocol.RpcProtocol;

/**
 * 连接池管理器接口
 * 管理到所有服务端节点的连接池
 */
public interface ConnectionPoolManager extends AutoCloseable {

    /**
     * 获取或创建到指定节点的连接
     * @param host 主机地址
     * @param port 端口号
     * @return 可用的 RPC 连接
     * @throws Exception 如果获取连接失败
     */
    RpcConnection getConnection(String host, int port) throws Exception;

    /**
     * 获取或创建到指定节点的连接，带超时
     * @param host 主机地址
     * @param port 端口号
     * @param timeoutMillis 超时时间（毫秒）
     * @return 可用的 RPC 连接
     * @throws Exception 如果获取连接失败或超时
     */
    RpcConnection getConnection(String host, int port, long timeoutMillis) throws Exception;

    /**
     * 归还连接到连接池
     * @param host 主机地址
     * @param port 端口号
     * @param connection 要归还的连接
     */
    void returnConnection(String host, int port, RpcConnection connection);

    /**
     * 归还连接到连接池（异步）
     * @param host 主机地址
     * @param port 端口号
     * @param connection 要归还的连接
     * @param callback 归还完成后的回调
     */
    default void returnConnectionAsync(String host, int port, RpcConnection connection, Runnable callback) {
        try {
            returnConnection(host, port, connection);
            if (callback != null) {
                callback.run();
            }
        } catch (Exception e) {
            // 日志记录
            if (callback != null) {
                callback.run();
            }
        }
    }

    /**
     * 关闭并移除到指定节点的所有连接
     * @param host 主机地址
     * @param port 端口号
     */
    void closeNodeConnections(String host, int port);

    /**
     * 标记指定节点的连接为不可用
     * @param host 主机地址
     * @param port 端口号
     * @param connection 需要移除的连接
     */
    void markConnectionUnusable(String host, int port, RpcConnection connection);

    /**
     * 获取连接池统计信息
     * @param host 主机地址
     * @param port 端口号
     * @return 连接池统计信息
     */
    PoolStats getStats(String host, int port);

    /**
     * 获取连接池统计信息（所有节点汇总）
     * @return 所有节点的连接池统计信息
     */
    PoolStats getTotalStats();

    /**
     * 获取节点键
     * @param host 主机地址
     * @param port 端口号
     * @return 节点键，格式为 "host:port"
     */
    default String getNodeKey(String host, int port) {
        return host + ":" + port;
    }

    /**
     * 检查连接池是否已启用
     */
    boolean isEnabled();

    /**
     * 预热连接池
     * 预先创建最小数量的空闲连接
     */
    default void warmUp() {
        // 默认实现为空
    }

    @Override
    void close();
}