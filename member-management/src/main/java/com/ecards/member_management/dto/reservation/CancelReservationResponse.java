package com.ecards.member_management.dto.reservation;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 接口13：取消预约资源响应
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CancelReservationResponse {
    
    private Long reservationId;
    private Integer reservationStatus;
    private Long refundTransactionId;
    private BigDecimal refundAmount;
    private BigDecimal penaltyAmount;
    private String penaltyRule;
    private String updateTime;
}
