package com.ecards.member_management.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 管理员操作日志表实体类
 * 对应数据库表 t_admin_operation_log
 */
@Entity
@Table(name = "t_admin_operation_log", indexes = {
        @Index(name = "idx_admin_id", columnList = "admin_id"),
        @Index(name = "idx_operation_type", columnList = "operation_type"),
        @Index(name = "idx_target_id", columnList = "target_id"),
        @Index(name = "idx_operation_time", columnList = "operation_time"),
        @Index(name = "idx_result", columnList = "result")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdminOperationLog {

    /**
     * 日志ID，主键（自增）
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "log_id", nullable = false)
    private Long logId;

    /**
     * 操作管理员ID
     */
    @Column(name = "admin_id", columnDefinition = "BINARY(16)", nullable = false)
    private byte[] adminId;

    /**
     * 管理员账号（冗余存储，便于查询）
     */
    @Column(name = "admin_account", length = 50, nullable = false)
    private String adminAccount;

    /**
     * 操作类型
     * AUDIT_PASS - 审核通过
     * AUDIT_REJECT - 审核拒绝
     * WARN_MERCHANT - 商户警告
     * BAN_MERCHANT - 商户封禁
     * LEVEL_UPDATE - 等级修改
     * CREATE_ADMIN - 创建管理员
     * UPDATE_PASSWORD - 修改密码
     * RESET_PASSWORD - 重置密码
     * UPDATE_SND_PASSWORD - 修改二级密码
     * ADMIN_LOGIN - 管理员登录
     */
    @Column(name = "operation_type", length = 50, nullable = false)
    private String operationType;

    /**
     * 操作对象类型
     * MERCHANT - 商户
     * ADMIN - 管理员
     * SYSTEM - 系统操作
     */
    @Column(name = "target_type", length = 20, nullable = false)
    private String targetType;

    /**
     * 操作对象ID（商户ID或管理员ID）
     */
    @Column(name = "target_id", columnDefinition = "BINARY(16)")
    private byte[] targetId;

    /**
     * 操作描述
     */
    @Column(name = "operation_desc", length = 150, nullable = false)
    private String operationDesc;

    /**
     * 请求参数（JSON格式）
     */
    @Column(name = "request_params", columnDefinition = "TEXT")
    private String requestParams;

    /**
     * 操作IP
     */
    @Column(name = "operation_ip", length = 45, nullable = false)
    private String operationIp;

    /**
     * 设备ID
     */
    @Column(name = "device_id", length = 60)
    private String deviceId;

    /**
     * 操作时间
     */
    @Column(name = "operation_time", nullable = false)
    private LocalDateTime operationTime;

    /**
     * 操作结果
     * 1 - 成功
     * 0 - 失败
     */
    @Column(name = "result", nullable = false)
    private Integer result;

    /**
     * 错误信息（失败时记录）
     */
    @Column(name = "error_msg", length = 150)
    private String errorMsg;

    /**
     * 创建时自动设置时间
     */
    @PrePersist
    protected void onCreate() {
        if (operationTime == null) {
            operationTime = LocalDateTime.now();
        }
        if (result == null) {
            result = 1;
        }
    }
}

