package com.ecards.member_management.dto.reservation;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * 接口10：查询某日预约情况响应
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReservationQueryResponse {
    
    private String realTime; // 系统当前时间（格式：YYYY-MM-DDThh:mm:ss）
    private String queryDate;
    private Integer advanceDays;
    private List<String> forbiddenDays;
    private List<String> timeList;
    private List<String> cancelRule;
    private List<ResourceDetail> resourceDetails;
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ResourceDetail {
        private Long resourceId;
        private String resourceName;
        private Integer isReservable;
        private String resourceDesc;   // 资源描述
        private String resourceImg;    // 资源图片URL
        private ResourceRestriction resourceRestriction;
        private DiscountInfo discount; // 优惠策略信息（仅余额卡资源返回）
        private List<ReservationItem> reservationList;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ResourceRestriction {
        private Integer minContinuousTime;
        private Integer maxContinuousTime;
        private Integer supportCardTypes;
        private BigDecimal unitPrice;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DiscountInfo {
        private List<String> nonEffectiveDates;
        private List<DiscountTimeSlot> discountsTime;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DiscountTimeSlot {
        private String timeSlot;
        private String discount;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ReservationItem {
        private String startTime;
        private String endTime;
        private Integer operateType;
        private MoreInfo moreInfo; // 仅工作令牌返回
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MoreInfo {
        private String userId;
        private String userPhone;
        private Long transactionId;
        private String remark;
    }
}
