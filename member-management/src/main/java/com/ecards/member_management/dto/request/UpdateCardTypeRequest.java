package com.ecards.member_management.dto.request;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 会员卡种修改请求DTO
 * 
 * @author Ecards Team
 * @since 2025-11-02
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateCardTypeRequest {

    /**
     * 卡种ID
     */
    @NotNull(message = "卡种ID不能为空")
    private Long cardTypeId;

    /**
     * 店铺ID（UUID格式）
     */
    @NotBlank(message = "店铺ID不能为空")
    private String storeId;

    /**
     * 新卡种名称（2-50位，不填则不修改）
     */
    @Size(min = 2, max = 50, message = "卡种名称长度必须为2-50位")
    private String cardTypeName;

    /**
     * 新卡种描述（0-500位，不填则不修改）
     */
    @Size(max = 500, message = "卡种描述长度不能超过500位")
    private String description;

    /**
     * 新卡面蒙版图片（不填则不修改）
     */
    @Size(max = 100, message = "卡面蒙版图片长度不能超过100位")
    private String cardMask;

    /**
     * 新卡种背景图URL（不填则不修改）
     */
    @Size(max = 100, message = "卡种背景图URL长度不能超过100位")
    private String cardBgc;

    /**
     * 新预设充值项目（不填则不修改）
     */
    private String presetRecharge;

    /**
     * 新预设消费项目（不填则不修改）
     */
    private String presetCost;

    /**
     * 新自动消息通知类型（不填则不修改）
     */
    @Min(value = 0, message = "自动消息通知类型必须为0-关闭、1-短信、2-订阅、3-推送")
    @Max(value = 3, message = "自动消息通知类型必须为0-关闭、1-短信、2-订阅、3-推送")
    private Integer autoNotify;

    /**
     * 新跨店通用设置（不填则不修改）
     */
    @Min(value = 0, message = "跨店通用设置必须为0-仅本店铺、1-同商家跨店")
    @Max(value = 1, message = "跨店通用设置必须为0-仅本店铺、1-同商家跨店")
    private Integer crossStore;
}

