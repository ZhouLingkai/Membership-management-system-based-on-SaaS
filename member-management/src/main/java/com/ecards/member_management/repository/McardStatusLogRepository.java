package com.ecards.member_management.repository;

import com.ecards.member_management.entity.McardStatusLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 会员卡状态变更记录Repository
 */
@Repository
public interface McardStatusLogRepository extends JpaRepository<McardStatusLog, Long> {

    /**
     * 根据会员卡ID查询状态变更记录
     */
    List<McardStatusLog> findByMemberCardId(byte[] memberCardId);

    /**
     * 根据会员卡ID分页查询状态变更记录
     */
    Page<McardStatusLog> findByMemberCardId(byte[] memberCardId, Pageable pageable);

    /**
     * 根据会员卡ID和变更类型查询记录
     */
    List<McardStatusLog> findByMemberCardIdAndChangeType(byte[] memberCardId, Integer changeType);

    /**
     * 根据操作人ID查询记录
     */
    List<McardStatusLog> findByOperatorId(byte[] operatorId);

    /**
     * 根据变更类型查询记录
     */
    List<McardStatusLog> findByChangeType(Integer changeType);
}

