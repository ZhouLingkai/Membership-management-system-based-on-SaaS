package com.ecards.member_management.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 检查手机号注册状态请求DTO
 */
@Data
public class CheckPhoneRequest {

    /**
     * 手机号（可能是明文11位数字，也可能是AES-256-CBC加密后的Base64字符串）
     */
    @NotBlank(message = "手机号不能为空")
    private String phone;
}
