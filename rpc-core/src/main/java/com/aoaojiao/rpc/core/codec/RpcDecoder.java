package com.aoaojiao.rpc.core.codec;

import com.aoaojiao.rpc.common.constants.ProtocolConstants;
import com.aoaojiao.rpc.common.protocol.RpcMessageType;
import com.aoaojiao.rpc.common.protocol.RpcProtocol;
import com.aoaojiao.rpc.common.protocol.RpcStatus;
import com.aoaojiao.rpc.common.serialization.SerializationType;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.CorruptedFrameException;
import io.netty.handler.codec.MessageToMessageDecoder;

import java.util.List;

public class RpcDecoder extends MessageToMessageDecoder<ByteBuf> {
    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) {
        short magic = in.readShort();
        if (magic != ProtocolConstants.MAGIC) {
            throw new CorruptedFrameException("Illegal magic: " + magic);
        }
        byte version = in.readByte();
        byte messageType = in.readByte();
        byte serialization = in.readByte();
        byte status = in.readByte();
        long requestId = in.readLong();
        int bodyLength = in.readInt();
        if (bodyLength < 0 || bodyLength > ProtocolConstants.MAX_BODY_LENGTH) {
            throw new CorruptedFrameException("Illegal body length: " + bodyLength);
        }
        byte[] body = new byte[bodyLength];
        if (bodyLength > 0) {
            in.readBytes(body);
        }

        RpcProtocol protocol = new RpcProtocol();
        protocol.setVersion(version);
        protocol.setMessageType(RpcMessageType.fromCode(messageType));
        protocol.setSerialization(SerializationType.fromCode(serialization).getCode());
        protocol.setStatus(RpcStatus.fromCode(status));
        protocol.setRequestId(requestId);
        protocol.setBody(body);
        out.add(protocol);
    }
}
