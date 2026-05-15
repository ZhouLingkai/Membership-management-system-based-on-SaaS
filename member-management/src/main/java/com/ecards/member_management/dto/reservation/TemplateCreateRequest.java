package com.ecards.member_management.dto.reservation;

import com.ecards.member_management.validator.ValidForbiddenDays;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 接口2：创建高级预约模板 - 请求
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TemplateCreateRequest {

    @NotBlank(message = "店铺ID不能为空")
    private String storeId;

    @NotNull(message = "可预约时间段不能为空")
    @Size(min = 1, message = "至少需要一个时间段")
    private List<String> reservationTimeList;

    @NotNull(message = "取消规则不能为空")
    private List<String> cancelRule;

    @NotNull(message = "提前预约天数不能为空")
    @Min(value = 1, message = "提前预约天数最少为1天")
    @Max(value = 30, message = "提前预约天数最多为30天")
    private Integer advanceDays;

    @ValidForbiddenDays
    private List<String> forbiddenDays;

    @NotNull(message = "是否支持自定义禁止日期不能为空")
    @Min(value = 0, message = "customizeForbidden只能为0或1")
    @Max(value = 1, message = "customizeForbidden只能为0或1")
    private Integer customizeForbidden;
}
