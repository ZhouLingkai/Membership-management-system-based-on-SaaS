package com.ecards.member_management.repository;

import com.ecards.member_management.entity.ReservationTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * 预约模板Repository
 */
@Repository
public interface ReservationTemplateRepository extends JpaRepository<ReservationTemplate, Long> {

    /**
     * 查询店铺的有效模板
     */
    @Query("SELECT t FROM ReservationTemplate t WHERE t.storeId = :storeId " +
           "AND t.effectiveStartTime <= :currentDate " +
           "AND (t.effectiveEndTime IS NULL OR t.effectiveEndTime >= :currentDate)")
    Optional<ReservationTemplate> findEffectiveTemplate(
            @Param("storeId") byte[] storeId,
            @Param("currentDate") LocalDate currentDate
    );

    /**
     * 查询店铺的所有模板
     */
    List<ReservationTemplate> findByStoreId(byte[] storeId);

    /**
     * 检查时间段是否重叠
     */
    @Query("SELECT COUNT(t) > 0 FROM ReservationTemplate t WHERE t.storeId = :storeId " +
           "AND ((t.effectiveStartTime <= :endTime AND (t.effectiveEndTime IS NULL OR t.effectiveEndTime >= :startTime)))")
    boolean existsOverlappingTemplate(
            @Param("storeId") byte[] storeId,
            @Param("startTime") LocalDate startTime,
            @Param("endTime") LocalDate endTime
    );

    /**
     * 查询所有需要清理过期日期的模板
     */
    @Query("SELECT t FROM ReservationTemplate t WHERE t.forbiddenDays IS NOT NULL")
    List<ReservationTemplate> findAllWithForbiddenDays();
}
