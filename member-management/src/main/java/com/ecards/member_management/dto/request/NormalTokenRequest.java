package com.ecards.member_management.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;

/**
 * 获取普通令牌请求DTO
 */
@Data
public class NormalTokenRequest {

    /**
     * 手机号（AES256GCM加密）
     * 仅loginType=1/2时必填
     */
    private String phone;

    /**
     * 短信验证码
     * 仅loginType=1时必填
     */
    private String verifyCode;

    /**
     * 密码（明文，后端会验证）
     * 仅loginType=2时必填
     */
    private String password;

    /**
     * 登录方式
     * 1 - 验证码登录
     * 2 - 密码登录
     * 3 - 自动登录
     */
    @NotNull(message = "登录方式不能为空")
    @Min(value = 1, message = "登录方式无效")
    @Max(value = 3, message = "登录方式无效")
    private Integer loginType;

    /**
     * 自动登录凭证
     * 仅loginType=3时必填
     */
    private String autoLoginToken;

    /**
     * 平台类型
     * MINI_PROGRAM - 小程序
     * WEB - 网页
     */
    @NotBlank(message = "平台类型不能为空")
    @Pattern(regexp = "^(MINI_PROGRAM|WEB)$", message = "平台类型只能是MINI_PROGRAM或WEB")
    private String platform;
}

