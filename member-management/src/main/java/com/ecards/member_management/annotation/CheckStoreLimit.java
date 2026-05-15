package com.ecards.member_management.annotation;

import java.lang.annotation.*;

/**
 * 检查店铺数量限制
 * 根据商户等级限制可创建的店铺数量
 * - 普通商户：最多1个店铺
 * - VIP商户：最多2个店铺
 * - SVIP商户：最多4个店铺
 * 
 * @author Ecards Team
 * @since 2025-10-30
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface CheckStoreLimit {
    
    /**
     * 错误提示信息
     */
    String message() default "店铺数量已达上限，请升级VIP获取更多店铺配额";
}

