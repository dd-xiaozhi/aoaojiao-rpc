package com.aoaojiao.rpc.core.netty;

import com.aoaojiao.rpc.common.constants.ProtocolConstants;
import com.aoaojiao.rpc.common.protocol.RpcMessageType;
import com.aoaojiao.rpc.common.protocol.RpcProtocol;
import com.aoaojiao.rpc.common.protocol.RpcStatus;
import com.aoaojiao.rpc.common.serialization.SerializationType;
import com.aoaojiao.rpc.core.transport.PendingRequests;
import com.aoaojiao.rpc.core.transport.RequestIdGenerator;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;

public class RpcClientHandler extends SimpleChannelInboundHandler<RpcProtocol> {
    private final PendingRequests pendingRequests;

    public RpcClientHandler(PendingRequests pendingRequests) {
        this.pendingRequests = pendingRequests;
    }

    public long nextRequestId() {
        return RequestIdGenerator.nextId();
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, RpcProtocol msg) {
        if (msg.getMessageType() == RpcMessageType.RESPONSE) {
            pendingRequests.complete(msg.getRequestId(), msg);
        }
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) {
        if (evt instanceof IdleStateEvent idle) {
            if (idle.state() == IdleState.WRITER_IDLE) {
                RpcProtocol heartbeat = new RpcProtocol(
                        ProtocolConstants.VERSION,
                        RpcMessageType.HEARTBEAT,
                        SerializationType.HESSIAN.getCode(),
                        RpcStatus.OK,
                        RequestIdGenerator.nextId(),
                        new byte[0]
                );
                ctx.writeAndFlush(heartbeat);
            }
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        ctx.close();
    }
}
