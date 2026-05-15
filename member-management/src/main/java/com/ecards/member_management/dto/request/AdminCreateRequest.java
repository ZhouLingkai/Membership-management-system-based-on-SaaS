package com.ecards.member_management.dto.request;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 创建管理员请求DTO
 * 
 * @author Ecards Team
 * @since 2025-10-28
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminCreateRequest {

    /**
     * 手机号（明文）
     */
    @NotBlank(message = "手机号不能为空")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;

    /**
     * 登录账号
     */
    @NotBlank(message = "账号不能为空")
    @Size(min = 4, max = 50, message = "账号长度必须在4到50个字符之间")
    @Pattern(regexp = "^[a-zA-Z0-9_]+$", message = "账号只能包含字母、数字和下划线")
    private String account;

    /**
     * 登录密码
     */
    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 20, message = "密码长度必须在6到20个字符之间")
    private String password;

    /**
     * 二级密码（用于危险操作确认）
     */
    @NotBlank(message = "二级密码不能为空")
    @Size(min = 6, max = 20, message = "二级密码长度必须在6到20个字符之间")
    private String sndPassword;

    /**
     * 管理员角色
     * 2 - 审核员
     * 3 - 客服
     * 注意：禁止创建超级管理员（角色1）
     */
    @NotNull(message = "管理员角色不能为空")
    @Min(value = 2, message = "只能创建审核员或客服")
    @Max(value = 3, message = "管理员角色值无效")
    private Integer adminRole;

    /**
     * 备注信息
     */
    @Size(max = 255, message = "备注长度不能超过255个字符")
    private String remark;
}


