package com.ecards.member_management.dto.reservation;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 接口12：预约资源响应
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReserveResourceResponse {
    
    private Long reservationId;
    private Long resourceId;
    private String resourceName;
    private String reservationDate;
    private String startTime;
    private String endTime;
    private Integer operateType;
    private Long transactionId;
    private BigDecimal transactionAmount;
    private String createTime;
}
