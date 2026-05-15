package com.ecards.member_management.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 特权令牌响应DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PrivilegeTokenResponse {

    /**
     * 特权令牌（Bearer格式）
     */
    private String token;

    /**
     * 令牌过期时间（格式：yyyy-MM-dd HH:mm:ss）
     */
    private String expireTime;

    /**
     * 令牌唯一标识（JTI）
     */
    private String jti;

    /**
     * 是否单次有效（固定为true）
     */
    private Boolean singleUse;
}

