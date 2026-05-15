package com.ecards.member_management.constants;

/**
 * 令牌常量类
 * 定义令牌相关的常量
 */
public class TokenConstants {

    // ==================== Redis Key前缀 ====================
    
    /**
     * 黑名单Key前缀
     * 格式：blacklist:{jti}
     */
    public static final String BLACKLIST_PREFIX = "blacklist:";

    /**
     * 自动登录令牌Key前缀
     * 格式：auto_login:{user_id}:{platform}:{device_id}
     */
    public static final String AUTO_LOGIN_PREFIX = "auto_login:";

    /**
     * 管理令牌使用次数Key前缀
     * 格式：manager_token_usage:{jti}
     */
    public static final String MANAGER_TOKEN_USAGE_PREFIX = "manager_token_usage:";

    // ==================== JWT Payload字段名 ====================
    
    /**
     * 用户ID字段
     */
    public static final String CLAIM_USER_ID = "user_id";

    /**
     * 角色字段
     */
    public static final String CLAIM_ROLE = "role";

    /**
     * 商家ID字段
     */
    public static final String CLAIM_MERCHANT_ID = "merchant_id";

    /**
     * 店铺ID字段
     */
    public static final String CLAIM_STORE_ID = "store_id";

    /**
     * 权限列表字段
     */
    public static final String CLAIM_PERMISSIONS = "permissions";

    /**
     * 登录IP字段
     */
    public static final String CLAIM_LOGIN_IP = "login_ip";

    /**
     * 设备ID字段
     */
    public static final String CLAIM_DEVICE_ID = "device_id";

    /**
     * 平台类型字段
     */
    public static final String CLAIM_PLATFORM = "platform";

    /**
     * 单次使用标记字段
     */
    public static final String CLAIM_SINGLE_USE = "single_use";

    /**
     * 最后续期时间字段
     */
    public static final String CLAIM_LAST_RENEW_TIME = "last_renew_time";

    /**
     * 允许续期标记字段
     */
    public static final String CLAIM_ALLOW_RENEW = "allow_renew";

    /**
     * 续期次数字段
     */
    public static final String CLAIM_RENEW_TIMES = "renew_times";

    /**
     * 令牌类型字段
     */
    public static final String CLAIM_TOKEN_TYPE = "token_type";

    // ==================== 其他常量 ====================
    
    /**
     * Bearer前缀
     */
    public static final String BEARER_PREFIX = "Bearer ";

    /**
     * Authorization请求头
     */
    public static final String AUTHORIZATION_HEADER = "Authorization";

    /**
     * Device-ID请求头
     */
    public static final String DEVICE_ID_HEADER = "Device-ID";

    /**
     * 管理令牌最大使用次数（默认）
     */
    public static final int MANAGER_TOKEN_MAX_USE_COUNT = 5;

    /**
     * 自动登录令牌最大续期次数
     */
    public static final int AUTO_LOGIN_MAX_RENEW_TIMES = 2;
}

