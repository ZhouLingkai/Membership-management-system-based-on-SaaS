package com.ecards.member_management.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 会员卡种列表项DTO
 * 
 * @author Ecards Team
 * @since 2025-11-02
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CardTypeItemResponse {

    /**
     * 卡种ID
     */
    private Long cardTypeId;

    /**
     * 卡种名称
     */
    private String cardTypeName;

    /**
     * 卡种类型
     */
    private Integer cardTtype;

    /**
     * 卡种类型名称（余额卡/次数卡/时效卡/积分卡）
     */
    private String cardTtypeName;

    /**
     * 卡种描述
     */
    private String description;

    /**
     * 自动消息通知类型
     */
    private Integer autoNotify;

    /**
     * 跨店通用设置
     */
    private Integer crossStore;

    /**
     * 卡面蒙版图片
     */
    private String cardMask;

    /**
     * 卡种背景图URL
     */
    private String cardBgc;

    /**
     * 预设充值项目数量
     */
    private Integer presetRechargeCount;

    /**
     * 预设消费项目数量
     */
    private Integer presetCostCount;
}

