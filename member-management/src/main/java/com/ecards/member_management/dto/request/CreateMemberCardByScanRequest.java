package com.ecards.member_management.dto.request;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 会员卡办理（线下扫码办理）请求DTO
 * 
 * @author Ecards Team
 * @since 2025-11-03
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateMemberCardByScanRequest {

    /**
     * 店铺ID（UUID格式）
     */
    @NotBlank(message = "店铺ID不能为空")
    private String storeId;

    /**
     * 卡种ID
     */
    @NotNull(message = "卡种ID不能为空")
    private Long cardTypeId;

    /**
     * 特权令牌（一次性二维码令牌）
     */
    @NotBlank(message = "特权令牌不能为空")
    private String privilegeToken;

    /**
     * 会员预留姓名（可选）
     */
    @Size(max = 64, message = "会员姓名长度不能超过64位")
    private String memberName;

    /**
     * 初始余额（可选，默认0.00）
     */
    @DecimalMin(value = "0.00", message = "初始余额不能为负数")
    private BigDecimal initialBalance;

    /**
     * 初始次数（可选，默认0）
     */
    @Min(value = 0, message = "初始次数不能为负数")
    private Integer initialTimes;

    /**
     * 初始积分（可选，默认0）
     */
    @Min(value = 0, message = "初始积分不能为负数")
    private Integer initialPoints;

    /**
     * 到期时间（ISO8601格式，如"2085-11-03T00:00:00"）
     * 可选，默认60年后
     */
    private String expireTime;
}

