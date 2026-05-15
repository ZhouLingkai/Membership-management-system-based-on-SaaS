package com.ecards.member_management.dto.reservation;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 接口19：创建/修改优惠策略响应
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PromotionStrategyResponse {
    private Integer successCount;
    private String updateTime;
}
