package com.ecards.member_management.repository;

import com.ecards.member_management.entity.AdminOperationLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

/**
 * 管理员操作日志数据访问层
 */
@Repository
public interface AdminOperationLogRepository extends JpaRepository<AdminOperationLog, Long> {

    /**
     * 查询指定管理员的操作日志（分页）
     *
     * @param adminId  管理员ID
     * @param pageable 分页参数
     * @return Page<AdminOperationLog>
     */
    Page<AdminOperationLog> findByAdminId(byte[] adminId, Pageable pageable);

    /**
     * 查询指定操作类型的日志（分页）
     *
     * @param operationType 操作类型
     * @param pageable      分页参数
     * @return Page<AdminOperationLog>
     */
    Page<AdminOperationLog> findByOperationType(String operationType, Pageable pageable);

    /**
     * 根据管理员账号查询日志（分页）
     *
     * @param adminAccount 管理员账号
     * @param pageable     分页参数
     * @return Page<AdminOperationLog>
     */
    Page<AdminOperationLog> findByAdminAccount(String adminAccount, Pageable pageable);

    /**
     * 查询指定时间范围内的日志（分页）
     *
     * @param startTime 开始时间
     * @param endTime   结束时间
     * @param pageable  分页参数
     * @return Page<AdminOperationLog>
     */
    Page<AdminOperationLog> findByOperationTimeBetween(
            LocalDateTime startTime,
            LocalDateTime endTime,
            Pageable pageable
    );

    /**
     * 删除指定时间之前的日志（用于定期清理）
     *
     * @param beforeTime 时间点
     * @return 删除的记录数
     */
    @Query("DELETE FROM AdminOperationLog WHERE operationTime < :beforeTime")
    int deleteByOperationTimeBefore(@Param("beforeTime") LocalDateTime beforeTime);

    /**
     * 复合条件查询日志（分页）
     *
     * @param adminId       管理员ID（可选）
     * @param operationType 操作类型（可选）
     * @param startTime     开始时间（可选）
     * @param endTime       结束时间（可选）
     * @param pageable      分页参数
     * @return Page<AdminOperationLog>
     */
    @Query("SELECT l FROM AdminOperationLog l WHERE " +
            "(:adminId IS NULL OR l.adminId = :adminId) AND " +
            "(:operationType IS NULL OR l.operationType = :operationType) AND " +
            "(:startTime IS NULL OR l.operationTime >= :startTime) AND " +
            "(:endTime IS NULL OR l.operationTime <= :endTime) " +
            "ORDER BY l.operationTime DESC")
    Page<AdminOperationLog> findByConditions(
            @Param("adminId") byte[] adminId,
            @Param("operationType") String operationType,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime,
            Pageable pageable
    );
}

