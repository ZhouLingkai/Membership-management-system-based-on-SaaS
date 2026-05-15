package com.ecards.member_management.dto.request;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 会员卡种创建请求DTO
 * 
 * @author Ecards Team
 * @since 2025-11-02
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateCardTypeRequest {

    /**
     * 店铺ID（UUID格式）
     */
    @NotBlank(message = "店铺ID不能为空")
    private String storeId;

    /**
     * 卡种名称（2-50位）
     */
    @NotBlank(message = "卡种名称不能为空")
    @Size(min = 2, max = 50, message = "卡种名称长度必须为2-50位")
    private String cardTypeName;

    /**
     * 卡种描述（0-500位）
     */
    @Size(max = 500, message = "卡种描述长度不能超过500位")
    private String description;

    /**
     * 卡面蒙版图片（数字或OSS路径，最多100位）
     */
    @Size(max = 100, message = "卡面蒙版图片长度不能超过100位")
    private String cardMask;

    /**
     * 卡种背景图URL（数字或OSS路径，最多100位）
     */
    @Size(max = 100, message = "卡种背景图URL长度不能超过100位")
    private String cardBgc;

    /**
     * 卡种类型：1-余额卡，2-次数卡，3-时效卡，4-积分卡
     */
    @NotNull(message = "卡种类型不能为空")
    @Min(value = 1, message = "卡种类型必须为1-余额卡、2-次数卡、3-时效卡、4-积分卡")
    @Max(value = 4, message = "卡种类型必须为1-余额卡、2-次数卡、3-时效卡、4-积分卡")
    private Integer cardTtype;

    /**
     * 预设充值项目（JSON字符串）
     * 示例：[{"itemName":"充值100元","itemDesc":"首次充值赠送10元","amount":110.00}]
     */
    @NotBlank(message = "预设充值项目不能为空")
    private String presetRecharge;

    /**
     * 预设消费项目（JSON字符串）
     * 示例：[{"itemName":"洗车服务","itemDesc":"普通洗车一次","amount":30.00}]
     */
    @NotBlank(message = "预设消费项目不能为空")
    private String presetCost;

    /**
     * 自动消息通知：0-关闭，1-短信通知，2-订阅通知，3-程序内推送
     */
    @NotNull(message = "自动消息通知类型不能为空")
    @Min(value = 0, message = "自动消息通知类型必须为0-关闭、1-短信、2-订阅、3-推送")
    @Max(value = 3, message = "自动消息通知类型必须为0-关闭、1-短信、2-订阅、3-推送")
    private Integer autoNotify;

    /**
     * 跨店通用：0-仅本店铺，1-同商家跨店通用
     */
    @NotNull(message = "跨店通用设置不能为空")
    @Min(value = 0, message = "跨店通用设置必须为0-仅本店铺、1-同商家跨店")
    @Max(value = 1, message = "跨店通用设置必须为0-仅本店铺、1-同商家跨店")
    private Integer crossStore;
}

