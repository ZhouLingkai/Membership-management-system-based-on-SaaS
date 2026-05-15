package com.ecards.member_management.dto.request;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 时效调整请求DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExpireAdjustRequest {

    /**
     * 会员卡ID（UUID格式）
     */
    @NotBlank(message = "会员卡ID不能为空")
    private String memberCardId;

    /**
     * 操作店铺ID（UUID格式）
     */
    @NotBlank(message = "店铺ID不能为空")
    private String storeId;

    /**
     * 调整类型：1-相对延期，2-绝对设置
     */
    @NotNull(message = "调整类型不能为空")
    @Min(value = 1, message = "调整类型必须为1或2")
    @Max(value = 2, message = "调整类型必须为1或2")
    private Integer adjustType;

    /**
     * 延期天数（adjustType=1时必填，正整数）
     */
    private Integer days;

    /**
     * 到期时间（adjustType=2时必填，yyyy-MM-dd HH:mm:ss）
     */
    private String expireTime;

    /**
     * 调整原因（0-200位）
     */
    @NotNull(message = "调整原因不能为空")
    @Size(max = 200, message = "调整原因长度不能超过200位")
    private String remark;
}

