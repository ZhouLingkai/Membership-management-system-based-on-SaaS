package com.ecards.member_management.dto.reservation;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 接口9：启停用资源请求
 */
@Data
public class ResourceToggleRequest {
    
    @NotNull(message = "isReservable不能为空")
    @Min(value = 0, message = "isReservable必须为0或1")
    @Max(value = 1, message = "isReservable必须为0或1")
    private Integer isReservable;
}
