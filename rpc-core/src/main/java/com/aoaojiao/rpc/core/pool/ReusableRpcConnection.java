package com.aoaojiao.rpc.core.pool;

import com.aoaojiao.rpc.core.codec.RpcDecoder;
import com.aoaojiao.rpc.core.codec.RpcEncoder;
import com.aoaojiao.rpc.core.codec.RpcFrameDecoder;
import com.aoaojiao.rpc.core.netty.RpcClientHandler;
import com.aoaojiao.rpc.core.transport.PendingRequests;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.timeout.IdleStateHandler;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 可复用 RPC 连接实现
 * 支持引用计数管理连接生命周期
 */
public class ReusableRpcConnection implements RpcConnection {

    private final String host;
    private final int port;
    private final String connectionId;
    private final PoolConfig config;
    private final EventLoopGroup sharedGroup;
    private final PerNodeConnectionPool ownerPool;

    private Channel channel;
    private PendingRequests pendingRequests;
    private RpcClientHandler clientHandler;
    private EventLoopGroup ownGroup; // 仅在非共享模式时使用

    private final AtomicInteger refCount = new AtomicInteger(0);
    private final AtomicLong activeCount = new AtomicLong(0);
    private volatile boolean closing = false;
    private volatile boolean unusable = false;

    private final long createdTime;
    private volatile long lastActiveTime;

    private final AtomicLong totalRequests = new AtomicLong(0);
    private final AtomicLong failedRequests = new AtomicLong(0);

    public ReusableRpcConnection(String host, int port, PoolConfig config,
                                  EventLoopGroup sharedGroup, PerNodeConnectionPool ownerPool) {
        this.host = host;
        this.port = port;
        this.config = config;
        this.sharedGroup = sharedGroup;
        this.ownerPool = ownerPool;
        this.connectionId = host + ":" + port + "-" + System.nanoTime();
        this.createdTime = System.currentTimeMillis();
        this.lastActiveTime = createdTime;
    }

    /**
     * 连接到服务端
     */
    public void connect() throws InterruptedException {
        if (closing) {
            throw new IllegalStateException("Connection is closing");
        }

        // 如果是共享 EventLoopGroup，直接使用；否则创建新的
        if (sharedGroup != null) {
            connectInternal(sharedGroup);
        } else {
            ownGroup = new NioEventLoopGroup();
            connectInternal(ownGroup);
        }
    }

    /**
     * 连接到服务端（带超时）
     */
    public void connect(long timeoutMillis) throws InterruptedException, TimeoutException {
        if (closing) {
            throw new IllegalStateException("Connection is closing");
        }

        EventLoopGroup group = sharedGroup != null ? sharedGroup : new NioEventLoopGroup();
        try {
            connectInternalWithTimeout(group, timeoutMillis);
        } finally {
            if (sharedGroup == null) {
                // 非共享模式，由调用方管理 group 生命周期
            }
        }
    }

    private void connectInternal(EventLoopGroup group) throws InterruptedException {
        Bootstrap bootstrap = createBootstrap(group);
        ChannelFuture future = bootstrap.connect(host, port).sync();
        initChannel(future.channel());
    }

    private void connectInternalWithTimeout(EventLoopGroup group, long timeoutMillis)
            throws InterruptedException, TimeoutException {
        Bootstrap bootstrap = createBootstrap(group);
        ChannelFuture future = bootstrap.connect(host, port);

        // 等待连接完成或超时
        if (!future.await(timeoutMillis, TimeUnit.MILLISECONDS)) {
            throw new TimeoutException("Connection to " + host + ":" + port + " timeout");
        }

        if (!future.isSuccess()) {
            throw new RuntimeException("Failed to connect to " + host + ":" + port, future.cause());
        }

        initChannel(future.channel());
    }

    private Bootstrap createBootstrap(EventLoopGroup group) {
        Bootstrap bootstrap = new Bootstrap();
        this.pendingRequests = new PendingRequests();
        this.clientHandler = new RpcClientHandler(pendingRequests);

        return bootstrap.group(group)
                .channel(NioSocketChannel.class)
                .option(ChannelOption.TCP_NODELAY, true)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, (int) config.getConnectTimeoutMillis())
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) {
                        ch.pipeline()
                                .addLast(new RpcFrameDecoder())
                                .addLast(new RpcDecoder())
                                .addLast(new RpcEncoder())
                                .addLast(new IdleStateHandler(0, 30, 0))
                                .addLast(clientHandler);
                    }
                });
    }

    private void initChannel(Channel channel) {
        this.channel = channel;
        this.lastActiveTime = System.currentTimeMillis();

        // 注册连接断连监听器
        channel.closeFuture().addListener(future -> {
            // 连接断开时通知连接池
            if (ownerPool != null && !closing) {
                ownerPool.onConnectionClosed(this);
            }
        });
    }

    @Override
    public Channel getChannel() {
        return channel;
    }

    @Override
    public boolean isConnected() {
        return channel != null && channel.isActive();
    }

    @Override
    public boolean isClosing() {
        return closing;
    }

    @Override
    public long getCreatedTime() {
        return createdTime;
    }

    @Override
    public long getLastActiveTime() {
        return lastActiveTime;
    }

    @Override
    public void touch() {
        this.lastActiveTime = System.currentTimeMillis();
    }

    @Override
    public PendingRequests getPendingRequests() {
        return pendingRequests;
    }

    @Override
    public String getConnectionId() {
        return connectionId;
    }

    @Override
    public int retain() {
        int rc = refCount.incrementAndGet();
        activeCount.incrementAndGet();
        return rc;
    }

    @Override
    public void release() {
        int rc = refCount.decrementAndGet();
        if (rc == 0) {
            // 引用计数归零，通知连接池可以复用
            if (ownerPool != null && !closing && !unusable) {
                ownerPool.returnConnection(this);
            }
        }
    }

    @Override
    public int getRefCount() {
        return refCount.get();
    }

    @Override
    public void markUnusable() {
        this.unusable = true;
        if (ownerPool != null) {
            ownerPool.markConnectionUnusable(this);
        }
    }

    @Override
    public void close() {
        if (closing) {
            return;
        }
        closing = true;

        // 关闭 Channel
        if (channel != null) {
            channel.close();
        }

        // 关闭专属的 EventLoopGroup
        if (ownGroup != null) {
            ownGroup.shutdownGracefully();
        }
    }

    // ========== 辅助方法 ==========

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public EventLoopGroup getSharedGroup() {
        return sharedGroup;
    }

    public boolean isUnusable() {
        return unusable;
    }

    public long getTotalRequests() {
        return totalRequests.get();
    }

    public long getFailedRequests() {
        return failedRequests.get();
    }

    public void incrementRequestCount() {
        totalRequests.incrementAndGet();
    }

    public void incrementFailedCount() {
        failedRequests.incrementAndGet();
    }

    public long getActiveCount() {
        return activeCount.get();
    }

    @Override
    public String toString() {
        return String.format("ReusableRpcConnection[id=%s, host=%s, port=%d, refCount=%d, connected=%s]",
                connectionId, host, port, refCount.get(), isConnected());
    }
}