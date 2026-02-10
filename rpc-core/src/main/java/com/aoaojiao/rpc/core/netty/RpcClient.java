package com.aoaojiao.rpc.core.netty;

import com.aoaojiao.rpc.core.codec.RpcDecoder;
import com.aoaojiao.rpc.core.codec.RpcEncoder;
import com.aoaojiao.rpc.core.codec.RpcFrameDecoder;
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

public class RpcClient {
    private final String host;
    private final int port;
    private final PendingRequests pendingRequests;
    private final RpcClientHandler clientHandler;
    private EventLoopGroup group;
    private Channel channel;

    public RpcClient(String host, int port, PendingRequests pendingRequests) {
        this.host = host;
        this.port = port;
        this.pendingRequests = pendingRequests;
        this.clientHandler = new RpcClientHandler(pendingRequests);
    }

    public void connect() throws InterruptedException {
        group = new NioEventLoopGroup();
        Bootstrap bootstrap = new Bootstrap();
        bootstrap.group(group)
                .channel(NioSocketChannel.class)
                .option(ChannelOption.TCP_NODELAY, true)
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
        ChannelFuture future = bootstrap.connect(host, port).sync();
        channel = future.channel();
    }

    public Channel getChannel() {
        return channel;
    }

    public PendingRequests getPendingRequests() {
        return pendingRequests;
    }

    public RpcClientHandler getClientHandler() {
        return clientHandler;
    }

    public void close() {
        if (channel != null) {
            channel.close();
        }
        if (group != null) {
            group.shutdownGracefully();
        }
    }
}
