package com.ecards.member_management.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

/**
 * 会员卡种表实体类
 * 对应数据库表 t_member_card_type
 */
@Entity
@Table(name = "t_member_card_type",
        indexes = {
                @Index(name = "idx_store_id", columnList = "store_id"),
                @Index(name = "idx_merchant_id", columnList = "merchant_id"),
                @Index(name = "idx_card_type_name", columnList = "card_type_name")
        })
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MemberCardType {

    /**
     * 卡种ID，主键（自增）
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "card_type_id")
    private Long cardTypeId;

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
     * 商家ID（外键，冗余字段提高性能）
     */
    @Column(name = "merchant_id", columnDefinition = "BINARY(16)", nullable = false)
    private byte[] merchantId;

    /**
     * 卡种名称
     */
    @Column(name = "card_type_name", length = 50, nullable = false)
    private String cardTypeName;

    /**
     * 卡种描述
     */
    @Column(name = "description", length = 300)
    private String description;

    /**
     * 卡面样式标识
     * 用于标识会员卡的显示样式或模板
     */
    @Column(name = "card_mask", length = 100)
    private String cardMask;

    /**
     * 卡种背景图URL
     * 数字或OSS路径
     */
    @Column(name = "card_bgc", length = 100)
    private String cardBgc;

    /**
     * 卡种类型
     * 1-余额卡，2-次数卡，3-时效卡，4-积分卡
     * 创建后不可修改
     */
    @Column(name = "card_ttype", nullable = false)
    private Integer cardTtype;

    /**
     * 预设充值项目（JSON格式）
     * 示例：[{"itemName":"充值100元","itemDesc":"首次充值赠送10元","amount":110.00}]
     */
    @Column(name = "preset_recharge", columnDefinition = "JSON", nullable = false)
    @JdbcTypeCode(SqlTypes.JSON)
    private String presetRecharge;

    /**
     * 预设消费项目（JSON格式）
     * 示例：[{"itemName":"洗车服务","itemDesc":"普通洗车一次","amount":30.00}]
     */
    @Column(name = "preset_cost", columnDefinition = "JSON", nullable = false)
    @JdbcTypeCode(SqlTypes.JSON)
    private String presetCost;

    /**
     * 自动消息通知类型
     * 0-关闭，1-短信通知，2-订阅通知，3-程序内推送
     */
    @Column(name = "auto_notify", nullable = false)
    private Integer autoNotify = 0;

    /**
     * 跨店通用设置
     * 0-仅本店铺，1-同商家跨店通用
     */
    @Column(name = "cross_store", nullable = false)
    private Integer crossStore = 0;

    /**
     * 创建时间
     */
    @Column(name = "create_time", nullable = false)
    private LocalDateTime createTime;

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
    }
}

