package com.ecards.member_management.annotation;

import java.lang.annotation.*;

/**
 * 要求商户达到指定等级
 * 用于VIP/SVIP专属功能
 * 
 * @author Ecards Team
 * @since 2025-10-29
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RequireMerchantLevel {
    
    /**
     * 最低要求等级：1-普通，2-VIP，3-SVIP
     */
    int minLevel() default 1;
    
    /**
     * 错误提示信息
     */
    String message() default "此功能需要VIP等级";
}

