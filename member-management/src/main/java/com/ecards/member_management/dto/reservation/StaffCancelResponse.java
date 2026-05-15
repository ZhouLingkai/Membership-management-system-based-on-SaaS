package com.ecards.member_management.dto.reservation;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 接口17：员工取消预约响应
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class StaffCancelResponse {

    private Long reservationId;
    private String cancelTime;
    private BigDecimal refundAmount;
    private BigDecimal penaltyAmount;
    private BigDecimal balanceSnapshot;
    private String operatorName;
    private String cancelReason;
}
