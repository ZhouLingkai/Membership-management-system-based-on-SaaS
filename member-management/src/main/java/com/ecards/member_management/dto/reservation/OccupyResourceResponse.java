package com.ecards.member_management.dto.reservation;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 接口16：线下占用预约资源响应
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OccupyResourceResponse {

    private Long reservationId;
    private String resourceName;
    private String occupyDate;
    private String startTime;
    private String endTime;
    private String customerPhone; // AES加密
    private String operatorName;
    private String createTime;
}
