package com.ecards.member_management.dto.reservation;

import com.ecards.member_management.validator.ValidForbiddenDays;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 接口3：修改高级预约模板 - 请求
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TemplateUpdateRequest {

    private List<String> reservationTimeList;

    private List<String> cancelRule;

    @Min(value = 1, message = "提前预约天数最少为1天")
    @Max(value = 30, message = "提前预约天数最多为30天")
    private Integer advanceDays;

    @ValidForbiddenDays
    private List<String> forbiddenDays;
}
