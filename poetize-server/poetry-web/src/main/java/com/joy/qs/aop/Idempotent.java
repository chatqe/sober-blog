package com.joy.qs.aop;

import java.lang.annotation.*;

/**
 * 接口幂等
 *
 * @author sjq
 * @since 2025-11-30 15:28
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Idempotent {

    /**
     * 幂等维度，SpEL 表达式，维度值key
     * 例："#userId"、"#orderNo"、"#ip+':'+#userId"
     */
    String key();

    /**
     * 过期时间（秒），默认 60
     */
    int ttl() default 60;

    /**
     * 提示语
     */
    String message() default "请求过于频繁，请稍后再试";

    /**
     * 是否开启分布式锁（可选，默认 false）
     */
    boolean useLock() default false;
}
