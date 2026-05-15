package com.ecards.member_management.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

/**
 * 用户表实体类
 * 对应数据库表 t_user
 */
@Entity
@Table(name = "t_user", indexes = {
        @Index(name = "idx_user_type", columnList = "user_type"),
        @Index(name = "idx_register_time", columnList = "register_time")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class User {

    /**
     * 用户ID，主键（UUID，存储为BINARY(16)）
     */
    @Id
    @Column(name = "user_id", columnDefinition = "BINARY(16)", nullable = false)
    private byte[] userId;

    /**
     * 手机号（明文存储，前后端传输时AES加密保护）
     */
    @Column(name = "phone", length = 15, nullable = false, unique = true)
    private String phone;

    /**
     * 密码（argon2加密存储，Argon2哈希长度约97字符）
     */
    @Column(name = "password", length = 128, nullable = false)
    private String password;

    /**
     * 会员昵称
     */
    @Column(name = "nickname", length = 30, nullable = false)
    private String nickname;

    /**
     * 用户头像URL
     */
    @Column(name = "avatar", length = 100)
    private String avatar;

    /**
     * 会员头像（用于商家验证是否本人交易）
     */
    @Column(name = "member_avatar", length = 100)
    private String memberAvatar;

    /**
     * 用户类型
     * 1 - 普通用户
     * 2 - 商家用户
     * 3 - 员工用户
     */
    @Column(name = "user_type", nullable = false)
    private Integer userType;

    /**
     * 邀请码（用户自己的邀请码，供他人注册时填写）
     */
    @Column(name = "invite_code", length = 10, unique = true)
    private String inviteCode;

    /**
     * 邀请者的邀请码（注册时填写的别人的邀请码）
     */
    @Column(name = "invited_code", length = 10)
    private String invitedCode;

    /**
     * 最后登录时间
     */
    @Column(name = "last_login_time")
    private LocalDateTime lastLoginTime;

    /**
     * 令牌版本号（用于全局注销令牌）
     * 每次密码重置/全设备退出时递增，使所有旧令牌失效
     */
    @Column(name = "token_version", nullable = false)
    private Integer tokenVersion = 0;

    /**
     * 注册时间
     */
    @Column(name = "register_time", nullable = false)
    private LocalDateTime registerTime;

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
        registerTime = LocalDateTime.now();
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

