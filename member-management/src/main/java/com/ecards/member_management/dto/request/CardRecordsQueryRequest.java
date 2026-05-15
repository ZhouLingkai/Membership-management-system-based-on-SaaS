package com.ecards.member_management.dto.request;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 单卡交易记录查询请求DTO（接口4）
 * 
 * @author Ecards Team
 * @since 2025-11-05
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CardRecordsQueryRequest {

    /**
     * 会员卡ID（UUID格式）
     */
    @NotBlank(message = "会员卡ID不能为空")
    private String memberCardId;

    /**
     * 交易类型筛选：1-充值，2-消费，3-退款，4-延期，5-日期变动
     */
    @Min(value = 1, message = "交易类型范围1-5")
    @Max(value = 5, message = "交易类型范围1-5")
    private Integer transactionType;

    /**
     * 开始日期（yyyy-MM-dd）
     */
    @Pattern(regexp = "^\\d{4}-\\d{2}-\\d{2}$", message = "开始日期格式错误")
    private String startDate;

    /**
     * 结束日期（yyyy-MM-dd）
     */
    @Pattern(regexp = "^\\d{4}-\\d{2}-\\d{2}$", message = "结束日期格式错误")
    private String endDate;

    /**
     * 页码（从1开始）
     */
    @NotNull(message = "页码不能为空")
    @Min(value = 1, message = "页码必须大于0")
    private Integer pageNum;

    /**
     * 每页条数（1-100）
     */
    @NotNull(message = "每页条数不能为空")
    @Min(value = 1, message = "每页条数最小为1")
    @Max(value = 100, message = "每页条数最大为100")
    private Integer pageSize;
}

