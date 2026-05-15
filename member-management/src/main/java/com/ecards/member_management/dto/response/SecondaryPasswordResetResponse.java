package com.ecards.member_management.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 商户二级密码重置响应DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SecondaryPasswordResetResponse {
    /**
     * 密码重置时间
     */
    private String resetTime;
}

