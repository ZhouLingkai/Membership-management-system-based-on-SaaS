package com.ecards.member_management.repository;

import com.ecards.member_management.entity.Admin;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 管理员表数据访问层
 */
@Repository
public interface AdminRepository extends JpaRepository<Admin, byte[]> {

    /**
     * 通过账号查询管理员
     *
     * @param account 账号
     * @return Optional<Admin>
     */
    Optional<Admin> findByAccount(String account);

    /**
     * 通过手机号查询管理员
     *
     * @param phone 手机号
     * @return Optional<Admin>
     */
    Optional<Admin> findByPhone(String phone);

    /**
     * 检查账号是否存在
     *
     * @param account 账号
     * @return boolean
     */
    boolean existsByAccount(String account);

    /**
     * 检查手机号是否存在
     *
     * @param phone 手机号
     * @return boolean
     */
    boolean existsByPhone(String phone);

    /**
     * 统计启用状态的管理员数量
     *
     * @param status 状态（1-启用）
     * @return long
     */
    long countByStatus(Integer status);
}

