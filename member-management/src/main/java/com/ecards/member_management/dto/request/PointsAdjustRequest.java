package com.ecards.member_management.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;

/**
 * 积分变动请求
 */
@Data
public class PointsAdjustRequest {

    /**
     * 会员卡ID（UUID格式）
     */
    @NotBlank(message = "会员卡ID不能为空")
    @Pattern(regexp = "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$",
             message = "会员卡ID格式错误")
    private String memberCardId;

    /**
     * 操作店铺ID（UUID格式）
     */
    @NotBlank(message = "店铺ID不能为空")
    @Pattern(regexp = "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$",
             message = "店铺ID格式错误")
    private String storeId;

    /**
     * 积分变动值（正数为增加，负数为扣减，绝对值≤10000）
     */
    @NotNull(message = "积分变动值不能为空")
    @Min(value = -10000, message = "单次积分变动不能低于-10000")
    @Max(value = 10000, message = "单次积分变动不能超过10000")
    private Integer pointsChange;

    /**
     * 变动原因（0-200位，允许空字符串）
     */
    @NotNull(message = "变动原因不能为空")
    @Size(max = 200, message = "变动原因长度不能超过200位")
    private String remark;
}

