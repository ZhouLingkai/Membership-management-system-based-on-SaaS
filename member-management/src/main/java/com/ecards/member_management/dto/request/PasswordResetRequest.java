package com.ecards.member_management.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 密码重置请求
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PasswordResetRequest {
    /**
     * 手机号（AES加密后的字符串）
     */
    @NotBlank(message = "手机号不能为空")
    private String phone;

    /**
     * 短信验证码（6位数字）
     */
    @NotBlank(message = "验证码不能为空")
    @Size(min = 6, max = 6, message = "验证码必须为6位")
    private String verifyCode;

    /**
     * 新密码（明文，长度≥8位）
     */
    @NotBlank(message = "新密码不能为空")
    @Size(min = 8, message = "密码长度至少为8位")
    private String newPassword;

    /**
     * 平台类型（WEB/MINI_PROGRAM）
     */
    @NotBlank(message = "平台类型不能为空")
    private String platform;
}

