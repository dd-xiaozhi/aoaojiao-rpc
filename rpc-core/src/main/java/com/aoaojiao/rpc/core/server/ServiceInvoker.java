package com.aoaojiao.rpc.core.server;

import com.aoaojiao.rpc.common.service.RpcInvocation;

public interface ServiceInvoker {
    Object invoke(RpcInvocation invocation) throws Exception;
}
