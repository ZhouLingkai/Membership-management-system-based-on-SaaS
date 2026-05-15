package com.ecards.member_management.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/**
 * 获取短信验证码请求DTO
 */
@Data
public class VerifyCodeRequest {

    /**
     * 手机号（AES256GCM加密后）
     */
    @NotBlank(message = "手机号不能为空")
    private String phone;

    /**
     * 平台类型
     * MINI_PROGRAM - 小程序
     * WEB - 网页
     */
    @NotBlank(message = "平台类型不能为空")
    @Pattern(regexp = "^(MINI_PROGRAM|WEB)$", message = "平台类型只能是MINI_PROGRAM或WEB")
    private String platform;
}

