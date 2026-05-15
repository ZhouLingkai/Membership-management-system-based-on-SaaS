package com.ecards.member_management.constants;

/**
 * 管理员系统常量定义
 */
public class AdminConstants {

    /**
     * 管理员角色
     */
    public static class Role {
        /**
         * 超级管理员
         */
        public static final int SUPER_ADMIN = 1;

        /**
         * 审核员
         */
        public static final int AUDITOR = 2;

        /**
         * 客服（预留）
         */
        public static final int CUSTOMER_SERVICE = 3;

        /**
         * 角色代码
         */
        public static final String SUPER_ADMIN_CODE = "SUPER_ADMIN";
        public static final String AUDITOR_CODE = "AUDITOR";
        public static final String CUSTOMER_SERVICE_CODE = "CUSTOMER_SERVICE";

        /**
         * 获取角色代码
         */
        public static String getRoleCode(int role) {
            return switch (role) {
                case SUPER_ADMIN -> SUPER_ADMIN_CODE;
                case AUDITOR -> AUDITOR_CODE;
                case CUSTOMER_SERVICE -> CUSTOMER_SERVICE_CODE;
                default -> "UNKNOWN";
            };
        }
    }

    /**
     * 管理员状态
     */
    public static class Status {
        /**
         * 启用
         */
        public static final int ENABLED = 1;

        /**
         * 禁用
         */
        public static final int DISABLED = 0;
    }

    /**
     * 操作类型
     */
    public static class OperationType {
        /**
         * 审核通过
         */
        public static final String AUDIT_PASS = "AUDIT_PASS";

        /**
         * 审核拒绝
         */
        public static final String AUDIT_REJECT = "AUDIT_REJECT";

        /**
         * 商户审核通过
         */
        public static final String APPROVE_MERCHANT = "APPROVE_MERCHANT";

        /**
         * 商户审核拒绝
         */
        public static final String REJECT_MERCHANT = "REJECT_MERCHANT";

        /**
         * 商户警告
         */
        public static final String WARN_MERCHANT = "WARN_MERCHANT";

        /**
         * 商户封禁
         */
        public static final String BAN_MERCHANT = "BAN_MERCHANT";

        /**
         * 解除商户封禁
         */
        public static final String UNBAN_MERCHANT = "UNBAN_MERCHANT";

        /**
         * 等级修改
         */
        public static final String LEVEL_UPDATE = "LEVEL_UPDATE";

        /**
         * 创建管理员
         */
        public static final String CREATE_ADMIN = "CREATE_ADMIN";

        /**
         * 修改密码
         */
        public static final String UPDATE_PASSWORD = "UPDATE_PASSWORD";

        /**
         * 重置密码
         */
        public static final String RESET_PASSWORD = "RESET_PASSWORD";

        /**
         * 修改二级密码
         */
        public static final String UPDATE_SND_PASSWORD = "UPDATE_SND_PASSWORD";

        /**
         * 管理员登录
         */
        public static final String ADMIN_LOGIN = "ADMIN_LOGIN";
    }

    /**
     * 操作对象类型
     */
    public static class TargetType {
        /**
         * 商户
         */
        public static final String MERCHANT = "MERCHANT";

        /**
         * 管理员
         */
        public static final String ADMIN = "ADMIN";

        /**
         * 系统操作
         */
        public static final String SYSTEM = "SYSTEM";
    }

    /**
     * Redis键前缀
     */
    public static class RedisKey {
        /**
         * AdminToken存储
         * admin_token:{admin_id}:{device_id} -> jti
         */
        public static final String ADMIN_TOKEN_PREFIX = "admin_token:";

        /**
         * Token版本存储
         * admin_token_version:{admin_id} -> version
         */
        public static final String ADMIN_TOKEN_VERSION_PREFIX = "admin_token_version:";

        /**
         * 构建Token键
         */
        public static String buildTokenKey(String adminId, String deviceId) {
            return ADMIN_TOKEN_PREFIX + adminId + ":" + deviceId;
        }

        /**
         * 构建Token版本键
         */
        public static String buildTokenVersionKey(String adminId) {
            return ADMIN_TOKEN_VERSION_PREFIX + adminId;
        }
    }

    /**
     * JWT Claims键
     */
    public static class JwtClaims {
        public static final String ADMIN_ID = "admin_id";
        public static final String ADMIN_ROLE = "admin_role";
        public static final String ROLE_CODE = "role_code";
        public static final String LOGIN_IP = "login_ip";
        public static final String DEVICE_ID = "device_id";
        public static final String TOKEN_VERSION = "token_version";
    }

    /**
     * HTTP请求头
     */
    public static class Header {
        /**
         * 设备ID请求头
         */
        public static final String DEVICE_ID = "Device-Id";

        /**
         * AdminToken请求头
         */
        public static final String AUTHORIZATION = "Authorization";

        /**
         * Token前缀
         */
        public static final String TOKEN_PREFIX = "Bearer ";
    }
}

