package com.aoaojiao.rpc.common.service;

import java.io.Serializable;

public class RpcInvocation implements Serializable {
    private static final long serialVersionUID = 1L;

    private ServiceKey serviceKey;
    private String methodName;
    private String[] parameterTypes;
    private Object[] args;

    public RpcInvocation() {
    }

    public RpcInvocation(ServiceKey serviceKey, String methodName, String[] parameterTypes, Object[] args) {
        this.serviceKey = serviceKey;
        this.methodName = methodName;
        this.parameterTypes = parameterTypes;
        this.args = args;
    }

    public ServiceKey getServiceKey() {
        return serviceKey;
    }

    public void setServiceKey(ServiceKey serviceKey) {
        this.serviceKey = serviceKey;
    }

    public String getMethodName() {
        return methodName;
    }

    public void setMethodName(String methodName) {
        this.methodName = methodName;
    }

    public String[] getParameterTypes() {
        return parameterTypes;
    }

    public void setParameterTypes(String[] parameterTypes) {
        this.parameterTypes = parameterTypes;
    }

    public Object[] getArgs() {
        return args;
    }

    public void setArgs(Object[] args) {
        this.args = args;
    }
}
