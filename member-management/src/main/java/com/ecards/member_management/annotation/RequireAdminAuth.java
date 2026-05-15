package com.ecards.member_management.annotation;

import java.lang.annotation.*;

/**
 * 管理员认证注解
 * 标记需要管理员Token认证的接口
 * 
 * 使用方式：
 * - @RequireAdminAuth - 仅验证管理员身份，不限制角色
 * - @RequireAdminAuth + @RequireRole - 验证身份并限制角色
 * 
 * @author Ecards Team
 * @since 2025-10-28
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RequireAdminAuth {
    /**
     * 接口描述（用于日志记录）
     */
    String value() default "";
}


