package com.ecards.member_management.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 店铺创建响应DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StoreCreateResponse {

    /**
     * 店铺ID
     */
    private String storeId;

    /**
     * 关联商户ID
     */
    private String merchantId;

    /**
     * 店铺状态
     */
    private String storeStatus;

    /**
     * 店铺创建时间
     */
    private String createTime;
}

