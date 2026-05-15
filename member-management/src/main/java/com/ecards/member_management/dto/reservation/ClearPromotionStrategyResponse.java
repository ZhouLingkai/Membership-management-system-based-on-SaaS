package com.ecards.member_management.dto.reservation;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 接口21：清空指定资源优惠策略响应
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ClearPromotionStrategyResponse {
    private Integer successCount;
    private String updateTime;
}
