package com.aoaojiao.rpc.spring.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RpcReference {
    String version() default "v1";

    String group() default "default";

    long timeoutMillis() default 3000;

    String loadBalancer() default "roundRobin";

    String faultTolerance() default "failFast";

    int retryTimes() default 0;

    long rateLimit() default 0;

    int circuitFailureThreshold() default 3;

    long circuitOpenMillis() default 3000;
}
