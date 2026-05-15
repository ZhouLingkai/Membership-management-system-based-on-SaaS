package com.ecards.member_management.aspect;

import com.ecards.member_management.annotation.RequireAdminAuth;
import com.ecards.member_management.annotation.RequireRole;
import com.ecards.member_management.common.ErrorCode;
import com.ecards.member_management.context.AdminContext;
import com.ecards.member_management.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.Arrays;

/**
 * 管理员权限验证AOP切面
 * 
 * 功能：
 * 1. 验证管理员身份（@RequireAdminAuth）
 * 2. 验证管理员角色权限（@RequireRole）
 * 
 * 执行顺序：
 * - AdminJwtAuthenticationFilter（解析Token，设置AdminContext）
 * - AdminAuthAspect（验证权限）
 * - Controller方法
 * 
 * @author Ecards Team
 * @since 2025-10-28
 */
@Aspect
@Component
@Order(100) // 确保在Filter之后执行
@Slf4j
public class AdminAuthAspect {

    /**
     * 验证管理员身份
     * 拦截所有带 @RequireAdminAuth 注解的方法
     */
    @Before("@annotation(com.ecards.member_management.annotation.RequireAdminAuth) || " +
            "@within(com.ecards.member_management.annotation.RequireAdminAuth)")
    public void checkAdminAuth(JoinPoint joinPoint) {
        // 获取当前管理员信息
        AdminContext.AdminInfo adminInfo = AdminContext.getAdminInfo();
        
        if (adminInfo == null) {
            log.warn("管理员认证失败: AdminContext为空，可能Token未通过验证");
            throw new BusinessException(401, "未登录或Token无效");
        }

        String adminId = adminInfo.getAdminId();
        Integer adminRole = adminInfo.getAdminRole();

        if (adminId == null || adminRole == null) {
            log.warn("管理员认证失败: AdminContext信息不完整, adminId={}, adminRole={}", 
                    adminId, adminRole);
            throw new BusinessException(401, "管理员信息异常");
        }
    }

    /**
     * 验证管理员角色权限
     * 拦截所有带 @RequireRole 注解的方法
     */
    @Before("@annotation(com.ecards.member_management.annotation.RequireRole) || " +
            "@within(com.ecards.member_management.annotation.RequireRole)")
    public void checkRole(JoinPoint joinPoint) {
        // 获取方法签名
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();

        // 获取注解（优先方法级别，其次类级别）
        RequireRole requireRole = method.getAnnotation(RequireRole.class);
        if (requireRole == null) {
            requireRole = joinPoint.getTarget().getClass().getAnnotation(RequireRole.class);
        }

        if (requireRole == null) {
            log.warn("角色权限验证失败: 未找到@RequireRole注解");
            return;
        }

        // 获取当前管理员角色
        Integer currentRole = AdminContext.getAdminRole();
        if (currentRole == null) {
            log.warn("角色权限验证失败: 无法获取当前管理员角色");
            throw new BusinessException(403, "权限不足");
        }

        // 获取要求的角色列表
        int[] requiredRoles = requireRole.value();
        
        // 验证角色
        boolean hasPermission = false;
        for (int requiredRole : requiredRoles) {
            if (currentRole == requiredRole) {
                hasPermission = true;
                break;
            }
        }

        if (!hasPermission) {
            String adminId = AdminContext.getAdminId();
            log.warn("角色权限验证失败: adminId={}, currentRole={}, requiredRoles={}, method={}", 
                    adminId, currentRole, Arrays.toString(requiredRoles), 
                    joinPoint.getSignature().toShortString());
            
            throw new BusinessException(403, 
                    requireRole.description() + "（需要角色: " + getRoleNames(requiredRoles) + "）");
        }
    }

    /**
     * 获取角色名称（用于错误提示）
     */
    private String getRoleNames(int[] roleIds) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < roleIds.length; i++) {
            if (i > 0) {
                sb.append("或");
            }
            switch (roleIds[i]) {
                case 1 -> sb.append("超级管理员");
                case 2 -> sb.append("审核员");
                default -> sb.append("未知角色").append(roleIds[i]);
            }
        }
        return sb.toString();
    }
}

