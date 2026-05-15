package com.ecards.member_management.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 预约模板表
 */
@Entity
@Table(name = "t_reservation_template")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReservationTemplate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "reserve_id")
    private Long reserveId;

    @Column(name = "store_id", nullable = false, length = 16)
    private byte[] storeId;

    @Column(name = "reservation_time_list", nullable = false, length = 300)
    @JdbcTypeCode(SqlTypes.JSON)
    private List<String> reservationTimeList;

    @Column(name = "cancel_rule", nullable = false, length = 50)
    @JdbcTypeCode(SqlTypes.JSON)
    private List<String> cancelRule;

    @Column(name = "advance_days", nullable = false)
    private Integer advanceDays;

    @Column(name = "forbidden_days", length = 300)
    @JdbcTypeCode(SqlTypes.JSON)
    private List<String> forbiddenDays;

    @Column(name = "customize_forbidden", nullable = false)
    private Integer customizeForbidden;

    @Column(name = "effective_start_time", nullable = false)
    private LocalDate effectiveStartTime;

    @Column(name = "effective_end_time")
    private LocalDate effectiveEndTime;

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
    }

    @PreUpdate
    protected void onUpdate() {
        updateTime = LocalDateTime.now();
    }
}
