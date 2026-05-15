package com.ecards.member_management.context;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 令牌上下文（ThreadLocal）
 * 用于在请求处理过程中传递令牌信息
 */
public class TokenContext {

    private static final ThreadLocal<TokenInfo> CONTEXT = new ThreadLocal<>();

    /**
     * 设置令牌信息
     */
    public static void set(TokenInfo tokenInfo) {
        CONTEXT.set(tokenInfo);
    }

    /**
     * 获取令牌信息
     */
    public static TokenInfo get() {
        return CONTEXT.get();
    }

    /**
     * 清除令牌信息（请求结束时必须调用）
     */
    public static void clear() {
        CONTEXT.remove();
    }

    /**
     * 获取当前用户ID
     */
    public static String getCurrentUserId() {
        TokenInfo tokenInfo = get();
        return tokenInfo != null ? tokenInfo.getUserId() : null;
    }

    /**
     * 获取当前用户角色
     */
    public static String getCurrentUserRole() {
        TokenInfo tokenInfo = get();
        return tokenInfo != null ? tokenInfo.getRole() : null;
    }

    /**
     * 获取当前商家ID
     */
    public static String getCurrentMerchantId() {
        TokenInfo tokenInfo = get();
        return tokenInfo != null ? tokenInfo.getMerchantId() : null;
    }

    /**
     * 获取当前店铺ID
     */
    public static String getCurrentStoreId() {
        TokenInfo tokenInfo = get();
        return tokenInfo != null ? tokenInfo.getStoreId() : null;
    }

    /**
     * 获取当前设备ID
     */
    public static String getCurrentDeviceId() {
        TokenInfo tokenInfo = get();
        return tokenInfo != null ? tokenInfo.getDeviceId() : null;
    }

    /**
     * 获取当前权限列表
     */
    public static List<String> getCurrentPermissions() {
        TokenInfo tokenInfo = get();
        return tokenInfo != null ? tokenInfo.getPermissions() : null;
    }

    /**
     * 检查是否有指定权限
     */
    public static boolean hasPermission(String permission) {
        List<String> permissions = getCurrentPermissions();
        return permissions != null && permissions.contains(permission);
    }

    /**
     * 检查是否有任意一个权限
     */
    public static boolean hasAnyPermission(String... permissions) {
        List<String> currentPermissions = getCurrentPermissions();
        if (currentPermissions == null || currentPermissions.isEmpty()) {
            return false;
        }
        for (String permission : permissions) {
            if (currentPermissions.contains(permission)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 检查是否拥有所有权限
     */
    public static boolean hasAllPermissions(String... permissions) {
        List<String> currentPermissions = getCurrentPermissions();
        if (currentPermissions == null || currentPermissions.isEmpty()) {
            return false;
        }
        for (String permission : permissions) {
            if (!currentPermissions.contains(permission)) {
                return false;
            }
        }
        return true;
    }

    /**
     * 令牌信息实体
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TokenInfo {
        /**
         * 用户ID
         */
        private String userId;

        /**
         * 用户角色
         */
        private String role;

        /**
         * 商家ID（可为null）
         */
        private String merchantId;

        /**
         * 店铺ID（可为null）
         */
        private String storeId;

        /**
         * 设备ID
         */
        private String deviceId;

        /**
         * 令牌JTI
         */
        private String jti;

        /**
         * 令牌类型
         */
        private Integer tokenType;

        /**
         * 权限列表（可为null）
         */
        private List<String> permissions;

        /**
         * 登录IP
         */
        private String loginIp;
    }
}

