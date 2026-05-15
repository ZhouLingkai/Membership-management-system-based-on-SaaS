package com.ecards.member_management.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 积分记录表实体类
 * 对应数据库表 t_points_record
 */
@Entity
@Table(name = "t_points_record",
        indexes = {
                @Index(name = "idx_member_card_id", columnList = "member_card_id"),
                @Index(name = "idx_user_id", columnList = "user_id"),
                @Index(name = "idx_merchant_id", columnList = "merchant_id"),
                @Index(name = "idx_trans_store_id", columnList = "trans_store_id"),
                @Index(name = "idx_create_time", columnList = "create_time"),
                @Index(name = "idx_operator_id", columnList = "operator_id"),
                @Index(name = "idx_card_time", columnList = "member_card_id, create_time"),
                @Index(name = "idx_user_time", columnList = "user_id, create_time")
        })
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PointsRecord {

    /**
     * 积分记录ID，主键（自增）
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "points_record_id", nullable = false)
    private Long pointsRecordId;

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
     * 操作店铺ID
     */
    @Column(name = "trans_store_id", columnDefinition = "BINARY(16)", nullable = false)
    private byte[] transStoreId;

    /**
     * 操作店铺关联（多对一关系）
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "trans_store_id", referencedColumnName = "store_id", insertable = false, updatable = false)
    private Store transStore;

    /**
     * 积分变动值（正数为增加，负数为扣减）
     */
    @Column(name = "points_change", nullable = false)
    private Integer pointsChange;

    /**
     * 变动后积分余额
     */
    @Column(name = "points_snapshot", nullable = false)
    private Integer pointsSnapshot;

    /**
     * 操作人ID
     */
    @Column(name = "operator_id", columnDefinition = "BINARY(16)", nullable = false)
    private byte[] operatorId;

    /**
     * 操作人关联（多对一关系）
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "operator_id", referencedColumnName = "user_id", insertable = false, updatable = false)
    private User operator;

    /**
     * 变动原因（必填）
     */
    @Column(name = "remark", length = 60, nullable = false)
    private String remark;

    /**
     * 创建时间
     */
    @Column(name = "create_time", nullable = false)
    private LocalDateTime createTime;

    // ========== 以下为非数据库字段，用于VO转换 ==========

    /**
     * 操作人姓名（冗余字段）
     */
    @Transient
    private String operatorName;

    /**
     * 操作店铺名称（冗余字段）
     */
    @Transient
    private String storeName;

    /**
     * 创建时自动设置时间
     */
    @PrePersist
    protected void onCreate() {
        if (createTime == null) {
            createTime = LocalDateTime.now();
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
    }
}

