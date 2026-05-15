package com.ecards.member_management.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 自动登录响应DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AutoLoginResponse {

    /**
     * 普通令牌
     */
    private String normalToken;

    /**
     * 普通令牌过期时间
     */
    private String normalExpireTime;

    /**
     * 是否发生了令牌轮换
     */
    private Boolean tokenRotated;

    /**
     * 新的自动登录令牌（仅小程序端轮换时返回）
     */
    private String newAutoLoginToken;

    /**
     * 新的自动登录令牌过期时间（仅小程序端轮换时返回）
     */
    private String newAutoExpireTime;

    /**
     * 用户信息（简化版，与登录接口保持一致）
     */
    private SimpleUserInfo userInfo;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SimpleUserInfo {
        /**
         * 用户ID
         */
        private String userId;

        /**
         * 用户昵称
         */
        private String nickname;

        /**
         * 用户类型
         */
        private Integer userType;

        /**
         * 用户头像
         */
        private String avatar;

        /**
         * 用户邀请码
         */
        private String inviteCode;

        /**
         * 商家信息（仅当userType=2时返回）
         */
        private MerchantInfo merchantInfo;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserInfo {
        /**
         * 用户ID
         */
        private String userId;

        /**
         * 用户昵称
         */
        private String nickname;

        /**
         * 用户类型
         */
        private Integer userType;

        /**
         * 用户头像
         */
        private String avatar;

        /**
         * 商家信息（仅当userType=2时返回）
         */
        private MerchantInfo merchantInfo;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MerchantInfo {
        /**
         * 商户ID
         */
        private String merchantId;

        /**
         * 商户名称
         */
        private String merchantName;

        /**
         * 认证状态：1-已认证，2-测试中，3-审核中，4-审核拒绝，5-测试期过，6-认证存疑
         */
        private Integer certification;

        /**
         * 商家特权等级：1-普通，2-VIP，3-SVIP
         */
        private Integer merchantLevel;

        /**
         * 测试期过期时间（certification=2时返回）
         */
        private String testExpireTime;
    }
}
