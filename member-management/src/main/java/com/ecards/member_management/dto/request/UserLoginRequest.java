package com.ecards.member_management.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/**
 * 用户登录请求DTO
 */
@Data
public class UserLoginRequest {

    /**
     * 手机号（AES256GCM加密后）
     */
    @NotBlank(message = "手机号不能为空")
    private String phone;

    /**
     * 密码（明文，后端验证）
     */
    @NotBlank(message = "密码不能为空")
    private String password;

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

