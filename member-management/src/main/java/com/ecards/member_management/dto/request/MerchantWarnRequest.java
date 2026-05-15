package com.ecards.member_management.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 商户警告请求DTO
 * 
 * @author Ecards Team
 * @since 2025-10-29
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MerchantWarnRequest {

    /**
     * 用户手机号（明文）
     */
    @NotBlank(message = "手机号不能为空")
    private String phone;
}

