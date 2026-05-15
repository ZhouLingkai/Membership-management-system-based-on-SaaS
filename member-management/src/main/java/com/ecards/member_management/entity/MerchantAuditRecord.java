package com.ecards.member_management.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 商户审核记录表实体类
 * 对应数据库表 t_merchant_audit_record
 */
@Entity
@Table(name = "t_merchant_audit_record", indexes = {
        @Index(name = "idx_user_id", columnList = "user_id"),
        @Index(name = "idx_application_method", columnList = "application_method"),
        @Index(name = "idx_audit_status", columnList = "audit_status"),
        @Index(name = "idx_auditor_id", columnList = "auditor_id"),
        @Index(name = "idx_audit_time", columnList = "audit_time")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MerchantAuditRecord {

    /**
     * 审核ID，主键（自增）
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "audit_id", nullable = false)
    private Long auditId;

    /**
     * 申请用户ID（外键，关联t_user）
     */
    @Column(name = "user_id", columnDefinition = "BINARY(16)", nullable = false)
    private byte[] userId;

    /**
     * 店铺规模
     */
    @Column(name = "num_stores", nullable = false)
    private Integer numStores;

    /**
     * 会员规模
     */
    @Column(name = "num_members", length = 100, nullable = false)
    private String numMembers;

    /**
     * 第一家店铺名称（任选1家，便于审核开通，审核通过后自动为用户扩展商家，自动创建第一家店铺）
     */
    @Column(name = "store_name", length = 64, nullable = false)
    private String storeName;

    /**
     * 门头店照（OSS URL）
     */
    @Column(name = "store_photos", length = 500, nullable = false)
    private String storePhotos;

    /**
     * 营业执照（OSS URL）
     */
    @Column(name = "business_license", length = 500, nullable = false)
    private String businessLicense;

    /**
     * 申请方式
     * 1 - 直接认证
     * 2 - 免认证后续补充
     */
    @Column(name = "application_method", nullable = false)
    private Integer applicationMethod;

    /**
     * 审核状态
     * 0 - 待审核
     * 1 - 已通过
     * 2 - 已拒绝
     */
    @Column(name = "audit_status", nullable = false)
    private Integer auditStatus;

    /**
     * 审核人ID（关联管理员用户ID）
     */
    @Column(name = "auditor_id", columnDefinition = "BINARY(16)")
    private byte[] auditorId;

    /**
     * 拒绝原因
     */
    @Column(name = "reject_reason", length = 200)
    private String rejectReason;

    /**
     * 审核时间
     */
    @Column(name = "audit_time")
    private LocalDateTime auditTime;

    /**
     * 申请时间
     */
    @Column(name = "create_time", nullable = false)
    private LocalDateTime createTime;

    /**
     * 创建时自动设置时间
     */
    @PrePersist
    protected void onCreate() {
        createTime = LocalDateTime.now();
    }
}

