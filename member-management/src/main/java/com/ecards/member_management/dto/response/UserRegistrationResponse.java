package com.ecards.member_management.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 用户注册响应DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserRegistrationResponse {

    /**
     * 用户ID
     */
    private String userId;

    /**
     * 用户类型
     */
    private Integer userType;

    /**
     * 注册时间
     */
    private String registerTime;

    /**
     * 普通令牌
     */
    private String normalToken;

    /**
     * 令牌过期时间
     */
    private String tokenExpireTime;

    /**
     * 用户信息
     */
    private UserInfo userInfo;

    /**
     * 自动登录令牌（rememberMe=true时返回）
     */
    private String autoLoginToken;

    /**
     * 自动登录令牌过期时间
     */
    private String autoExpireTime;

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
         * 用户邀请码
         */
        private String inviteCode;

        /**
         * 用户头像
         */
        private String avatar;
    }
}

