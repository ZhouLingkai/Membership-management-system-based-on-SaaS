package com.ecards.member_management.dto.reservation;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * 接口19：创建/修改优惠策略请求
 */
@Data
public class PromotionStrategyRequest {

    @NotEmpty(message = "资源ID列表不能为空")
    private List<Long> resourceIds;

    @NotEmpty(message = "生效星期不能为空")
    private List<String> effectiveWeek;

    @NotEmpty(message = "优惠时间段列表不能为空")
    @Valid
    private List<DiscountTimeSlot> discountsTime;

    @Data
    public static class DiscountTimeSlot {
        @NotNull(message = "时间段不能为空")
        private String timeSlot;

        @NotNull(message = "优惠规则不能为空")
        private String discount;
    }
}
