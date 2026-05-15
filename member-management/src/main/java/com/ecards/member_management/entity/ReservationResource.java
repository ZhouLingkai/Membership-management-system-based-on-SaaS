package com.ecards.member_management.entity;

import com.ecards.member_management.converter.StringListConverter;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 预约资源表
 */
@Entity
@Table(name = "t_reservation_resource")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReservationResource {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "resource_id")
    private Long resourceId;

    @Column(name = "store_id", nullable = false, length = 16)
    private byte[] storeId;

    @Column(name = "resource_name", nullable = false, length = 20)
    private String resourceName;

    @Column(name = "is_reservable", nullable = false)
    private Integer isReservable;

    @Column(name = "support_card_types", nullable = false)
    private Integer supportCardTypes;

    @Column(name = "min_continuous_time")
    private Integer minContinuousTime;

    @Column(name = "max_continuous_time")
    private Integer maxContinuousTime;

    @Column(name = "unit_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal unitPrice;

    @Column(name = "resource_img", length = 100)
    private String resourceImg;

    @Column(name = "resource_desc", length = 150)
    private String resourceDesc;

    @Column(name = "down_time")
    private LocalDateTime downTime;

    @Column(name = "forbidden_days", length = 300)
    @Convert(converter = StringListConverter.class)
    private List<String> forbiddenDays;

    @Column(name = "promotion_strategy", columnDefinition = "JSON")
    @JdbcTypeCode(SqlTypes.JSON)
    private Map<String, Object> promotionStrategy;

    @Column(name = "create_time", nullable = false, updatable = false)
    private LocalDateTime createTime;

    @Column(name = "update_time", nullable = false)
    private LocalDateTime updateTime;

    @Column(name = "ext_json", columnDefinition = "JSON")
    @JdbcTypeCode(SqlTypes.JSON)
    private Map<String, Object> extJson;

    @PrePersist
    protected void onCreate() {
        createTime = LocalDateTime.now();
        updateTime = LocalDateTime.now();
        if (isReservable == null) {
            isReservable = 1;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updateTime = LocalDateTime.now();
    }
}
