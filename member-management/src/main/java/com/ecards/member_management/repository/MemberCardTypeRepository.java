package com.ecards.member_management.repository;

import com.ecards.member_management.entity.MemberCardType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 会员卡种Repository
 */
@Repository
public interface MemberCardTypeRepository extends JpaRepository<MemberCardType, Long> {

    /**
     * 根据店铺ID和卡种名称查询卡种（用于唯一性校验）
     */
    Optional<MemberCardType> findByStoreIdAndCardTypeName(byte[] storeId, String cardTypeName);

    /**
     * 根据店铺ID和卡种名称查询卡种（排除指定ID，用于修改时的唯一性校验）
     */
    Optional<MemberCardType> findByStoreIdAndCardTypeNameAndCardTypeIdNot(
            byte[] storeId, String cardTypeName, Long cardTypeId);

    /**
     * 根据店铺ID分页查询卡种
     */
    Page<MemberCardType> findByStoreId(byte[] storeId, Pageable pageable);

    /**
     * 根据店铺ID和卡种类型分页查询
     */
    Page<MemberCardType> findByStoreIdAndCardTtype(byte[] storeId, Integer cardTtype, Pageable pageable);

    /**
     * 根据店铺ID和卡种名称模糊查询
     */
    Page<MemberCardType> findByStoreIdAndCardTypeNameContaining(byte[] storeId, String cardTypeName, Pageable pageable);

    /**
     * 根据店铺ID查询卡种数量
     */
    long countByStoreId(byte[] storeId);

    /**
     * 根据商家ID查询卡种数量
     */
    long countByMerchantId(byte[] merchantId);

    /**
     * 根据卡种ID和店铺ID查询（双重校验）
     */
    Optional<MemberCardType> findByCardTypeIdAndStoreId(Long cardTypeId, byte[] storeId);

    /**
     * 多条件组合查询（支持可选条件）
     */
    @Query("SELECT m FROM MemberCardType m WHERE m.storeId = :storeId " +
            "AND (:cardTtype IS NULL OR m.cardTtype = :cardTtype) " +
            "AND (:crossStore IS NULL OR m.crossStore = :crossStore) " +
            "AND (:cardTypeName IS NULL OR m.cardTypeName LIKE %:cardTypeName%)")
    Page<MemberCardType> findByConditions(
            @Param("storeId") byte[] storeId,
            @Param("cardTtype") Integer cardTtype,
            @Param("crossStore") Integer crossStore,
            @Param("cardTypeName") String cardTypeName,
            Pageable pageable);
}

