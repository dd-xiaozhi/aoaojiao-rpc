package com.aoaojiao.rpc.registry;

public final class RegistrySwitch {
    private static final ThreadLocal<Boolean> FORCE_LOCAL = ThreadLocal.withInitial(() -> false);

    private RegistrySwitch() {
    }

    public static void forceLocal(boolean enable) {
        FORCE_LOCAL.set(enable);
    }

    public static boolean isForceLocal() {
        return FORCE_LOCAL.get();
    }
}
