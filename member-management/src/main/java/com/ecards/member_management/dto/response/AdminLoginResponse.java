package com.ecards.member_management.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 管理员登录响应DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminLoginResponse {

    /**
     * AdminToken（包含Bearer前缀）
     */
    private String adminToken;

    /**
     * Token过期时间（yyyy-MM-dd HH:mm:ss格式）
     */
    private String expireTime;

    /**
     * 管理员信息
     */
    private AdminInfo adminInfo;

    /**
     * 管理员信息内部类
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AdminInfo {
        /**
         * 管理员ID
         */
        private String adminId;

        /**
         * 账号
         */
        private String account;

        /**
         * 管理员角色（1-超管，2-审核员）
         */
        private Integer adminRole;

        /**
         * 角色代码（SUPER_ADMIN, AUDITOR）
         */
        private String roleCode;

        /**
         * 手机号（脱敏）
         */
        private String phone;
    }
}

