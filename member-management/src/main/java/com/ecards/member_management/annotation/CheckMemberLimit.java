package com.ecards.member_management.annotation;

import java.lang.annotation.*;

/**
 * 检查会员数量限制
 * 用于添加会员等需要检查数量限制的接口
 * 
 * 限制规则：
 * - 普通商户：最多200个会员
 * - VIP商户：最多1000个会员
 * - SVIP商户：无限制
 * 
 * @author Ecards Team
 * @since 2025-10-29
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface CheckMemberLimit {
    
    /**
     * 错误提示信息
     */
    String message() default "您的会员数量已达上限，请升级VIP";
}

