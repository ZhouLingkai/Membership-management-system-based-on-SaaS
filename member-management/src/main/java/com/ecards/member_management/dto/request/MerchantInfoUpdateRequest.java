package com.ecards.member_management.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 商户基础信息修改请求DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MerchantInfoUpdateRequest {

    /**
     * 商户ID（与令牌一致）
     */
    @NotBlank(message = "商户ID不能为空")
    private String merchantId;

    /**
     * 商户名称（不填则不修改）
     */
    @Size(max = 100, message = "商户名称长度不能超过100个字符")
    private String merchantName;

    /**
     * 新联系邮箱（不填则不修改）
     */
    @Size(max = 255, message = "联系邮箱长度不能超过255个字符")
    private String contactEmail;

    /**
     * 商户简介（长度0-500位，不填则不修改）
     */
    @Size(max = 500, message = "商户简介长度不能超过500个字符")
    private String merchantIntro;
}

