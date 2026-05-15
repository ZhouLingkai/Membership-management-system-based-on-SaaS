package com.ecards.member_management.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

/**
 * 店铺表实体类
 * 对应数据库表 t_store
 */
@Entity
@Table(name = "t_store", indexes = {
        @Index(name = "idx_merchant_id", columnList = "merchant_id"),
        @Index(name = "idx_store_name", columnList = "store_name"),
        @Index(name = "idx_status", columnList = "status")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Store {

    /**
     * 店铺ID，主键（UUID，存储为BINARY(16)）
     */
    @Id
    @Column(name = "store_id", columnDefinition = "BINARY(16)", nullable = false)
    private byte[] storeId;

    /**
     * 商家ID（外键，关联t_merchant_extend.merchant_id）
     */
    @Column(name = "merchant_id", columnDefinition = "BINARY(16)", nullable = false)
    private byte[] merchantId;

    /**
     * 商家关联（多对一关系）
     */
    @ManyToOne
    @JoinColumn(name = "merchant_id", referencedColumnName = "merchant_id", insertable = false, updatable = false)
    private MerchantExtend merchantExtend;

    /**
     * 店铺名称
     */
    @Column(name = "store_name", length = 40, nullable = false)
    private String storeName;

    /**
     * 店铺类型（CONVENIENCE-便利店、RESTAURANT-餐饮等）
     */
    @Column(name = "store_type", length = 24)
    private String storeType;

    /**
     * 店铺地址
     */
    @Column(name = "address", length = 128)
    private String address;

    /**
     * 门头店照
     */
    @Column(name = "store_photos", length = 300, nullable = false)
    private String storePhotos;

    /**
     * 营业执照
     */
    @Column(name = "business_license", length = 100, nullable = false)
    private String businessLicense;

    /**
     * 联系电话（明文存储）
     */
    @Column(name = "contact_phone", length = 15, nullable = false)
    private String contactPhone;

    /**
     * 联系微信号（明文存储）
     */
    @Column(name = "contact_wx", length = 40, nullable = false)
    private String contactWx;

    /**
     * 店铺状态
     * 0 - 关闭
     * 1 - 正常营业
     * 2 - 暂停营业
     * 3 - 店铺注销
     */
    @Column(name = "status", nullable = false)
    private Integer status = 1;

    /**
     * 营业时间（文字描述）
     */
    @Column(name = "business_time", length = 50)
    private String businessTime;

    /**
     * 是否支持预约：1开启，0关闭
     */
    @Column(name = "appointment", nullable = false)
    private Integer appointment = 0;

    /**
     * 建店时间
     */
    @Column(name = "open_store_time")
    private LocalDateTime openStoreTime;

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

