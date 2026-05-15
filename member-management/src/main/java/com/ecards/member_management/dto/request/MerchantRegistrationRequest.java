package com.ecards.member_management.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 商户注册请求DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MerchantRegistrationRequest {

    /**
     * 申请人用户ID（与令牌中user_id一致）
     */
    @NotBlank(message = "用户ID不能为空")
    private String userId;

    /**
     * 申请方式
     * 1 - 免认证通道
     * 2 - 直接认证通道
     */
    @NotNull(message = "申请方式不能为空")
    @Min(value = 1, message = "申请方式只能是1或2")
    private Integer applicationType;

    /**
     * 商户名称（applicationType=1时必填，长度1-100位）
     */
    @Size(min = 1, max = 100, message = "商户名称长度必须在1-100位之间")
    private String merchantName;

    /**
     * 二级密码（applicationType=1时必填，明文，长度≥8位）
     */
    @Size(min = 8, message = "二级密码长度至少为8位")
    private String sndPswd;

    /**
     * 店铺规模（applicationType=2时必填）
     */
    private Integer numStores;

    /**
     * 会员规模（applicationType=2时必填）
     */
    private String numMembers;

    /**
     * 第一家店铺名称（applicationType=2时必填）
     */
    private String storeName;

    /**
     * 门头店照（applicationType=2时必填，Base64编码）
     */
    private String storePhotos;

    /**
     * 营业执照图片（applicationType=2时必填，Base64编码）
     */
    private String businessLicense;
}

