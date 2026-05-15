package com.ecards.member_management.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 流水数据统计请求DTO
 * 
 * @author Ecards Team
 * @since 2025-12-17
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransStatisticsRequest {

    /**
     * 店铺ID（与merchantId至少填一个）
     */
    private String storeId;

    /**
     * 商家ID（与storeId至少填一个）
     */
    private String merchantId;

    /**
     * 日期范围代号：0-昨日，1-今日，7-近7日，17-本周，30-近30日，32-本月
     * 默认7（近7日）
     */
    @Builder.Default
    private Integer dateRange = 7;

    /**
     * 会员卡种ID（可选，不填则不限）
     */
    private Long cardTypeId;
}
