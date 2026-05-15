package com.ecards.member_management.dto.reservation;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * 接口5：查询资源详细信息 - 响应
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ResourceDetailResponse {
    private Long id;
    private String storeId;
    private String resourceName;
    private Integer isReservable;
    private Integer supportCardTypes;
    private Integer minContinuousTime;
    private Integer maxContinuousTime;
    private BigDecimal unitPrice;
    private String resourceImg;
    private String resourceDesc;
    private String downTime;
    private String createTime;
    private String updateTime;
    
    // 模板相关信息
    private List<String> templateForbiddenDays;
    private Integer customizeForbidden;
    
    // 资源自定义禁止日期（仅当customizeForbidden=1时返回）
    private List<String> resourceForbiddenDays;
    
    // 优惠策略（仅余额卡资源返回，次数卡为null）
    private java.util.Map<String, Object> promotionStrategy;
}
