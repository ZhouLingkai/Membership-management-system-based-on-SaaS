package com.ecards.member_management.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 用户主动退出请求
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserLogoutRequest {
    /**
     * 用户ID（必填，用于验证权限）
     */
    @NotBlank(message = "用户ID不能为空")
    private String userId;

    /**
     * 自动登录令牌（可选，存在则一并注销）
     */
    private String autoLoginToken;

    /**
     * 是否全设备退出（可选，默认false）
     * true: 递增令牌版本号，使所有设备的令牌失效
     * false: 仅注销当前设备的令牌
     */
    private Boolean logoutAllDevices;

    /**
     * 平台类型（用于Cookie清理）
     * WEB - 网页端
     * MINI_PROGRAM - 小程序端
     */
    private String platform;
}

