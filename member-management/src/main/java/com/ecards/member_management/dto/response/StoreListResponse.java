package com.ecards.member_management.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 店铺列表响应DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StoreListResponse {

    /**
     * 店铺总数
     */
    private Integer total;

    /**
     * 店铺列表
     */
    private List<StoreItem> list;

    /**
     * 店铺列表项
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StoreItem {
        /**
         * 店铺ID
         */
        private String storeId;

        /**
         * 店铺名称
         */
        private String storeName;

        /**
         * 店铺类型
         */
        private String storeType;

        /**
         * 店铺状态
         */
        private Integer storeStatus;

        /**
         * 创建时间
         */
        private String createTime;
    }
}

