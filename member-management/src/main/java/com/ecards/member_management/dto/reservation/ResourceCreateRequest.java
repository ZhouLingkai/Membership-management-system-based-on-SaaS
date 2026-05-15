package com.ecards.member_management.dto.reservation;

import com.ecards.member_management.validator.ValidForbiddenDays;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * 接口6：创建资源 - 请求
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ResourceCreateRequest {

    @NotBlank(message = "店铺ID不能为空")
    private String storeId;

    @NotNull(message = "资源列表不能为空")
    @Size(min = 1, max = 60, message = "资源数量必须在1-60之间")
    @Valid
    private List<ResourceItem> resources;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ResourceItem {

        @NotBlank(message = "资源名称不能为空")
        @Size(min = 1, max = 20, message = "资源名称长度必须为1-20字符")
        private String resourceName;

        @NotNull(message = "支持的卡种不能为空")
        @Min(value = 1, message = "支持的卡种只能为1或2")
        @Max(value = 2, message = "支持的卡种只能为1或2")
        private Integer supportCardTypes;

        @NotNull(message = "最少连续预约时间不能为空")
        @Min(value = 0, message = "最少连续预约时间必须≥0")
        private Integer minContinuousTime;

        @NotNull(message = "最大连续预约时间不能为空")
        @Max(value = 1440, message = "最大连续预约时间必须≤1440")
        private Integer maxContinuousTime;

        @NotNull(message = "单价不能为空")
        @DecimalMin(value = "0.00", message = "单价必须≥0")
        private BigDecimal unitPrice;

        @Size(max = 100, message = "资源图片URL长度不能超过100字符")
        private String resourceImg;

        @Size(max = 150, message = "资源描述长度不能超过150字符")
        private String resourceDesc;

        @ValidForbiddenDays
        private List<String> forbiddenDays;
    }
}
