package com.ecards.member_management.repository;

import com.ecards.member_management.entity.PointsRecord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

/**
 * 积分记录Repository
 */
@Repository
public interface PointsRecordRepository extends JpaRepository<PointsRecord, Long>, JpaSpecificationExecutor<PointsRecord> {
    
    /**
     * 根据会员卡ID查询积分记录（支持日期范围和分页）
     */
    @Query("SELECT pr FROM PointsRecord pr " +
           "WHERE pr.memberCardId = :memberCardId " +
           "AND (:startDate IS NULL OR pr.createTime >= :startDate) " +
           "AND (:endDate IS NULL OR pr.createTime <= :endDate) " +
           "ORDER BY pr.createTime DESC")
    Page<PointsRecord> findByMemberCardIdAndDateRange(
        @Param("memberCardId") byte[] memberCardId,
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate,
        Pageable pageable
    );

    /**
     * 根据用户ID查询积分记录（用于用户本人查询）
     */
    @Query("SELECT pr FROM PointsRecord pr " +
           "WHERE pr.userId = :userId " +
           "AND (:startDate IS NULL OR pr.createTime >= :startDate) " +
           "AND (:endDate IS NULL OR pr.createTime <= :endDate) " +
           "ORDER BY pr.createTime DESC")
    Page<PointsRecord> findByUserIdAndDateRange(
        @Param("userId") byte[] userId,
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate,
        Pageable pageable
    );
}

