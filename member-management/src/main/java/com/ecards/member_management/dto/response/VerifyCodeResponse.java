package com.ecards.member_management.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 获取短信验证码响应DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VerifyCodeResponse {

    /**
     * 验证码发送时间
     */
    private String sendTime;

    /**
     * 验证码有效期（秒）
     */
    private Integer expireSeconds;

    /**
     * 剩余重试次数（今日）
     */
    private Integer remainingRetries;
}

