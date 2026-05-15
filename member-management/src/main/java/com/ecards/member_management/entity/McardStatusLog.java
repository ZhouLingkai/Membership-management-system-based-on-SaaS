package com.ecards.member_management.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 会员卡状态变更记录表实体类
 * 对应数据库表 t_mcard_status_log
 */
@Entity
@Table(name = "t_mcard_status_log",
        indexes = {
                @Index(name = "idx_member_card_id", columnList = "member_card_id"),
                @Index(name = "idx_change_type", columnList = "change_type"),
                @Index(name = "idx_operator_id", columnList = "operator_id"),
                @Index(name = "idx_operator_time", columnList = "operator_time")
        })
@Data
@NoArgsConstructor
@AllArgsConstructor
public class McardStatusLog {

    /**
     * 会员卡变更记录ID，主键（自增）
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "mcardlog_id")
    private Long mcardlogId;

    /**
     * 关联会员卡ID（外键）
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
     * 变更类型
     * 0-冻结，1-解冻，2-激活，3-到期，4-注销
     */
    @Column(name = "change_type", nullable = false)
    private Integer changeType;

    /**
     * 变更前状态
     */
    @Column(name = "old_status")
    private Integer oldStatus;

    /**
     * 变更后状态
     */
    @Column(name = "new_status", nullable = false)
    private Integer newStatus;

    /**
     * 变更原因
     */
    @Column(name = "change_reason", length = 60)
    private String changeReason;

    /**
     * 操作人ID（外键）
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
     * 操作人角色
     * 0-商家，1-店长，2-店员，3-用户本人
     */
    @Column(name = "operator_role", nullable = false)
    private Integer operatorRole;

    /**
     * 操作时间
     */
    @Column(name = "operator_time", nullable = false)
    private LocalDateTime operatorTime;

    /**
     * 创建时自动设置时间
     */
    @PrePersist
    protected void onCreate() {
        if (operatorTime == null) {
            operatorTime = LocalDateTime.now();
        }
    }
}

