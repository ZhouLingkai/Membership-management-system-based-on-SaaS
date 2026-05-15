package com.ecards.member_management.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 用户信息查询响应
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserInfoResponse {
    /**
     * 用户信息对象
     */
    private UserInfo userInfo;

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
         * 用户类型：1-普通用户，2-商家，3-员工
         */
        private Integer userType;

        /**
         * 用户头像URL
         */
        private String avatar;

        /**
         * 用户自己的邀请码（供他人注册时填写）
         */
        private String inviteCode;

        /**
         * 会员头像URL
         */
        private String memberAvatar;

        /**
         * 手机号（AES-256-CBC加密后）
         */
        private String phone;

        /**
         * 注册时填写的邀请码（邀请者的邀请码）
         */
        private String invitedCode;

        /**
         * 注册时间
         */
        private String registerTime;

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

