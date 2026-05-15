package com.ecards.member_management.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

/**
 * 商家信息扩展表实体类
 * 对应数据库表 t_merchant_extend
 */
@Entity
@Table(name = "t_merchant_extend", indexes = {
        @Index(name = "idx_merchant_name", columnList = "merchant_name"),
        @Index(name = "idx_merchant_level", columnList = "merchant_level"),
        @Index(name = "idx_privilege_expire_time", columnList = "privilege_expire_time")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MerchantExtend {

    /**
     * 商家ID，主键（UUID，存储为BINARY(16)）
     */
    @Id
    @Column(name = "merchant_id", columnDefinition = "BINARY(16)", nullable = false)
    private byte[] merchantId;

    /**
     * 用户ID（外键，关联t_user.user_id）
     */
    @Column(name = "user_id", columnDefinition = "BINARY(16)", nullable = false, unique = true)
    private byte[] userId;

    /**
     * 用户关联（一对一关系）
     */
    @OneToOne
    @JoinColumn(name = "user_id", referencedColumnName = "user_id", insertable = false, updatable = false)
    private User user;

    /**
     * 商家名称
     */
    @Column(name = "merchant_name", length = 40)
    private String merchantName;

    /**
     * 联系邮箱（明文存储，不脱敏）
     */
    @Column(name = "contact_email", length = 255)
    private String contactEmail;

    /**
     * 商户简介
     */
    @Column(name = "merchant_intro", length = 300)
    private String merchantIntro;

    /**
     * 商家认证状态
     * 1 - 已认证（审核通过的正式商户）
     * 2 - 未认证测试中（免认证通道，7天体验期）
     * 3 - 审核中（提交资质等待审核）
     * 4 - 审核拒绝（审核未通过）
     * 5 - 未认证测试期过（7天内未提交认证）
     * 6 - 认证存疑（预留）
     */
    @Column(name = "certification", nullable = false)
    private Integer certification;

    /**
     * 商家特权等级
     * 1 - 普通
     * 2 - VIP
     * 3 - SVIP
     * 4 - 至尊VIP
     */
    @Column(name = "merchant_level", nullable = false)
    private Integer merchantLevel;

    /**
     * 二级密码（argon2加密存储）
     */
    @Column(name = "snd_pswd", length = 128, nullable = false)
    private String sndPswd;

    /**
     * 商家特权过期时间
     */
    @Column(name = "privilege_expire_time", nullable = false)
    private LocalDateTime privilegeExpireTime;

    /**
     * 剩余消息通知次数
     */
    @Column(name = "remaining_notice_count", nullable = false)
    private Integer remainingNoticeCount = 0;

    /**
     * 店铺数量上限
     */
    @Column(name = "maximum_store_limit", nullable = false)
    private Integer maximumStoreLimit = 2;

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

    /**
     * 创建时自动设置时间
     */
    @PrePersist
    protected void onCreate() {
        createTime = LocalDateTime.now();
        updateTime = LocalDateTime.now();
    }

    /**
     * 更新时自动设置时间
     */
    @PreUpdate
    protected void onUpdate() {
        updateTime = LocalDateTime.now();
    }
}

