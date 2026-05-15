package com.ecards.member_management.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 办卡记录表实体
 * 
 * @author Ecards Team
 * @since 2025-11-03
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "t_registration_card_record")
public class RegistrationCardRecord {

    /**
     * 办卡记录ID（主键，自增）
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "registration_id", nullable = false)
    private Long registrationId;

    /**
     * 会员卡ID（外键）
     */
    @Column(name = "member_card_id", nullable = false, columnDefinition = "BINARY(16)")
    private byte[] memberCardId;

    /**
     * 操作员ID（办卡操作人）
     */
    @Column(name = "operator_id", nullable = false, columnDefinition = "BINARY(16)")
    private byte[] operatorId;

    /**
     * 办卡渠道
     * 0-线下二维码
     * 1-先办后激活（手机号快速办理）
     * 2-线上领卡
     * 3-批量办卡
     */
    @Column(name = "registration_channel", nullable = false)
    private Integer registrationChannel;

    /**
     * 操作员角色
     * 0-商家
     * 1-店长
     * 2-店员
     */
    @Column(name = "operator_role", nullable = false)
    private Integer operatorRole;

    /**
     * 办卡店铺ID
     */
    @Column(name = "trans_store_id", nullable = false, columnDefinition = "BINARY(16)")
    private byte[] transStoreId;

    /**
     * 办卡时间
     */
    @Column(name = "registration_time", nullable = false)
    private LocalDateTime registrationTime;

    /**
     * 保存前自动设置时间
     */
    @PrePersist
    public void prePersist() {
        if (this.registrationTime == null) {
            this.registrationTime = LocalDateTime.now();
        }
    }
}

