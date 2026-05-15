package com.ecards.member_management.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 管理员表实体类
 * 对应数据库表 t_admin
 */
@Entity
@Table(name = "t_admin", indexes = {
        @Index(name = "idx_phone", columnList = "phone"),
        @Index(name = "idx_account", columnList = "account"),
        @Index(name = "idx_admin_role", columnList = "admin_role"),
        @Index(name = "idx_status", columnList = "status")
}, uniqueConstraints = {
        @UniqueConstraint(name = "uk_phone", columnNames = "phone"),
        @UniqueConstraint(name = "uk_account", columnNames = "account")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Admin {

    /**
     * 管理员ID，主键（UUID二进制存储）
     */
    @Id
    @Column(name = "admin_id", columnDefinition = "BINARY(16)", nullable = false)
    private byte[] adminId;

    /**
     * 手机号（明文，用于验证码找回密码）
     */
    @Column(name = "phone", length = 15, nullable = false, unique = true)
    private String phone;

    /**
     * 登录账号
     */
    @Column(name = "account", length = 50, nullable = false, unique = true)
    private String account;

    /**
     * 密码（Argon2加密）
     */
    @Column(name = "password", length = 128, nullable = false)
    private String password;

    /**
     * 二级密码（Argon2加密，用于危险操作确认）
     */
    @Column(name = "snd_pswd", length = 128, nullable = false)
    private String sndPswd;

    /**
     * 角色
     * 1 - 超级管理员
     * 2 - 审核员
     */
    @Column(name = "admin_role", nullable = false)
    private Integer adminRole;

    /**
     * 令牌版本号（密码修改时递增）
     */
    @Column(name = "token_version", nullable = false)
    private Integer tokenVersion;

    /**
     * 状态
     * 1 - 启用
     * 0 - 禁用
     */
    @Column(name = "status", nullable = false)
    private Integer status;

    /**
     * IP白名单（逗号分隔，预留字段）
     */
    @Column(name = "allowed_ips", length = 225)
    private String allowedIps;

    /**
     * 最后登录时间
     */
    @Column(name = "last_login_time")
    private LocalDateTime lastLoginTime;

    /**
     * 最后登录IP
     */
    @Column(name = "last_login_ip", length = 45)
    private String lastLoginIp;

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
     * 创建者ID（超管创建时填写）
     */
    @Column(name = "creator_id", columnDefinition = "BINARY(16)")
    private byte[] creatorId;

    /**
     * 备注
     */
    @Column(name = "remark", length = 100)
    private String remark;

    /**
     * 创建时自动设置时间
     */
    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        createTime = now;
        updateTime = now;
        if (tokenVersion == null) {
            tokenVersion = 1;
        }
        if (status == null) {
            status = 1;
        }
    }

    /**
     * 更新时自动设置时间
     */
    @PreUpdate
    protected void onUpdate() {
        updateTime = LocalDateTime.now();
    }
}

