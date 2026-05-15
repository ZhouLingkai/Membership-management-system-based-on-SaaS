package com.ecards.member_management.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 会员卡表实体类
 * 对应数据库表 t_member_card
 */
@Entity
@Table(name = "t_member_card",
        indexes = {
                @Index(name = "idx_card_type_id", columnList = "card_type_id"),
                @Index(name = "idx_store_id", columnList = "store_id"),
                @Index(name = "idx_merchant_id", columnList = "merchant_id"),
                @Index(name = "idx_user_id", columnList = "user_id"),
                @Index(name = "idx_member_phone", columnList = "member_phone"),
                @Index(name = "idx_status", columnList = "status"),
                @Index(name = "idx_card_ttype", columnList = "card_ttype"),
                @Index(name = "idx_open_card_time", columnList = "open_card_time"),
                @Index(name = "idx_activate_time", columnList = "activate_time"),
                @Index(name = "idx_expire_time", columnList = "expire_time"),
                @Index(name = "idx_store_status", columnList = "store_id, status"),
                @Index(name = "idx_merchant_status", columnList = "merchant_id, status"),
                @Index(name = "idx_user_status", columnList = "user_id, status"),
                @Index(name = "idx_merchant_user", columnList = "merchant_id, user_id"),
                @Index(name = "idx_phone_store", columnList = "member_phone, store_id")
        })
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MemberCard {

    /**
     * 会员卡ID，主键（UUID，存储为BINARY(16)）
     */
    @Id
    @Column(name = "member_card_id", columnDefinition = "BINARY(16)", nullable = false)
    private byte[] memberCardId;

    /**
     * 卡种ID（外键，关联t_member_card_type.card_type_id）
     */
    @Column(name = "card_type_id", nullable = false)
    private Long cardTypeId;

    /**
     * 卡种关联（多对一关系）
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "card_type_id", referencedColumnName = "card_type_id", insertable = false, updatable = false)
    private MemberCardType cardType;

    /**
     * 店铺ID（外键，冗余字段提高查询性能）
     */
    @Column(name = "store_id", columnDefinition = "BINARY(16)", nullable = false)
    private byte[] storeId;

    /**
     * 店铺关联（多对一关系）
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_id", referencedColumnName = "store_id", insertable = false, updatable = false)
    private Store store;

    /**
     * 商家ID（外键，冗余字段提高查询性能）
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
     * 用户ID（外键，可先办卡后认主）
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
     * 会员预留姓名
     */
    @Column(name = "member_name", length = 30)
    private String memberName;

    /**
     * 会员预留手机号（明文存储，传输加密）
     */
    @Column(name = "member_phone", length = 15)
    private String memberPhone;

    /**
     * 卡种类型（冗余字段）
     * 1-余额卡，2-次数卡，3-时效卡，4-积分卡
     */
    @Column(name = "card_ttype", nullable = false)
    private Integer cardTtype;

    /**
     * 会员卡余额（余额卡主要使用）
     */
    @Column(name = "balance", precision = 10, scale = 2)
    private BigDecimal balance = BigDecimal.ZERO;

    /**
     * 会员卡剩余次数（次数卡主要使用）
     */
    @Column(name = "times")
    private Integer times = 0;

    /**
     * 积分值（所有卡种都可使用）
     */
    @Column(name = "points")
    private Integer points = 0;

    /**
     * 累积总积分（只增不减）
     */
    @Column(name = "cumulative_points", nullable = false)
    private Integer cumulativePoints = 0;

    /**
     * 会员卡状态
     * 0-未激活，1-正常，2-已过期，3-已冻结，4-已注销
     */
    @Column(name = "status", nullable = false)
    private Integer status;

    /**
     * 开卡时间
     */
    @Column(name = "open_card_time", nullable = false)
    private LocalDateTime openCardTime;

    /**
     * 激活时间（未激活时为NULL）
     */
    @Column(name = "activate_time")
    private LocalDateTime activateTime;

    /**
     * 到期时间（时效卡主要参考）
     */
    @Column(name = "expire_time", nullable = false)
    private LocalDateTime expireTime;

    /**
     * 创建时间
     */
    @Column(name = "create_time", nullable = false)
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @Column(name = "update_time", nullable = false)
    private LocalDateTime updateTime;

    /**
     * 扩展字段（JSON格式）
     */
    @Column(name = "ext_json", columnDefinition = "JSON")
    @JdbcTypeCode(SqlTypes.JSON)
    private String extJson;

    // ========== 以下为非数据库字段，用于VO转换 ==========

    /**
     * 卡种名称（冗余字段，从cardType获取）
     */
    @Transient
    private String cardTypeName;

    /**
     * 店铺名称（冗余字段，从store获取）
     */
    @Transient
    private String storeName;

    /**
     * 商家名称（冗余字段，从merchant获取）
     */
    @Transient
    private String merchantName;

    /**
     * 卡种描述（冗余字段，从cardType获取）
     */
    @Transient
    private String cardTypeDescription;

    /**
     * 卡面背景图URL（冗余字段，从cardType获取）
     */
    @Transient
    private String cardBgc;

    /**
     * 卡面样式标识（冗余字段，从cardType获取）
     */
    @Transient
    private String cardMask;

    /**
     * 跨店通用标记（冗余字段，从cardType获取）
     */
    @Transient
    private Integer crossStore;

    /**
     * 自动消息通知（冗余字段，从cardType获取）
     */
    @Transient
    private Integer autoNotify;

    /**
     * 创建时自动设置时间
     */
    @PrePersist
    protected void onCreate() {
        createTime = LocalDateTime.now();
        updateTime = LocalDateTime.now();
        if (openCardTime == null) {
            openCardTime = LocalDateTime.now();
        }
    }

    /**
     * 更新时自动设置时间
     */
    @PreUpdate
    protected void onUpdate() {
        updateTime = LocalDateTime.now();
    }

    /**
     * 从关联对象填充冗余字段（用于VO转换）
     */
    public void fillRedundantFields() {
        if (this.cardType != null) {
            this.cardTypeName = this.cardType.getCardTypeName();
            this.cardTypeDescription = this.cardType.getDescription();
            this.cardBgc = this.cardType.getCardBgc();
            this.cardMask = this.cardType.getCardMask();
            this.crossStore = this.cardType.getCrossStore();
            this.autoNotify = this.cardType.getAutoNotify();
        }
        if (this.store != null) {
            this.storeName = this.store.getStoreName();
        }
        if (this.merchant != null) {
            this.merchantName = this.merchant.getMerchantName();
        }
    }
}

