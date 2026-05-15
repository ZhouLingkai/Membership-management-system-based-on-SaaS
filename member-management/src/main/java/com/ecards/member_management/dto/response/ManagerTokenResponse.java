package com.ecards.member_management.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 管理令牌响应DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ManagerTokenResponse {

    /**
     * 管理令牌（Bearer格式）
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
     * 5分钟内最大使用次数
     */
    private Integer maxUseCount;

    /**
     * 已使用次数
     */
    private Integer usedCount;
}

