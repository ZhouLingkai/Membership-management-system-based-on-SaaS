package com.ecards.member_management.repository;

import com.ecards.member_management.entity.ReservationRecord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

/**
 * 预约记录Repository
 */
@Repository
public interface ReservationRecordRepository extends JpaRepository<ReservationRecord, Long> {

    /**
     * 查询某资源某日的有效预约记录
     */
    @Query("SELECT r FROM ReservationRecord r WHERE r.resourceId = :resourceId " +
           "AND r.reservationDate = :date AND r.reservationStatus NOT IN (2, 3)")
    List<ReservationRecord> findValidReservations(
            @Param("resourceId") Long resourceId,
            @Param("date") LocalDate date
    );

    /**
     * 批量更新过期预约状态
     */
    @Modifying
    @Query("UPDATE ReservationRecord r SET r.reservationStatus = 4, r.updateTime = :updateTime " +
           "WHERE r.reservationStatus = 0 AND CONCAT(r.reservationDate, ' ', r.endTime) < :currentTime")
    int updateExpiredReservations(
            @Param("currentTime") LocalDateTime currentTime,
            @Param("updateTime") LocalDateTime updateTime
    );

    /**
     * 检查资源是否存在未完成的预约
     */
    @Query("SELECT COUNT(r) > 0 FROM ReservationRecord r WHERE r.resourceId = :resourceId " +
           "AND r.reservationStatus = 0")
    boolean existsPendingReservations(@Param("resourceId") Long resourceId);

    /**
     * 检查模板是否存在未使用的用户预约记录（operateType=1且reservationStatus=0）
     */
    @Query("SELECT COUNT(r) > 0 FROM ReservationRecord r WHERE r.templateId = :templateId " +
           "AND r.operateType = 1 AND r.reservationStatus = 0")
    boolean existsUnusedUserReservations(@Param("templateId") Long templateId);

    /**
     * 批量更新模板的线下占用和资源停用记录状态为3（员工取消）
     */
    @Modifying
    @Query("UPDATE ReservationRecord r SET r.reservationStatus = 3, r.updateTime = :updateTime " +
           "WHERE r.templateId = :templateId AND r.operateType IN (2, 3) AND r.reservationStatus = 0")
    int cancelOfflineReservations(
            @Param("templateId") Long templateId,
            @Param("updateTime") LocalDateTime updateTime
    );

    /**
     * 查询不一致预约记录（未使用的用户预约且那一天不能被预约）
     */
    @Query("SELECT r FROM ReservationRecord r WHERE r.templateId = :templateId " +
           "AND r.operateType = 1 AND r.reservationStatus = 0 " +
           "AND r.reservationDate BETWEEN :startDate AND :endDate " +
           "ORDER BY r.reservationDate, r.startTime")
    List<ReservationRecord> findInconsistentReservations(
            @Param("templateId") Long templateId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    /**
     * 查询某店铺某日的所有有效预约记录
     */
    @Query("SELECT r FROM ReservationRecord r " +
           "JOIN ReservationResource res ON r.resourceId = res.resourceId " +
           "WHERE res.storeId = :storeId AND r.reservationDate = :date " +
           "AND r.reservationStatus NOT IN (2, 3)")
    List<ReservationRecord> findStoreReservationsByDate(
            @Param("storeId") byte[] storeId,
            @Param("date") LocalDate date
    );

    /**
     * 查询用户的预约列表（支持分页和筛选）
     */
    @Query("SELECT r FROM ReservationRecord r WHERE r.userId = :userId " +
           "AND (:status IS NULL OR r.reservationStatus = :status) " +
           "AND (:startDate IS NULL OR r.reservationDate >= :startDate) " +
           "AND (:endDate IS NULL OR r.reservationDate <= :endDate) " +
           "ORDER BY r.reservationDate DESC, r.startTime DESC")
    Page<ReservationRecord> findUserReservations(
            @Param("userId") byte[] userId,
            @Param("status") Integer status,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            Pageable pageable
    );

    /**
     * 检查时间段是否冲突
     */
    @Query("SELECT COUNT(r) > 0 FROM ReservationRecord r " +
           "WHERE r.resourceId = :resourceId AND r.reservationDate = :date " +
           "AND r.reservationStatus NOT IN (2, 3) " +
           "AND ((r.startTime < :endTime AND r.endTime > :startTime))")
    boolean existsTimeConflict(
            @Param("resourceId") Long resourceId,
            @Param("date") LocalDate date,
            @Param("startTime") LocalTime startTime,
            @Param("endTime") LocalTime endTime
    );

    /**
     * 根据预约ID和用户ID查询预约记录
     */
    @Query("SELECT r FROM ReservationRecord r WHERE r.reservationId = :reservationId " +
           "AND r.userId = :userId")
    Optional<ReservationRecord> findByIdAndUserId(
            @Param("reservationId") Long reservationId,
            @Param("userId") byte[] userId
    );

    /**
     * 根据手机号查询会员的预约列表（支持分页和筛选）
     * 接口14使用，只返回指定店铺的预约记录
     */
    @Query("SELECT r FROM ReservationRecord r " +
           "JOIN ReservationResource res ON r.resourceId = res.resourceId " +
           "WHERE r.userPhone = :userPhone " +
           "AND res.storeId = :storeId " +
           "AND (:status IS NULL OR r.reservationStatus = :status) " +
           "AND (:startDate IS NULL OR r.reservationDate >= :startDate) " +
           "AND (:endDate IS NULL OR r.reservationDate <= :endDate) " +
           "ORDER BY r.reservationDate DESC, r.startTime DESC")
    Page<ReservationRecord> findMemberReservations(
            @Param("userPhone") String userPhone,
            @Param("storeId") byte[] storeId,
            @Param("status") Integer status,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            Pageable pageable
    );
}
