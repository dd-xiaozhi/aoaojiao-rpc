package com.aoaojiao.rpc.common.constants;

public final class ProtocolConstants {
    private ProtocolConstants() {
    }

    public static final short MAGIC = (short) 0xBEEF;
    public static final byte VERSION = 1;
    public static final int HEADER_LENGTH = 18;
    public static final int MAX_BODY_LENGTH = 10 * 1024 * 1024;
}
