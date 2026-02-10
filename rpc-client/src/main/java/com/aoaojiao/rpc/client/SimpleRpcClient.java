package com.aoaojiao.rpc.client;

import com.aoaojiao.rpc.common.constants.ProtocolConstants;
import com.aoaojiao.rpc.common.protocol.RpcMessageType;
import com.aoaojiao.rpc.common.protocol.RpcProtocol;
import com.aoaojiao.rpc.common.protocol.RpcRequest;
import com.aoaojiao.rpc.common.protocol.RpcResponse;
import com.aoaojiao.rpc.common.protocol.RpcStatus;
import com.aoaojiao.rpc.common.serialization.SerializationType;
import com.aoaojiao.rpc.common.serialization.Serializer;
import com.aoaojiao.rpc.common.serialization.SerializerFactory;
import com.aoaojiao.rpc.common.service.RpcInvocation;
import com.aoaojiao.rpc.common.util.TraceIdGenerator;
import com.aoaojiao.rpc.core.netty.RpcClient;
import com.aoaojiao.rpc.core.netty.RpcClientHandler;
import com.aoaojiao.rpc.core.transport.PendingRequests;
import com.aoaojiao.rpc.metrics.MetricsHolder;
import com.aoaojiao.rpc.metrics.MetricsRegistry;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public class SimpleRpcClient {
    private final RpcClient rpcClient;
    private final PendingRequests pendingRequests;
    private final RpcClientHandler clientHandler;
    private final Serializer serializer;
    private final MetricsRegistry metrics;

    public SimpleRpcClient(String host, int port) {
        this.pendingRequests = new PendingRequests();
        this.rpcClient = new RpcClient(host, port, pendingRequests);
        this.clientHandler = rpcClient.getClientHandler();
        this.serializer = SerializerFactory.get(SerializationType.HESSIAN.getCode());
        this.metrics = MetricsHolder.registry();
    }

    public void start() throws InterruptedException {
        rpcClient.connect();
    }

    public Object invoke(RpcInvocation invocation, long timeoutMillis) throws Exception {
        long start = System.nanoTime();
        String traceId = TraceIdGenerator.newTraceId();
        RpcRequest request = new RpcRequest(traceId, invocation);
        byte[] body = serializer.serialize(request);
        long requestId = clientHandler.nextRequestId();

        RpcProtocol protocol = new RpcProtocol(
                ProtocolConstants.VERSION,
                RpcMessageType.REQUEST,
                SerializationType.HESSIAN.getCode(),
                RpcStatus.OK,
                requestId,
                body
        );

        CompletableFuture<RpcProtocol> future = pendingRequests.register(requestId);
        rpcClient.getChannel().writeAndFlush(protocol);
        try {
            RpcProtocol response = future.get(timeoutMillis, TimeUnit.MILLISECONDS);
            RpcResponse rpcResponse = serializer.deserialize(response.getBody(), RpcResponse.class);
            if (rpcResponse == null) {
                metrics.increment("client.error");
                throw new IllegalStateException("Empty response");
            }
            if (!rpcResponse.isSuccess()) {
                metrics.increment("client.error");
                throw new IllegalStateException(rpcResponse.getErrorMessage());
            }
            metrics.increment("client.qps");
            return rpcResponse.getData();
        } catch (Exception ex) {
            metrics.increment("client.error");
            pendingRequests.fail(requestId, ex);
            throw ex;
        } finally {
            metrics.recordTime("client.rt", System.nanoTime() - start);
        }
    }

    public void close() {
        rpcClient.close();
    }
}
