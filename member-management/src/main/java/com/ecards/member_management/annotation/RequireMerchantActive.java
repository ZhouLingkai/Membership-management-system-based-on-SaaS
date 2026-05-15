package com.ecards.member_management.annotation;

import java.lang.annotation.*;

/**
 * 要求商户处于活跃状态（未被封禁）
 * 用于需要检查商户是否被封禁的接口
 * 
 * @author Ecards Team
 * @since 2025-10-29
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RequireMerchantActive {
    
    /**
     * 错误提示信息
     */
    String message() default "商家功能已被封禁，请联系管理员处理";
}

