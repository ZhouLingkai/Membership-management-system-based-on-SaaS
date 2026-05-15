package com.ecards.member_management.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 交易记录值对象
 * 
 * @author Ecards Team
 * @since 2025-11-05
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionRecordVO {

    /**
     * 交易记录ID
     */
    private Long transactionId;

    /**
     * 会员卡ID（接口5需要）
     */
    private String memberCardId;

    /**
     * 卡种名称
     */
    private String cardTypeName;

    /**
     * 店铺名称
     */
    private String storeName;

    /**
     * 交易类型：1-充值，2-消费，3-退款，4-延期，5-日期变动
     */
    private Integer transactionType;

    /**
     * 交易类型名称
     */
    private String transactionTypeName;

    /**
     * 交易金额/次数/天数
     */
    private BigDecimal amount;

    /**
     * 交易后余额/次数（接口4需要）
     */
    private BigDecimal balanceSnapshot;

    /**
     * 操作人姓名（接口4需要）
     */
    private String operatorName;

    /**
     * 交易备注（接口4需要）
     */
    private String remark;

    /**
     * 交易时间
     */
    private String transactionTime;
}

