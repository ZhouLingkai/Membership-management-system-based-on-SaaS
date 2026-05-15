package com.ecards.member_management.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

/**
 * 工作关系表实体类
 * 对应数据库表 t_work_relation
 * 记录员工与店铺的工作关系
 */
@Entity
@Table(name = "t_work_relation", 
       indexes = {
           @Index(name = "idx_store_id", columnList = "store_id"),
           @Index(name = "idx_merchant_id", columnList = "merchant_id"),
           @Index(name = "idx_user_id", columnList = "user_id"),
           @Index(name = "idx_role", columnList = "role")
       },
       uniqueConstraints = {
           @UniqueConstraint(name = "uk_store_user", columnNames = {"store_id", "user_id"})
       })
@Data
@NoArgsConstructor
@AllArgsConstructor
public class WorkRelation {

    /**
     * 关系ID，主键（自增）
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    /**
     * 店铺ID（外键，关联t_store.store_id）
     */
    @Column(name = "store_id", columnDefinition = "BINARY(16)", nullable = false)
    private byte[] storeId;

    /**
     * 店铺关联（多对一关系）
     */
    @ManyToOne
    @JoinColumn(name = "store_id", referencedColumnName = "store_id", insertable = false, updatable = false)
    private Store store;

    /**
     * 所属商家ID（冗余字段，方便跨店铺查询）
     */
    @Column(name = "merchant_id", columnDefinition = "BINARY(16)", nullable = false)
    private byte[] merchantId;

    /**
     * 用户ID（外键，关联t_user.user_id）
     */
    @Column(name = "user_id", columnDefinition = "BINARY(16)", nullable = false)
    private byte[] userId;

    /**
     * 用户关联（多对一关系）
     */
    @ManyToOne
    @JoinColumn(name = "user_id", referencedColumnName = "user_id", insertable = false, updatable = false)
    private User user;

    /**
     * 角色
     * manager - 店长
     * employee - 店员
     */
    @Column(name = "role", length = 20, nullable = false)
    private String role;

    /**
     * 员工姓名（绑定时商家填写）
     */
    @Column(name = "name", length = 30)
    private String name;

    /**
     * 权限列表（JSON格式）
     * 示例：{"manager": ["staff_add"], "employee": ["member_card_create", "transaction_recharge"]}
     */
    @Column(name = "permission", columnDefinition = "JSON", nullable = false)
    @JdbcTypeCode(SqlTypes.JSON)
    private String permission;

    /**
     * 工作令牌版本号
     * 角色/权限修改时递增，确保旧令牌失效
     */
    @Column(name = "token_version", nullable = false)
    private Integer tokenVersion = 1;

    /**
     * 备注
     */
    @Column(name = "remark", length = 100)
    private String remark = "";

    /**
     * 状态
     * 0 - 离职
     * 1 - 在职
     */
    @Column(name = "status")
    private Integer status = 1;

    /**
     * 入职时间
     */
    @Column(name = "entry_time", nullable = false)
    private LocalDateTime entryTime;

    /**
     * 更新时间
     */
    @Column(name = "update_time", nullable = false)
    private LocalDateTime updateTime;

    /**
     * 创建时自动设置时间
     */
    @PrePersist
    protected void onCreate() {
        entryTime = LocalDateTime.now();
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

