package com.ecards.member_management.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 刷新令牌响应DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RefreshTokenResponse {

    /**
     * 刷新后的新令牌（Bearer格式）
     */
    private String newToken;

    /**
     * 新令牌过期时间（格式：yyyy-MM-dd HH:mm:ss）
     */
    private String newExpireTime;

    /**
     * 旧令牌JTI（已自动加入黑名单）
     */
    private String oldJti;

    /**
     * 新令牌JTI
     */
    private String newJti;
}

