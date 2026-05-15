package com.ecards.member_management.dto.reservation;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 接口4：查询资源列表 - 响应
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ResourceListResponse {
    private Integer total;
    private Integer page;
    private Integer pageSize;
    private List<ResourceItem> list;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ResourceItem {
        private Long id;
        private String resourceName;
        private Integer isReservable;
        private String downTime;
        private Integer supportCardTypes;  // 支持的会员卡种类型：1-余额卡，2-次数卡
        private String resourceDesc;       // 资源描述
        private String resourceImg;        // 资源图片URL
        private java.math.BigDecimal unitPrice;  // 单价
    }
}
