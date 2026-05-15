package com.ecards.member_management.repository;

import com.ecards.member_management.entity.MerchantExtend;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * 商家扩展信息Repository接口
 * 提供商家数据访问方法
 */
@Repository
public interface MerchantExtendRepository extends JpaRepository<MerchantExtend, byte[]> {

    /**
     * 根据用户ID查询商家扩展信息
     *
     * @param userId 用户ID
     * @return 商家扩展信息
     */
    Optional<MerchantExtend> findByUserId(byte[] userId);

    /**
     * 检查用户是否已开通商家
     *
     * @param userId 用户ID
     * @return true-已开通，false-未开通
     */
    boolean existsByUserId(byte[] userId);

    /**
     * 根据商家等级查询商家数量
     *
     * @param merchantLevel 商家等级
     * @return 商家数量
     */
    Long countByMerchantLevel(Integer merchantLevel);

    /**
     * 查询特权即将过期的商家（用于提醒）
     *
     * @param expireTime 过期时间阈值
     * @return 商家列表
     */
    @Query("SELECT m FROM MerchantExtend m WHERE m.privilegeExpireTime <= :expireTime")
    java.util.List<MerchantExtend> findByPrivilegeExpireTimeBefore(@Param("expireTime") LocalDateTime expireTime);
}

