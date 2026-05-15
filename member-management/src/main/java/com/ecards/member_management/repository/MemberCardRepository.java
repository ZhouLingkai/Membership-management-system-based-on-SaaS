package com.ecards.member_management.repository;

import com.ecards.member_management.entity.MemberCard;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 会员卡Repository
 */
@Repository
public interface MemberCardRepository extends JpaRepository<MemberCard, byte[]>, JpaSpecificationExecutor<MemberCard> {

    /**
     * 统计商家会员卡总数（用于会员数量限制校验）
     * 注意：使用COUNT(*)，不去重
     */
    long countByMerchantId(byte[] merchantId);

    /**
     * 检查重复办卡（手机号 + 卡种）
     */
    boolean existsByCardTypeIdAndMemberPhone(Long cardTypeId, String memberPhone);

    /**
     * 检查重复办卡（用户ID + 卡种）
     */
    boolean existsByCardTypeIdAndUserId(Long cardTypeId, byte[] userId);

    /**
     * 根据商家ID分页查询会员卡列表
     */
    Page<MemberCard> findByMerchantId(byte[] merchantId, Pageable pageable);

    /**
     * 根据店铺ID分页查询会员卡列表（本店卡）
     */
    Page<MemberCard> findByStoreId(byte[] storeId, Pageable pageable);

    /**
     * 根据会员卡ID查询
     */
    Optional<MemberCard> findByMemberCardId(byte[] memberCardId);

    /**
     * 根据用户ID查询会员卡列表
     */
    List<MemberCard> findByUserId(byte[] userId);

    /**
     * 根据手机号查询会员卡列表
     */
    List<MemberCard> findByMemberPhone(String memberPhone);

    /**
     * 根据店铺ID和手机号查询会员卡列表
     */
    List<MemberCard> findByStoreIdAndMemberPhone(byte[] storeId, String memberPhone);

    /**
     * 查询跨店卡（同商家不同店铺 + 支持跨店）
     * 用于接口4的跨店卡查询
     */
    @Query("SELECT mc FROM MemberCard mc " +
            "JOIN mc.cardType ct " +
            "WHERE mc.merchantId = :merchantId " +
            "AND mc.storeId != :storeId " +
            "AND ct.crossStore = 1")
    Page<MemberCard> findCrossStoreCards(
            @Param("merchantId") byte[] merchantId,
            @Param("storeId") byte[] storeId,
            Pageable pageable);

    /**
     * 商家会员卡列表多条件查询（接口3使用）
     */
    @Query("SELECT mc FROM MemberCard mc " +
            "WHERE mc.merchantId = :merchantId " +
            "AND (:storeId IS NULL OR mc.storeId = :storeId) " +
            "AND (:cardTypeId IS NULL OR mc.cardTypeId = :cardTypeId) " +
            "AND (:cardTtype IS NULL OR mc.cardTtype = :cardTtype) " +
            "AND (:status IS NULL OR mc.status = :status) " +
            "AND (:memberPhone IS NULL OR mc.memberPhone LIKE %:memberPhone%) " +
            "AND (:memberName IS NULL OR mc.memberName LIKE %:memberName%) " +
            "AND (:startTime IS NULL OR mc.openCardTime >= :startTime) " +
            "AND (:endTime IS NULL OR mc.openCardTime <= :endTime)")
    Page<MemberCard> findByMerchantConditions(
            @Param("merchantId") byte[] merchantId,
            @Param("storeId") byte[] storeId,
            @Param("cardTypeId") Long cardTypeId,
            @Param("cardTtype") Integer cardTtype,
            @Param("status") Integer status,
            @Param("memberPhone") String memberPhone,
            @Param("memberName") String memberName,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime,
            Pageable pageable);

    /**
     * 店铺本店卡多条件查询（接口4的local模式使用）
     */
    @Query("SELECT mc FROM MemberCard mc " +
            "WHERE mc.storeId = :storeId " +
            "AND (:cardTypeId IS NULL OR mc.cardTypeId = :cardTypeId) " +
            "AND (:cardTtype IS NULL OR mc.cardTtype = :cardTtype) " +
            "AND (:status IS NULL OR mc.status = :status) " +
            "AND (:memberPhone IS NULL OR mc.memberPhone LIKE %:memberPhone%) " +
            "AND (:memberName IS NULL OR mc.memberName LIKE %:memberName%) " +
            "AND (:startTime IS NULL OR mc.openCardTime >= :startTime) " +
            "AND (:endTime IS NULL OR mc.openCardTime <= :endTime)")
    Page<MemberCard> findByStoreConditions(
            @Param("storeId") byte[] storeId,
            @Param("cardTypeId") Long cardTypeId,
            @Param("cardTtype") Integer cardTtype,
            @Param("status") Integer status,
            @Param("memberPhone") String memberPhone,
            @Param("memberName") String memberName,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime,
            Pageable pageable);

    /**
     * 店铺跨店卡多条件查询（接口4的cross_store模式使用）
     */
    @Query("SELECT mc FROM MemberCard mc " +
            "JOIN mc.cardType ct " +
            "WHERE mc.merchantId = :merchantId " +
            "AND mc.storeId != :storeId " +
            "AND ct.crossStore = 1 " +
            "AND (:cardTypeId IS NULL OR mc.cardTypeId = :cardTypeId) " +
            "AND (:cardTtype IS NULL OR mc.cardTtype = :cardTtype) " +
            "AND (:status IS NULL OR mc.status = :status) " +
            "AND (:memberPhone IS NULL OR mc.memberPhone LIKE %:memberPhone%) " +
            "AND (:memberName IS NULL OR mc.memberName LIKE %:memberName%) " +
            "AND (:startTime IS NULL OR mc.openCardTime >= :startTime) " +
            "AND (:endTime IS NULL OR mc.openCardTime <= :endTime)")
    Page<MemberCard> findByCrossStoreConditions(
            @Param("merchantId") byte[] merchantId,
            @Param("storeId") byte[] storeId,
            @Param("cardTypeId") Long cardTypeId,
            @Param("cardTtype") Integer cardTtype,
            @Param("status") Integer status,
            @Param("memberPhone") String memberPhone,
            @Param("memberName") String memberName,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime,
            Pageable pageable);

    // ========== 接口5：商家会员统计 ==========

    /**
     * 统计商家会员总数（去重）
     */
    @Query("SELECT COUNT(DISTINCT mc.userId) FROM MemberCard mc WHERE mc.merchantId = :merchantId AND mc.userId IS NOT NULL")
    Integer countDistinctMembersByMerchantId(@Param("merchantId") byte[] merchantId);

    /**
     * 统计商家会员卡总数
     */
    @Query("SELECT COUNT(mc) FROM MemberCard mc WHERE mc.merchantId = :merchantId")
    Integer countTotalCardsByMerchantId(@Param("merchantId") byte[] merchantId);

    /**
     * 统计商家指定状态的会员卡数量
     */
    @Query("SELECT COUNT(mc) FROM MemberCard mc WHERE mc.merchantId = :merchantId AND mc.status = :status")
    Integer countCardsByMerchantIdAndStatus(@Param("merchantId") byte[] merchantId, @Param("status") Integer status);

    /**
     * 统计商家已激活会员卡数量（status != 0）
     */
    @Query("SELECT COUNT(mc) FROM MemberCard mc WHERE mc.merchantId = :merchantId AND mc.status != 0")
    Integer countActivatedCardsByMerchantId(@Param("merchantId") byte[] merchantId);

    /**
     * 统计商家未激活会员卡数量（status = 0）
     */
    @Query("SELECT COUNT(mc) FROM MemberCard mc WHERE mc.merchantId = :merchantId AND mc.status = 0")
    Integer countUnactivatedCardsByMerchantId(@Param("merchantId") byte[] merchantId);

    // ========== 接口6：店铺会员统计 ==========

    /**
     * 统计本店会员总数（去重）
     */
    @Query("SELECT COUNT(DISTINCT mc.userId) FROM MemberCard mc WHERE mc.storeId = :storeId AND mc.userId IS NOT NULL")
    Integer countDistinctMembersByStoreId(@Param("storeId") byte[] storeId);

    /**
     * 统计本店会员卡总数
     */
    @Query("SELECT COUNT(mc) FROM MemberCard mc WHERE mc.storeId = :storeId")
    Integer countTotalCardsByStoreId(@Param("storeId") byte[] storeId);

    /**
     * 统计跨店卡总数
     */
    @Query("SELECT COUNT(mc) FROM MemberCard mc " +
           "JOIN mc.cardType ct " +
           "WHERE mc.merchantId = :merchantId " +
           "AND mc.storeId != :storeId " +
           "AND ct.crossStore = 1")
    Integer countCrossStoreCards(@Param("merchantId") byte[] merchantId, @Param("storeId") byte[] storeId);

    // ========== 接口7：用户查询自己的会员卡列表 ==========

    /**
     * 查询用户自己的会员卡列表（支持多条件筛选）
     */
    @Query("SELECT mc FROM MemberCard mc " +
           "WHERE mc.userId = :userId " +
           "AND (:status IS NULL OR mc.status = :status) " +
           "AND (:storeId IS NULL OR mc.storeId = :storeId) " +
           "AND (:merchantId IS NULL OR mc.merchantId = :merchantId) " +
           "AND (:cardTtype IS NULL OR mc.cardTtype = :cardTtype) " +
           "ORDER BY mc.openCardTime DESC")
    Page<MemberCard> findMyCards(
        @Param("userId") byte[] userId,
        @Param("status") Integer status,
        @Param("storeId") byte[] storeId,
        @Param("merchantId") byte[] merchantId,
        @Param("cardTtype") Integer cardTtype,
        Pageable pageable
    );

    // ========== 接口8：通过手机号查询会员卡 ==========

    /**
     * 查询本店卡（通过手机号）
     */
    @Query("SELECT mc FROM MemberCard mc " +
           "WHERE mc.memberPhone = :memberPhone " +
           "AND mc.storeId = :storeId " +
           "ORDER BY mc.openCardTime DESC")
    List<MemberCard> findLocalCardsByPhone(
        @Param("memberPhone") String memberPhone,
        @Param("storeId") byte[] storeId
    );

    /**
     * 查询跨店卡（通过手机号），限制500条
     */
    @Query("SELECT mc FROM MemberCard mc " +
           "JOIN mc.cardType ct " +
           "WHERE mc.memberPhone = :memberPhone " +
           "AND mc.merchantId = :merchantId " +
           "AND mc.storeId != :storeId " +
           "AND ct.crossStore = 1 " +
           "ORDER BY mc.openCardTime DESC")
    List<MemberCard> findCrossStoreCardsByPhone(
        @Param("memberPhone") String memberPhone,
        @Param("merchantId") byte[] merchantId,
        @Param("storeId") byte[] storeId,
        Pageable pageable
    );

    // ========== 接口11：批量激活会员卡 ==========

    /**
     * 根据手机号和状态查询会员卡列表
     */
    List<MemberCard> findByMemberPhoneAndStatus(String memberPhone, Integer status);

    // ========== 接口15：线下扫码查询个人会员卡 ==========

    /**
     * 根据用户ID和店铺ID查询本店卡
     */
    @Query("SELECT mc FROM MemberCard mc WHERE mc.userId = :userId AND mc.storeId = :storeId ORDER BY mc.openCardTime DESC")
    List<MemberCard> findByUserIdAndStoreId(@Param("userId") byte[] userId, @Param("storeId") byte[] storeId);

    /**
     * 根据用户ID查询跨店卡（其他店铺办理的跨店卡）
     */
    @Query("SELECT mc FROM MemberCard mc " +
           "JOIN mc.cardType ct " +
           "WHERE mc.userId = :userId " +
           "AND mc.merchantId = :merchantId " +
           "AND mc.storeId != :storeId " +
           "AND ct.crossStore = 1 " +
           "ORDER BY mc.openCardTime DESC")
    List<MemberCard> findCrossStoreCardsByUserId(
        @Param("userId") byte[] userId,
        @Param("merchantId") byte[] merchantId,
        @Param("storeId") byte[] storeId,
        Pageable pageable
    );

    // ========== 交易管理模块：悲观锁查询 ==========

    /**
     * 使用悲观写锁查询会员卡（用于充值/消费/时效调整等需要并发控制的操作）
     * 防止并发修改导致余额/次数/时效计算错误
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT mc FROM MemberCard mc WHERE mc.memberCardId = :memberCardId")
    Optional<MemberCard> findByIdForUpdate(@Param("memberCardId") byte[] memberCardId);

    // ========== 预约系统优化：JOIN查询 ==========

    /**
     * 查询会员卡及其卡类型信息（优化版）
     * 使用INNER JOIN一次性获取会员卡和卡类型信息，减少数据库查询次数
     * 
     * @param memberCardId 会员卡ID
     * @return 会员卡实体（包含cardType关联）
     */
    @Query("SELECT mc FROM MemberCard mc " +
           "JOIN FETCH mc.cardType " +
           "WHERE mc.memberCardId = :memberCardId")
    Optional<MemberCard> findByMemberCardIdWithType(@Param("memberCardId") byte[] memberCardId);
}

