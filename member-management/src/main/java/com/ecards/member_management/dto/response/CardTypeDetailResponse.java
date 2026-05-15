package com.ecards.member_management.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 会员卡种详情响应DTO
 * 
 * @author Ecards Team
 * @since 2025-11-02
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CardTypeDetailResponse {

    /**
     * 卡种ID
     */
    private Long cardTypeId;

    /**
     * 店铺ID
     */
    private String storeId;

    /**
     * 商家ID
     */
    private String merchantId;

    /**
     * 店铺名称
     */
    private String storeName;

    /**
     * 卡种名称
     */
    private String cardTypeName;

    /**
     * 卡种类型
     */
    private Integer cardTtype;

    /**
     * 卡种类型名称
     */
    private String cardTtypeName;

    /**
     * 卡种描述
     */
    private String description;

    /**
     * 完整预设充值项目（JSON字符串）
     */
    private String presetRecharge;

    /**
     * 完整预设消费项目（JSON字符串）
     */
    private String presetCost;

    /**
     * 自动消息通知类型
     */
    private Integer autoNotify;

    /**
     * 自动消息通知类型名称
     */
    private String autoNotifyName;

    /**
     * 跨店通用设置
     */
    private Integer crossStore;

    /**
     * 跨店通用设置名称
     */
    private String crossStoreName;

    /**
     * 卡面蒙版图片
     */
    private String cardMask;

    /**
     * 卡种背景图URL
     */
    private String cardBgc;
}

