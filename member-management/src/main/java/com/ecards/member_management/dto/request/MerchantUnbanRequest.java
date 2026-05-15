package com.ecards.member_management.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 解除商户封禁请求DTO
 * 
 * @author Ecards Team
 * @since 2025-10-29
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MerchantUnbanRequest {

    /**
     * 用户手机号（明文）
     */
    @NotBlank(message = "手机号不能为空")
    private String phone;

    /**
     * 二级密码（用于危险操作确认）
     */
    @NotBlank(message = "二级密码不能为空")
    @Size(min = 6, max = 20, message = "二级密码长度必须在6到20个字符之间")
    private String sndPassword;
}

