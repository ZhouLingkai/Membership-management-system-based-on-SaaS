package com.ecards.member_management.dto.reservation;

import com.ecards.member_management.validator.ValidForbiddenDays;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * 接口7：修改资源 - 请求
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ResourceUpdateRequest {

    @Size(min = 1, max = 20, message = "资源名称长度必须为1-20字符")
    private String resourceName;

    @Min(value = 0, message = "最少连续预约时间必须≥0")
    private Integer minContinuousTime;

    @Max(value = 1440, message = "最大连续预约时间必须≤1440")
    private Integer maxContinuousTime;

    @DecimalMin(value = "0.00", message = "单价必须≥0")
    private BigDecimal unitPrice;

    @Size(max = 100, message = "资源图片URL长度不能超过100字符")
    private String resourceImg;

    @Size(max = 150, message = "资源描述长度不能超过150字符")
    private String resourceDesc;

    @ValidForbiddenDays
    private List<String> forbiddenDays;
}
