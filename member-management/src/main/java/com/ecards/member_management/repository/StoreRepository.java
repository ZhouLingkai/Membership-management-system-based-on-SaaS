package com.ecards.member_management.repository;

import com.ecards.member_management.entity.Store;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 店铺Repository接口
 * 提供店铺数据访问方法
 */
@Repository
public interface StoreRepository extends JpaRepository<Store, byte[]> {

    /**
     * 根据商家ID查询所有店铺
     *
     * @param merchantId 商家ID
     * @return 店铺列表
     */
    List<Store> findByMerchantId(byte[] merchantId);

    /**
     * 根据商家ID和状态查询店铺
     *
     * @param merchantId 商家ID
     * @param status     店铺状态
     * @return 店铺列表
     */
    List<Store> findByMerchantIdAndStatus(byte[] merchantId, Integer status);

    /**
     * 根据店铺名称模糊查询
     *
     * @param storeName 店铺名称
     * @return 店铺列表
     */
    List<Store> findByStoreNameContaining(String storeName);

    /**
     * 根据商家ID查询正常营业的店铺数量
     *
     * @param merchantId 商家ID
     * @param status     店铺状态（1-正常营业）
     * @return 店铺数量
     */
    Long countByMerchantIdAndStatus(byte[] merchantId, Integer status);

    /**
     * 检查店铺是否属于指定商家
     *
     * @param storeId    店铺ID
     * @param merchantId 商家ID
     * @return true-属于，false-不属于
     */
    @Query("SELECT CASE WHEN COUNT(s) > 0 THEN true ELSE false END FROM Store s WHERE s.storeId = :storeId AND s.merchantId = :merchantId")
    boolean existsByStoreIdAndMerchantId(@Param("storeId") byte[] storeId, @Param("merchantId") byte[] merchantId);

    /**
     * 根据商家ID查询所有店铺（按创建时间降序）
     *
     * @param merchantId 商家ID
     * @return 店铺列表
     */
    List<Store> findByMerchantIdOrderByCreateTimeDesc(byte[] merchantId);

    /**
     * 根据商家ID统计所有状态的店铺数量
     *
     * @param merchantId 商家ID
     * @return 店铺总数
     */
    Long countByMerchantId(byte[] merchantId);
}

