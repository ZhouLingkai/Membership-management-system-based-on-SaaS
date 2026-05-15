package com.ecards.member_management.annotation;

import java.lang.annotation.*;

/**
 * 管理员角色权限注解
 * 标记需要特定角色才能访问的接口
 * 
 * 使用方式：
 * - @RequireRole(1) - 仅超级管理员可访问
 * - @RequireRole({1, 2}) - 超管和审核员都可访问
 * 
 * 注意：必须配合 @RequireAdminAuth 使用
 * 
 * @author Ecards Team
 * @since 2025-10-28
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RequireRole {
    /**
     * 允许的角色ID列表
     * 1 - 超级管理员
     * 2 - 审核员
     */
    int[] value();
    
    /**
     * 权限描述（用于错误提示和日志）
     */
    String description() default "权限不足";
}


