package com.ecards.member_management.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 幂等性注解
 * 用于防止重复提交
 * 
 * 使用方式：在Controller方法上添加此注解
 * 前端需在请求头中携带X-Request-ID（UUID格式）
 * 
 * @author Ecards Team
 * @since 2025-11-04
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Idempotent {
    /**
     * 幂等性有效期（秒），默认10分钟
     */
    long timeout() default 600;
    
    /**
     * 业务描述（用于日志）
     */
    String value() default "";
}

