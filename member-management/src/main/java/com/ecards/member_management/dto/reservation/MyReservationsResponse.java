package com.ecards.member_management.dto.reservation;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * 接口11：获取预约列表响应
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MyReservationsResponse {
    
    private Integer total;
    private Integer page;
    private Integer pageSize;
    private List<ReservationItem> list;
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ReservationItem {
        private Long reservationId;
        private Long resourceId;
        private String resourceName;
        private String storeName;
        private String reservationDate;
        private String startTime;
        private String endTime;
        private Integer operateType;
        private BigDecimal transactionAmount;
        private Integer reservationStatus;
        private String remark;
        private String createTime;
    }
}
