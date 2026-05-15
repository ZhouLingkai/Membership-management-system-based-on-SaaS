package com.ecards.member_management.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 会员卡消费响应DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConsumeResponse {

    /**
     * 交易记录ID
     */
    private Long transactionId;

    /**
     * 会员卡ID
     */
    private String memberCardId;

    /**
     * 卡种名称
     */
    private String cardTypeName;

    /**
     * 卡种类型：1-余额卡，2-次数卡
     */
    private Integer cardTtype;

    /**
     * 消费金额/次数
     */
    private BigDecimal consumeAmount;

    /**
     * 消费后余额/次数
     */
    private BigDecimal balanceSnapshot;

    /**
     * 交易时间
     */
    private String transactionTime;

    /**
     * 本次消费获得的积分（0表示未获得积分）
     */
    private Integer earnedPoints;
}

