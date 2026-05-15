package com.ecards.member_management.dto.reservation;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

/**
 * 接口21：清空指定资源优惠策略请求
 */
@Data
public class ClearPromotionStrategyRequest {

    @NotEmpty(message = "资源ID列表不能为空")
    private List<Long> resourceIds;
}
