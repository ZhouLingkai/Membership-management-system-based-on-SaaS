package com.ecards.member_management.repository;

import com.ecards.member_management.entity.MerchantAuditRecord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 商户审核记录Repository接口
 * 提供商户审核记录数据访问方法
 */
@Repository
public interface MerchantAuditRecordRepository extends JpaRepository<MerchantAuditRecord, Long> {

    /**
     * 根据用户ID查询审核记录列表
     *
     * @param userId 用户ID
     * @return 审核记录列表
     */
    List<MerchantAuditRecord> findByUserIdOrderByCreateTimeDesc(byte[] userId);

    /**
     * 查询待审核的记录数量
     *
     * @param auditStatus 审核状态（0-待审核）
     * @return 待审核记录数量
     */
    Long countByAuditStatus(Integer auditStatus);

    /**
     * 根据审核状态查询记录列表
     *
     * @param auditStatus 审核状态
     * @return 审核记录列表
     */
    List<MerchantAuditRecord> findByAuditStatusOrderByCreateTimeDesc(Integer auditStatus);

    /**
     * 分页查询所有审核记录
     *
     * @param pageable 分页参数
     * @return 分页结果
     */
    Page<MerchantAuditRecord> findAll(Pageable pageable);

    /**
     * 分页查询指定状态的审核记录
     * 审核状态：0-待审核, 1-已通过, 2-已拒绝
     *
     * @param auditStatus 审核状态
     * @param pageable 分页参数
     * @return 分页结果
     */
    Page<MerchantAuditRecord> findByAuditStatus(Integer auditStatus, Pageable pageable);
}

