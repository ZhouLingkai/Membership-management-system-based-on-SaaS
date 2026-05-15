package com.ecards.member_management.dto.reservation;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.util.List;

/**
 * 接口20：设置优惠策略不生效日期请求
 */
@Data
public class NonEffectiveDatesRequest {

    @NotEmpty(message = "资源ID列表不能为空")
    private List<Long> resourceIds;

    @NotNull(message = "操作类型不能为空")
    @Pattern(regexp = "add|replace|remove", message = "操作类型必须为add、replace或remove")
    private String operation;

    @NotEmpty(message = "日期列表不能为空")
    private List<String> dates;
}
