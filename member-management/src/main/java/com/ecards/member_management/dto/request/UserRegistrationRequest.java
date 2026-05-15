package com.ecards.member_management.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 用户注册请求DTO
 */
@Data
public class UserRegistrationRequest {

    /**
     * 手机号（AES256GCM加密后）
     */
    @NotBlank(message = "手机号不能为空")
    private String phone;

    /**
     * 密码（明文，后端Argon2加密）
     */
    @NotBlank(message = "密码不能为空")
    @Size(min = 8, message = "密码长度至少8位")
    private String password;

    /**
     * 短信验证码
     */
    @NotBlank(message = "验证码不能为空")
    @Pattern(regexp = "^\\d{6}$", message = "验证码必须是6位数字")
    private String verifyCode;

    /**
     * 用户昵称
     */
    @NotBlank(message = "昵称不能为空")
    @Size(min = 1, max = 50, message = "昵称长度为1-50位")
    private String nickname;

    /**
     * 邀请码（可选）
     */
    private String invitedCode;

    /**
     * 是否记住登录（7天自动登录）
     */
    private Boolean rememberMe;

    /**
     * 平台类型
     */
    @NotBlank(message = "平台类型不能为空")
    @Pattern(regexp = "^(MINI_PROGRAM|WEB)$", message = "平台类型只能是MINI_PROGRAM或WEB")
    private String platform;
}

