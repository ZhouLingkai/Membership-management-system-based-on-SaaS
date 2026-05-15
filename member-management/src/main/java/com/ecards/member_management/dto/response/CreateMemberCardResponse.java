package com.ecards.member_management.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 会员卡办理响应DTO
 * 
 * @author Ecards Team
 * @since 2025-11-03
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateMemberCardResponse {

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
     * 卡种类型
     */
    private Integer cardTtype;

    /**
     * 卡种类型名称
     */
    private String cardTtypeName;

    /**
     * 会员手机号（明文）
     */
    private String memberPhone;

    /**
     * 会员姓名
     */
    private String memberName;

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
     * 到期时间（ISO8601格式）
     */
    private String expireTime;

    /**
     * 提示信息（如：该手机号尚未注册，会员卡将处于"未激活"状态）
     */
    private String message;
}

