package com.ecards.member_management.context;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 管理员上下文工具类
 * 使用ThreadLocal存储当前请求的管理员信息
 * 
 * 生命周期：
 * - 请求进入：AdminJwtAuthenticationFilter 设置上下文
 * - 请求结束：Filter 清理上下文
 * 
 * 使用方式：
 * - AdminContext.getAdminId() - 获取当前管理员ID
 * - AdminContext.getAdminRole() - 获取当前管理员角色
 * - AdminContext.hasRole(1) - 判断是否有指定角色
 * 
 * @author Ecards Team
 * @since 2025-10-28
 */
@Slf4j
public class AdminContext {

    /**
     * ThreadLocal存储当前管理员信息
     */
    private static final ThreadLocal<AdminInfo> ADMIN_INFO_HOLDER = new ThreadLocal<>();

    /**
     * 设置当前管理员信息
     * 
     * @param adminInfo 管理员信息
     */
    public static void setAdminInfo(AdminInfo adminInfo) {
        ADMIN_INFO_HOLDER.set(adminInfo);
    }

    /**
     * 获取当前管理员信息
     * 
     * @return AdminInfo 或 null
     */
    public static AdminInfo getAdminInfo() {
        return ADMIN_INFO_HOLDER.get();
    }

    /**
     * 获取当前管理员ID
     * 
     * @return adminId（UUID字符串）或 null
     */
    public static String getAdminId() {
        AdminInfo adminInfo = ADMIN_INFO_HOLDER.get();
        return adminInfo != null ? adminInfo.getAdminId() : null;
    }

    /**
     * 获取当前管理员角色
     * 
     * @return adminRole 或 null
     */
    public static Integer getAdminRole() {
        AdminInfo adminInfo = ADMIN_INFO_HOLDER.get();
        return adminInfo != null ? adminInfo.getAdminRole() : null;
    }

    /**
     * 获取当前管理员账号
     * 
     * @return account 或 null
     */
    public static String getAccount() {
        AdminInfo adminInfo = ADMIN_INFO_HOLDER.get();
        return adminInfo != null ? adminInfo.getAccount() : null;
    }

    /**
     * 获取当前管理员角色代码
     * 
     * @return roleCode 或 null
     */
    public static String getRoleCode() {
        AdminInfo adminInfo = ADMIN_INFO_HOLDER.get();
        return adminInfo != null ? adminInfo.getRoleCode() : null;
    }

    /**
     * 获取当前请求的设备ID
     * 
     * @return deviceId 或 null
     */
    public static String getDeviceId() {
        AdminInfo adminInfo = ADMIN_INFO_HOLDER.get();
        return adminInfo != null ? adminInfo.getDeviceId() : null;
    }

    /**
     * 获取当前请求的IP
     * 
     * @return loginIp 或 null
     */
    public static String getLoginIp() {
        AdminInfo adminInfo = ADMIN_INFO_HOLDER.get();
        return adminInfo != null ? adminInfo.getLoginIp() : null;
    }

    /**
     * 判断当前管理员是否有指定角色
     * 
     * @param roleId 角色ID
     * @return boolean
     */
    public static boolean hasRole(int roleId) {
        Integer currentRole = getAdminRole();
        return currentRole != null && currentRole == roleId;
    }

    /**
     * 判断当前管理员是否有指定角色中的任意一个
     * 
     * @param roleIds 角色ID数组
     * @return boolean
     */
    public static boolean hasAnyRole(int... roleIds) {
        Integer currentRole = getAdminRole();
        if (currentRole == null) {
            return false;
        }
        for (int roleId : roleIds) {
            if (currentRole == roleId) {
                return true;
            }
        }
        return false;
    }

    /**
     * 判断当前管理员是否是超级管理员
     * 
     * @return boolean
     */
    public static boolean isSuperAdmin() {
        return hasRole(1); // 1 = SUPER_ADMIN
    }

    /**
     * 判断当前管理员是否是审核员
     * 
     * @return boolean
     */
    public static boolean isAuditor() {
        return hasRole(2); // 2 = AUDITOR
    }

    /**
     * 清除当前管理员信息
     * 注意：必须在请求结束后调用，防止内存泄漏
     */
    public static void clear() {
        ADMIN_INFO_HOLDER.remove();
    }

    /**
     * 管理员信息内部类
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AdminInfo {
        /**
         * 管理员ID（UUID字符串）
         */
        private String adminId;

        /**
         * 管理员账号
         */
        private String account;

        /**
         * 管理员角色
         * 1 - 超级管理员
         * 2 - 审核员
         */
        private Integer adminRole;

        /**
         * 角色代码
         * SUPER_ADMIN / AUDITOR
         */
        private String roleCode;

        /**
         * 设备ID
         */
        private String deviceId;

        /**
         * 登录IP
         */
        private String loginIp;

        /**
         * Token版本号
         */
        private Integer tokenVersion;
    }
}


