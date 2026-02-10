package com.aoaojiao.rpc.common.service;

import java.io.Serializable;
import java.util.Objects;

public class ServiceKey implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String interfaceName;
    private final String version;
    private final String group;

    private ServiceKey(String interfaceName, String version, String group) {
        this.interfaceName = interfaceName;
        this.version = version == null ? "" : version;
        this.group = group == null ? "" : group;
    }

    public static ServiceKey of(String interfaceName, String version, String group) {
        Objects.requireNonNull(interfaceName, "interfaceName");
        return new ServiceKey(interfaceName, version, group);
    }

    public String getInterfaceName() {
        return interfaceName;
    }

    public String getVersion() {
        return version;
    }

    public String getGroup() {
        return group;
    }

    public String key() {
        return interfaceName + ":" + group + ":" + version;
    }

    @Override
    public String toString() {
        return key();
    }
}
