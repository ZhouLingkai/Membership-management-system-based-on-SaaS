package com.ecards.member_management.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 店铺详情响应DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StoreDetailResponse {

    /**
     * 店铺ID
     */
    private String storeId;

    /**
     * 关联商户ID
     */
    private String merchantId;

    /**
     * 店铺名称
     */
    private String storeName;

    /**
     * 店铺类型
     */
    private String storeType;

    /**
     * 店铺地址
     */
    private String storeAddress;

    /**
     * 联系电话
     */
    private String contactPhone;

    /**
     * 联系微信号
     */
    private String contactWx;

    /**
     * 营业时间
     */
    private String businessHours;

    /**
     * 门头店照（OSS对象路径）
     */
    private String storePhotos;

    /**
     * 营业执照（OSS对象路径）
     */
    private String businessLicense;

    /**
     * 是否支持预约
     */
    private Integer appointment;

    /**
     * 店铺状态（1-正常营业等）
     */
    private Integer storeStatus;

    /**
     * 建店时间
     */
    private String openStoreTime;

    /**
     * 创建时间
     */
    private String createTime;

    /**
     * 最后更新时间
     */
    private String lastUpdateTime;
}

