package com.aoaojiao.rpc.core.netty;

import com.aoaojiao.rpc.common.constants.ProtocolConstants;
import com.aoaojiao.rpc.common.protocol.RpcMessageType;
import com.aoaojiao.rpc.common.protocol.RpcProtocol;
import com.aoaojiao.rpc.common.protocol.RpcRequest;
import com.aoaojiao.rpc.common.protocol.RpcResponse;
import com.aoaojiao.rpc.common.protocol.RpcStatus;
import com.aoaojiao.rpc.common.serialization.Serializer;
import com.aoaojiao.rpc.common.serialization.SerializerFactory;
import com.aoaojiao.rpc.core.server.InflightRequestTracker;
import com.aoaojiao.rpc.core.server.ServiceInvoker;
import com.aoaojiao.rpc.metrics.MetricsHolder;
import com.aoaojiao.rpc.metrics.MetricsRegistry;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;

public class RpcServerHandler extends SimpleChannelInboundHandler<RpcProtocol> {
    private final ServiceInvoker serviceInvoker;
    private final ExecutorService businessExecutor;
    private final MetricsRegistry metrics;

    public RpcServerHandler(ServiceInvoker serviceInvoker, ExecutorService businessExecutor) {
        this.serviceInvoker = serviceInvoker;
        this.businessExecutor = businessExecutor;
        this.metrics = MetricsHolder.registry();
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, RpcProtocol msg) {
        if (msg.getMessageType() == RpcMessageType.HEARTBEAT) {
            RpcProtocol heartbeat = new RpcProtocol(
                    ProtocolConstants.VERSION,
                    RpcMessageType.HEARTBEAT,
                    msg.getSerialization(),
                    RpcStatus.OK,
                    msg.getRequestId(),
                    new byte[0]
            );
            ctx.writeAndFlush(heartbeat);
            return;
        }

        if (msg.getMessageType() == RpcMessageType.REQUEST) {
            try {
                businessExecutor.execute(() -> handleRequest(ctx, msg));
            } catch (RejectedExecutionException ex) {
                metrics.increment("server.reject");
                respondReject(ctx, msg);
            }
        }
    }

    private void handleRequest(ChannelHandlerContext ctx, RpcProtocol msg) {
        InflightRequestTracker.increment();
        long start = System.nanoTime();
        try {
            Serializer serializer = SerializerFactory.get(msg.getSerialization());
            RpcRequest request = serializer.deserialize(msg.getBody(), RpcRequest.class);
            String traceId = request == null ? null : request.getTraceId();
            RpcResponse response;
            if (serviceInvoker == null || request == null || request.getInvocation() == null) {
                response = RpcResponse.fail(traceId, "No service invoker");
                metrics.increment("server.error");
            } else {
                try {
                    Object result = serviceInvoker.invoke(request.getInvocation());
                    response = RpcResponse.ok(traceId, result);
                } catch (Exception ex) {
                    metrics.increment("server.error");
                    response = RpcResponse.fail(traceId, ex.getMessage());
                }
            }
            byte[] body = serializer.serialize(response);

            RpcProtocol protocol = new RpcProtocol(
                    ProtocolConstants.VERSION,
                    RpcMessageType.RESPONSE,
                    msg.getSerialization(),
                    RpcStatus.OK,
                    msg.getRequestId(),
                    body
            );
            ctx.writeAndFlush(protocol);
            metrics.increment("server.qps");
        } finally {
            metrics.recordTime("server.rt", System.nanoTime() - start);
            InflightRequestTracker.decrement();
        }
    }

    private void respondReject(ChannelHandlerContext ctx, RpcProtocol msg) {
        Serializer serializer = SerializerFactory.get(msg.getSerialization());
        RpcResponse response = RpcResponse.fail(null, "Server busy");
        byte[] body = serializer.serialize(response);
        RpcProtocol protocol = new RpcProtocol(
                ProtocolConstants.VERSION,
                RpcMessageType.RESPONSE,
                msg.getSerialization(),
                RpcStatus.FAIL,
                msg.getRequestId(),
                body
        );
        ctx.writeAndFlush(protocol);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        ctx.close();
    }
}
