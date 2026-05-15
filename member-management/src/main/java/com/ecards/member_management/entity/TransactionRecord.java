package com.ecards.member_management.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 交易记录表实体类
 * 对应数据库表 t_transaction_record
 */
@Entity
@Table(name = "t_transaction_record",
        indexes = {
                @Index(name = "idx_member_card_id", columnList = "member_card_id"),
                @Index(name = "idx_user_id", columnList = "user_id"),
                @Index(name = "idx_trans_store_id", columnList = "trans_store_id"),
                @Index(name = "idx_merchant_id", columnList = "merchant_id"),
                @Index(name = "idx_transaction_type", columnList = "transaction_type"),
                @Index(name = "idx_transaction_time", columnList = "transaction_time"),
                @Index(name = "idx_operator_id", columnList = "operator_id"),
                @Index(name = "idx_store_time", columnList = "trans_store_id, transaction_time"),
                @Index(name = "idx_merchant_time", columnList = "merchant_id, transaction_time"),
                @Index(name = "idx_user_time", columnList = "user_id, transaction_time"),
                @Index(name = "idx_card_time", columnList = "member_card_id, transaction_time"),
                @Index(name = "idx_store_type_time", columnList = "trans_store_id, transaction_type, transaction_time")
        })
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TransactionRecord {

    /**
     * 交易ID，主键（自增）
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "transaction_id", nullable = false)
    private Long transactionId;

    /**
     * 会员卡ID（外键）
     */
    @Column(name = "member_card_id", columnDefinition = "BINARY(16)", nullable = false)
    private byte[] memberCardId;

    /**
     * 会员卡关联（多对一关系）
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_card_id", referencedColumnName = "member_card_id", insertable = false, updatable = false)
    private MemberCard memberCard;

    /**
     * 用户ID（冗余字段）
     */
    @Column(name = "user_id", columnDefinition = "BINARY(16)")
    private byte[] userId;

    /**
     * 用户关联（多对一关系）
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", referencedColumnName = "user_id", insertable = false, updatable = false)
    private User user;

    /**
     * 商家ID（冗余字段）
     */
    @Column(name = "merchant_id", columnDefinition = "BINARY(16)", nullable = false)
    private byte[] merchantId;

    /**
     * 商家关联（多对一关系）
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "merchant_id", referencedColumnName = "merchant_id", insertable = false, updatable = false)
    private MerchantExtend merchant;

    /**
     * 交易类型
     * 1-充值，2-消费，3-退款，4-延期，5-日期变动
     */
    @Column(name = "transaction_type", nullable = false)
    private Integer transactionType;

    /**
     * 交易值（金额/次数/天数，正数为入，负数为出）
     */
    @Column(name = "amount", precision = 10, scale = 2, nullable = false)
    private BigDecimal amount;

    /**
     * 余额/次数快照（交易后的值，时效卡不填）
     */
    @Column(name = "balance_snapshot", precision = 10, scale = 2)
    private BigDecimal balanceSnapshot;

    /**
     * 操作员ID
     */
    @Column(name = "operator_id", columnDefinition = "BINARY(16)", nullable = false)
    private byte[] operatorId;

    /**
     * 操作员关联（多对一关系）
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "operator_id", referencedColumnName = "user_id", insertable = false, updatable = false)
    private User operator;

    /**
     * 交易店铺ID（跨店卡关键字段）
     */
    @Column(name = "trans_store_id", columnDefinition = "BINARY(16)", nullable = false)
    private byte[] transStoreId;

    /**
     * 交易店铺关联（多对一关系）
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "trans_store_id", referencedColumnName = "store_id", insertable = false, updatable = false)
    private Store transStore;

    /**
     * 交易备注（必填）
     */
    @Column(name = "remark", length = 60, nullable = false)
    private String remark;

    /**
     * 交易时间
     */
    @Column(name = "transaction_time", nullable = false)
    private LocalDateTime transactionTime;

    // ========== 以下为非数据库字段，用于VO转换 ==========

    /**
     * 操作人姓名（冗余字段）
     */
    @Transient
    private String operatorName;

    /**
     * 交易店铺名称（冗余字段）
     */
    @Transient
    private String storeName;

    /**
     * 卡种名称（冗余字段）
     */
    @Transient
    private String cardTypeName;

    /**
     * 卡种类型（冗余字段）
     */
    @Transient
    private Integer cardTtype;

    /**
     * 交易类型名称（冗余字段）
     */
    @Transient
    private String transactionTypeName;

    /**
     * 创建时自动设置时间
     */
    @PrePersist
    protected void onCreate() {
        if (transactionTime == null) {
            transactionTime = LocalDateTime.now();
        }
    }

    /**
     * 从关联对象填充冗余字段（用于VO转换）
     */
    public void fillRedundantFields() {
        if (this.operator != null) {
            this.operatorName = this.operator.getNickname();
        }
        if (this.transStore != null) {
            this.storeName = this.transStore.getStoreName();
        }
        if (this.memberCard != null) {
            this.cardTtype = this.memberCard.getCardTtype();
            if (this.memberCard.getCardType() != null) {
                this.cardTypeName = this.memberCard.getCardType().getCardTypeName();
            }
        }
    }
}

