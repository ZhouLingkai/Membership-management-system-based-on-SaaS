package com.ecards.member_management.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 会员卡视图对象DTO（通用）
 * 
 * @author Ecards Team
 * @since 2025-11-03
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MemberCardVO {

    /**
     * 会员卡ID（UUID格式）
     */
    private String memberCardId;

    /**
     * 卡种ID
     */
    private Long cardTypeId;

    /**
     * 卡种名称
     */
    private String cardTypeName;

    /**
     * 卡面背景图URL
     */
    private String cardBgc;

    /**
     * 卡面样式标识
     */
    private String cardMask;

    /**
     * 店铺ID（UUID格式）
     */
    private String storeId;

    /**
     * 店铺名称
     */
    private String storeName;

    /**
     * 商家ID（UUID格式）
     */
    private String merchantId;

    /**
     * 用户ID（UUID格式，可为空）
     */
    private String userId;

    /**
     * 会员手机号（明文）
     */
    private String memberPhone;

    /**
     * 会员姓名
     */
    private String memberName;

    /**
     * 卡种类型：1-余额卡，2-次数卡，3-时效卡，4-积分卡
     */
    private Integer cardTtype;

    /**
     * 卡种类型名称
     */
    private String cardTtypeName;

    /**
     * 余额
     */
    private BigDecimal balance;

    /**
     * 剩余次数
     */
    private Integer times;

    /**
     * 积分
     */
    private Integer points;

    /**
     * 状态：0-未激活，1-正常，2-已过期，3-已冻结，4-已注销
     */
    private Integer status;

    /**
     * 状态名称
     */
    private String statusName;

    /**
     * 开卡时间（ISO8601格式）
     */
    private String openCardTime;

    /**
     * 激活时间（ISO8601格式，可为空）
     */
    private String activateTime;

    /**
     * 到期时间（ISO8601格式）
     */
    private String expireTime;

    /**
     * 是否为本店卡（true-本店卡，false-跨店卡）
     */
    private Boolean isLocalCard;

    /**
     * 原始开卡店铺ID（跨店卡时使用，UUID格式）
     */
    private String originalStoreId;

    /**
     * 原始开卡店铺名称（跨店卡时使用）
     */
    private String originalStoreName;
}

