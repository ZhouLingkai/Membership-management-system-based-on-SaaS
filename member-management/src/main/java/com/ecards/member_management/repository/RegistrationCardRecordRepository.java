package com.ecards.member_management.repository;

import com.ecards.member_management.entity.RegistrationCardRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 办卡记录Repository
 * 
 * @author Ecards Team
 * @since 2025-11-03
 */
@Repository
public interface RegistrationCardRecordRepository extends JpaRepository<RegistrationCardRecord, Long> {

    /**
     * 根据会员卡ID查询办卡记录
     *
     * @param memberCardId 会员卡ID
     * @return 办卡记录列表
     */
    List<RegistrationCardRecord> findByMemberCardId(byte[] memberCardId);

    /**
     * 根据操作员ID查询办卡记录
     *
     * @param operatorId 操作员ID
     * @return 办卡记录列表
     */
    List<RegistrationCardRecord> findByOperatorId(byte[] operatorId);

    /**
     * 根据店铺ID查询办卡记录
     *
     * @param transStoreId 店铺ID
     * @return 办卡记录列表
     */
    List<RegistrationCardRecord> findByTransStoreId(byte[] transStoreId);
}

