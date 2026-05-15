package com.ecards.member_management.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 店铺信息修改请求DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StoreUpdateRequest {

    /**
     * 商户ID（与令牌一致）
     */
    @NotBlank(message = "商户ID不能为空")
    private String merchantId;

    /**
     * 店铺名称（可选）
     */
    @Size(min = 1, max = 100, message = "店铺名称长度为1-100个字符")
    private String storeName;

    /**
     * 店铺地址（可选）
     */
    @Size(max = 255, message = "店铺地址长度不能超过255个字符")
    private String storeAddress;

    /**
     * 联系电话（可选）
     */
    @Size(max = 20, message = "联系电话长度不能超过20个字符")
    private String contactPhone;

    /**
     * 联系微信号（可选）
     */
    @Size(max = 20, message = "联系微信号长度不能超过20个字符")
    private String contactWx;

    /**
     * 营业时间（可选）
     */
    @Size(max = 100, message = "营业时间长度不能超过100个字符")
    private String businessHours;

    /**
     * 门头店照（OSS对象路径，可选）
     */
    @Size(max = 255, message = "门头店照路径长度不能超过255个字符")
    private String storePhotos;

    /**
     * 营业执照（OSS对象路径，可选）
     */
    @Size(max = 255, message = "营业执照路径长度不能超过255个字符")
    private String businessLicense;

    /**
     * 是否支持预约（可选）
     */
    private Integer appointment;

    /**
     * 建店时间（可选）
     */
    private LocalDateTime openStoreTime;
}

