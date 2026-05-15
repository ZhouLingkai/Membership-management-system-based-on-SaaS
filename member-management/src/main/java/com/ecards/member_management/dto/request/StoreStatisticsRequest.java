package com.ecards.member_management.dto.request;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 店铺交易统计请求DTO（接口6）
 * 
 * @author Ecards Team
 * @since 2025-11-05
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StoreStatisticsRequest {

    /**
     * 店铺ID（UUID格式）
     */
    @NotBlank(message = "店铺ID不能为空")
    private String storeId;

    /**
     * 开始日期（yyyy-MM-dd）
     */
    @NotBlank(message = "开始日期不能为空")
    @Pattern(regexp = "^\\d{4}-\\d{2}-\\d{2}$", message = "开始日期格式错误")
    private String startDate;

    /**
     * 结束日期（yyyy-MM-dd）
     */
    @NotBlank(message = "结束日期不能为空")
    @Pattern(regexp = "^\\d{4}-\\d{2}-\\d{2}$", message = "结束日期格式错误")
    private String endDate;

    /**
     * 交易类型筛选：1-充值，2-消费（不填则统计所有）
     */
    @Min(value = 1, message = "交易类型范围1-2")
    @Max(value = 2, message = "交易类型范围1-2")
    private Integer transactionType;
}

