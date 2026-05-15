package com.ecards.member_management.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 用户信息修改请求
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserInfoUpdateRequest {
    /**
     * 用户ID（必填，用于验证权限）
     */
    @NotBlank(message = "用户ID不能为空")
    private String userId;

    /**
     * 用户昵称（可选，1-50位）
     */
    @Size(min = 1, max = 50, message = "昵称长度必须在1-50位之间")
    private String nickname;

    /**
     * 用户头像URL（可选）
     */
    @Size(max = 255, message = "头像URL长度不能超过255个字符")
    private String avatar;

    /**
     * 会员头像URL（可选）
     */
    @Size(max = 255, message = "会员头像URL长度不能超过255个字符")
    private String memberAvatar;
}

