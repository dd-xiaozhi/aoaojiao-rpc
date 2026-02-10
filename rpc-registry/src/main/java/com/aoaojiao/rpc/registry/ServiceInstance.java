package com.aoaojiao.rpc.registry;

import java.io.Serializable;

public class ServiceInstance implements Serializable {
    private static final long serialVersionUID = 1L;

    private String host;
    private int port;
    private double weight = 1.0;

    public ServiceInstance() {
    }

    public ServiceInstance(String host, int port) {
        this.host = host;
        this.port = port;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public double getWeight() {
        return weight;
    }

    public void setWeight(double weight) {
        this.weight = weight;
    }

    @Override
    public String toString() {
        return host + ":" + port;
    }
}
