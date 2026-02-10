package com.aoaojiao.rpc.core.codec;

import com.aoaojiao.rpc.common.constants.ProtocolConstants;
import com.aoaojiao.rpc.common.protocol.RpcProtocol;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

public class RpcEncoder extends MessageToByteEncoder<RpcProtocol> {
    @Override
    protected void encode(ChannelHandlerContext ctx, RpcProtocol msg, ByteBuf out) {
        if (msg == null) {
            return;
        }
        byte[] body = msg.getBody();
        int bodyLength = body == null ? 0 : body.length;
        if (bodyLength > ProtocolConstants.MAX_BODY_LENGTH) {
            throw new IllegalArgumentException("Body too large: " + bodyLength);
        }

        out.writeShort(ProtocolConstants.MAGIC);
        out.writeByte(msg.getVersion());
        out.writeByte(msg.getMessageType().getCode());
        out.writeByte(msg.getSerialization());
        out.writeByte(msg.getStatus().getCode());
        out.writeLong(msg.getRequestId());
        out.writeInt(bodyLength);
        if (bodyLength > 0) {
            out.writeBytes(body);
        }
    }
}
