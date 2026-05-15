package com.ecards.member_management.dto.request;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 会员卡充值请求DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RechargeRequest {

    /**
     * 会员卡ID（UUID格式）
     */
    @NotBlank(message = "会员卡ID不能为空")
    private String memberCardId;

    /**
     * 交易店铺ID（UUID格式）
     */
    @NotBlank(message = "店铺ID不能为空")
    private String storeId;

    /**
     * 充值类型（1-金额，2-次数）
     */
    @NotNull(message = "充值类型不能为空")
    @Min(value = 1, message = "充值类型必须为1或2")
    @Max(value = 2, message = "充值类型必须为1或2")
    private Integer rechargeType;

    /**
     * 充值金额/次数（余额卡：金额，次数卡：次数，最大10000）
     */
    @NotNull(message = "充值金额不能为空")
    @DecimalMin(value = "0.01", message = "充值金额必须大于0")
    @DecimalMax(value = "10000.00", message = "充值金额不能超过10000")
    @Digits(integer = 5, fraction = 2, message = "金额格式不正确，最多5位整数和2位小数")
    private BigDecimal amount;

    /**
     * 充值备注（0-200位，可包含预设项目名）
     */
    @NotNull(message = "充值备注不能为空")
    @Size(max = 200, message = "充值备注长度不能超过200位")
    private String remark;
}

