package com.ecards.member_management.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 普通令牌响应DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NormalTokenResponse {

    /**
     * 普通令牌（Bearer格式）
     */
    private String token;

    /**
     * 普通令牌过期时间（格式：yyyy-MM-dd HH:mm:ss）
     */
    private String expireTime;

    /**
     * 普通令牌唯一标识（JTI）
     */
    private String jti;

    /**
     * 用户角色
     */
    private String userRole;

    /**
     * 自动登录令牌过期时间（可选）
     */
    private String autoExpireTime;
}

