package com.ecards.member_management.dto.reservation;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 接口15：停用预约资源响应
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DisableResourceResponse {

    private Long reservationId;
    private String resourceName;
    private String disableDate;
    private String startTime;
    private String endTime;
    private String operatorName;
    private String createTime;
}
