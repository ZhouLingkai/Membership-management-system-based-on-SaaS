package com.ecards.member_management.repository;

import com.ecards.member_management.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 用户Repository接口
 * 提供用户数据访问方法
 */
@Repository
public interface UserRepository extends JpaRepository<User, byte[]> {

    /**
     * 根据手机号查询用户
     *
     * @param phone 手机号（加密后）
     * @return 用户信息
     */
    Optional<User> findByPhone(String phone);

    /**
     * 根据邀请码查询用户
     *
     * @param inviteCode 邀请码
     * @return 用户信息
     */
    Optional<User> findByInviteCode(String inviteCode);

    /**
     * 根据用户类型查询用户数量
     *
     * @param userType 用户类型
     * @return 用户数量
     */
    Long countByUserType(Integer userType);

    /**
     * 检查手机号是否已存在
     *
     * @param phone 手机号（加密后）
     * @return true-存在，false-不存在
     */
    boolean existsByPhone(String phone);

    /**
     * 检查邀请码是否已存在
     *
     * @param inviteCode 邀请码
     * @return true-存在，false-不存在
     */
    boolean existsByInviteCode(String inviteCode);

    /**
     * 根据用户ID查询用户手机号
     *
     * @param userId 用户ID（BINARY(16)）
     * @return 用户手机号（明文）
     */
    @Query("SELECT u.phone FROM User u WHERE u.userId = :userId")
    Optional<String> findPhoneByUserId(@Param("userId") byte[] userId);
}

