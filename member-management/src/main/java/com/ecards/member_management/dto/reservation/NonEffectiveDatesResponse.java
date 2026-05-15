package com.ecards.member_management.dto.reservation;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 接口20：设置优惠策略不生效日期响应
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class NonEffectiveDatesResponse {
    private Integer successCount;
    private String updateTime;
}
