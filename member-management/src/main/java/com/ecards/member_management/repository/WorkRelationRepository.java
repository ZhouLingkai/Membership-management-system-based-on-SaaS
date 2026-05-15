package com.ecards.member_management.repository;

import com.ecards.member_management.entity.WorkRelation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 工作关系Repository接口
 * 提供工作关系数据访问方法
 */
@Repository
public interface WorkRelationRepository extends JpaRepository<WorkRelation, Long> {

    /**
     * 根据店铺ID和用户ID查询工作关系
     *
     * @param storeId 店铺ID
     * @param userId  用户ID
     * @return 工作关系信息
     */
    Optional<WorkRelation> findByStoreIdAndUserId(byte[] storeId, byte[] userId);

    /**
     * 根据店铺ID和用户ID和状态查询工作关系
     *
     * @param storeId 店铺ID
     * @param userId  用户ID
     * @param status  状态（1-在职）
     * @return 工作关系信息
     */
    Optional<WorkRelation> findByStoreIdAndUserIdAndStatus(byte[] storeId, byte[] userId, Integer status);

    /**
     * 根据用户ID查询所有工作关系
     *
     * @param userId 用户ID
     * @return 工作关系列表
     */
    List<WorkRelation> findByUserId(byte[] userId);

    /**
     * 根据用户ID和状态查询工作关系
     *
     * @param userId 用户ID
     * @param status 状态（1-在职）
     * @return 工作关系列表
     */
    List<WorkRelation> findByUserIdAndStatus(byte[] userId, Integer status);

    /**
     * 根据店铺ID查询所有员工
     *
     * @param storeId 店铺ID
     * @return 工作关系列表
     */
    List<WorkRelation> findByStoreId(byte[] storeId);

    /**
     * 根据店铺ID和状态查询员工
     *
     * @param storeId 店铺ID
     * @param status  状态（1-在职）
     * @return 工作关系列表
     */
    List<WorkRelation> findByStoreIdAndStatus(byte[] storeId, Integer status);

    /**
     * 根据店铺ID和角色查询员工
     *
     * @param storeId 店铺ID
     * @param role    角色
     * @param status  状态（1-在职）
     * @return 工作关系列表
     */
    List<WorkRelation> findByStoreIdAndRoleAndStatus(byte[] storeId, String role, Integer status);

    /**
     * 检查用户是否在指定店铺工作
     *
     * @param storeId 店铺ID
     * @param userId  用户ID
     * @param status  状态（1-在职）
     * @return true-在职，false-不在职
     */
    boolean existsByStoreIdAndUserIdAndStatus(byte[] storeId, byte[] userId, Integer status);

    /**
     * 统计店铺的在职员工数量
     *
     * @param storeId 店铺ID
     * @param status  状态（1-在职）
     * @return 员工数量
     */
    Long countByStoreIdAndStatus(byte[] storeId, Integer status);

    /**
     * 根据店铺ID和状态分页查询员工
     *
     * @param storeId  店铺ID
     * @param status   状态（1-在职）
     * @param pageable 分页参数
     * @return 员工分页列表
     */
    Page<WorkRelation> findByStoreIdAndStatus(byte[] storeId, Integer status, Pageable pageable);

    /**
     * 根据店铺ID、角色和状态分页查询员工
     *
     * @param storeId  店铺ID
     * @param role     角色
     * @param status   状态（1-在职）
     * @param pageable 分页参数
     * @return 员工分页列表
     */
    Page<WorkRelation> findByStoreIdAndRoleAndStatus(byte[] storeId, String role, Integer status, Pageable pageable);
}

