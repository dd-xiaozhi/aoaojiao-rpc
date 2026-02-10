package com.aoaojiao.rpc.core.codec;

import com.aoaojiao.rpc.common.constants.ProtocolConstants;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;

public class RpcFrameDecoder extends LengthFieldBasedFrameDecoder {
    public RpcFrameDecoder() {
        super(ProtocolConstants.MAX_BODY_LENGTH + ProtocolConstants.HEADER_LENGTH,
                14, 4, 0, 0);
    }
}
