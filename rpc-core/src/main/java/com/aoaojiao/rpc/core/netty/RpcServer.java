package com.aoaojiao.rpc.core.netty;

import com.aoaojiao.rpc.core.codec.RpcDecoder;
import com.aoaojiao.rpc.core.codec.RpcEncoder;
import com.aoaojiao.rpc.core.codec.RpcFrameDecoder;
import com.aoaojiao.rpc.core.server.BusinessExecutor;
import com.aoaojiao.rpc.core.server.InflightRequestTracker;
import com.aoaojiao.rpc.core.server.ServiceInvoker;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.timeout.IdleStateHandler;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionHandler;

public class RpcServer {
    private final int port;
    private final ExecutorService businessExecutor;
    private final ServiceInvoker serviceInvoker;
    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;
    private Channel channel;

    public RpcServer(int port, int businessThreads, ServiceInvoker serviceInvoker) {
        this(port, businessThreads, 10000, RejectPolicy.ABORT, serviceInvoker);
    }

    public RpcServer(int port,
                     int businessThreads,
                     int queueSize,
                     RejectPolicy rejectPolicy,
                     ServiceInvoker serviceInvoker) {
        this.port = port;
        RejectedExecutionHandler handler = rejectPolicy.toHandler();
        this.businessExecutor = new BusinessExecutor(businessThreads, queueSize, handler).getExecutor();
        this.serviceInvoker = serviceInvoker;
    }

    public void start() throws InterruptedException {
        bossGroup = new NioEventLoopGroup(1);
        workerGroup = new NioEventLoopGroup();
        ServerBootstrap bootstrap = new ServerBootstrap();
        bootstrap.group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel.class)
                .childOption(ChannelOption.TCP_NODELAY, true)
                .childOption(ChannelOption.SO_KEEPALIVE, true)
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) {
                        ch.pipeline()
                                .addLast(new RpcFrameDecoder())
                                .addLast(new RpcDecoder())
                                .addLast(new RpcEncoder())
                                .addLast(new IdleStateHandler(60, 0, 0))
                                .addLast(new RpcServerHandler(serviceInvoker, businessExecutor));
                    }
                });
        channel = bootstrap.bind(port).sync().channel();
    }

    public void stopGracefully(long timeoutMillis) {
        try {
            InflightRequestTracker.awaitZero(timeoutMillis);
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
        }
        stop();
    }

    public void stop() {
        if (channel != null) {
            channel.close();
        }
        if (bossGroup != null) {
            bossGroup.shutdownGracefully();
        }
        if (workerGroup != null) {
            workerGroup.shutdownGracefully();
        }
        businessExecutor.shutdown();
    }
}
