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
 * 商户资质补充提交请求
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QualificationSubmitRequest {
    
    /**
     * 商户ID（与令牌一致）
     */
    @NotBlank(message = "商户ID不能为空")
    private String merchantId;
    
    /**
     * 店铺规模（预计店铺数量）
     */
    @NotNull(message = "店铺规模不能为空")
    @Min(value = 1, message = "店铺数量至少为1")
    private Integer numStores;
    
    /**
     * 会员规模（预计会员数量范围）
     */
    @NotBlank(message = "会员规模不能为空")
    @Size(max = 100, message = "会员规模描述不能超过100个字符")
    private String numMembers;
    
    /**
     * 第一家店铺名称
     */
    @NotBlank(message = "店铺名称不能为空")
    @Size(max = 64, message = "店铺名称不能超过64个字符")
    private String storeName;
    
    /**
     * 门头店照（OSS Object路径，如：merchant/{userId}/storePhoto_xxx.jpg）
     */
    @NotBlank(message = "门头店照不能为空")
    @Size(max = 500, message = "门头店照路径不能超过500个字符")
    private String storePhotos;
    
    /**
     * 营业执照图片（OSS Object路径）
     */
    @NotBlank(message = "营业执照不能为空")
    @Size(max = 500, message = "营业执照路径不能超过500个字符")
    private String businessLicense;
}

