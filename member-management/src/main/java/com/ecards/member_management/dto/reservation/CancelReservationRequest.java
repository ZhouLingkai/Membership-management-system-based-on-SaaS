package com.ecards.member_management.dto.reservation;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 接口13：取消预约资源请求
 */
@Data
public class CancelReservationRequest {
    
    @NotNull(message = "预约ID不能为空")
    private Long reservationId;
    
    private String remark; // 取消原因（可选）
}
