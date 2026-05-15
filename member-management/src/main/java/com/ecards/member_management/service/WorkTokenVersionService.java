package com.ecards.member_management.service;

import com.ecards.member_management.entity.WorkRelation;
import com.ecards.member_management.exception.BusinessException;
import com.ecards.member_management.repository.WorkRelationRepository;
import com.ecards.member_management.utils.EncryptUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 工作令牌版本号管理服务
 * 
 * 功能说明：
 * 1. 当员工角色调整时，递增token_version
 * 2. 当员工权限修改时，递增token_version
 * 3. 版本号递增后，旧工作令牌失效，员工需重新申请工作令牌
 * 
 * 设计原理：
 * - 工作令牌中包含token_version字段
 * - 每次API请求时，会校验令牌中的版本号与数据库是否一致
 * - 版本号不一致时，返回错误码10007，提示用户重新申请令牌
 * 
 * @author Ecards Team
 * @since 2025-10-30
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WorkTokenVersionService {

    private final WorkRelationRepository workRelationRepository;
    private final EncryptUtils encryptUtils;

    /**
     * 递增工作令牌版本号（员工角色调整时调用）
     * 
     * @param storeId 店铺ID（UUID字符串）
     * @param userId  员工用户ID（UUID字符串）
     * @param oldRole 调整前角色
     * @param newRole 调整后角色
     * @return 新的版本号
     */
    @Transactional
    public Integer incrementVersionForRoleChange(String storeId, String userId, 
                                                  String oldRole, String newRole) {
        try {
            byte[] storeIdBytes = encryptUtils.uuidToBytes(storeId);
            byte[] userIdBytes = encryptUtils.uuidToBytes(userId);

            // 查询工作关系
            WorkRelation workRelation = workRelationRepository
                    .findByStoreIdAndUserIdAndStatus(storeIdBytes, userIdBytes, 1)
                    .orElseThrow(() -> new BusinessException(50002, "员工未关联该店铺"));

            // 递增版本号
            Integer oldVersion = workRelation.getTokenVersion();
            Integer newVersion = oldVersion + 1;
            workRelation.setTokenVersion(newVersion);

            // 保存
            workRelationRepository.save(workRelation);

            log.info("员工角色调整，工作令牌版本号递增: storeId={}, userId={}, oldRole={}, newRole={}, oldVersion={}, newVersion={}",
                    storeId, userId, oldRole, newRole, oldVersion, newVersion);

            return newVersion;
            
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("递增工作令牌版本号失败（角色调整）: storeId={}, userId={}", storeId, userId, e);
            throw new RuntimeException("递增工作令牌版本号失败", e);
        }
    }

    /**
     * 递增工作令牌版本号（员工权限修改时调用）
     * 
     * @param storeId       店铺ID（UUID字符串）
     * @param userId        员工用户ID（UUID字符串）
     * @param oldPermission 修改前权限JSON
     * @param newPermission 修改后权限JSON
     * @return 新的版本号
     */
    @Transactional
    public Integer incrementVersionForPermissionChange(String storeId, String userId,
                                                       String oldPermission, String newPermission) {
        try {
            byte[] storeIdBytes = encryptUtils.uuidToBytes(storeId);
            byte[] userIdBytes = encryptUtils.uuidToBytes(userId);

            // 查询工作关系
            WorkRelation workRelation = workRelationRepository
                    .findByStoreIdAndUserIdAndStatus(storeIdBytes, userIdBytes, 1)
                    .orElseThrow(() -> new BusinessException(50002, "员工未关联该店铺"));

            // 递增版本号
            Integer oldVersion = workRelation.getTokenVersion();
            Integer newVersion = oldVersion + 1;
            workRelation.setTokenVersion(newVersion);

            // 保存
            workRelationRepository.save(workRelation);

            log.info("员工权限修改，工作令牌版本号递增: storeId={}, userId={}, oldVersion={}, newVersion={}",
                    storeId, userId, oldVersion, newVersion);
            log.debug("权限变更详情: oldPermission={}, newPermission={}", oldPermission, newPermission);

            return newVersion;
            
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("递增工作令牌版本号失败（权限修改）: storeId={}, userId={}", storeId, userId, e);
            throw new RuntimeException("递增工作令牌版本号失败", e);
        }
    }

    /**
     * 递增工作令牌版本号（通用方法）
     * 
     * @param storeId 店铺ID（UUID字符串）
     * @param userId  员工用户ID（UUID字符串）
     * @param reason  递增原因（用于日志记录）
     * @return 新的版本号
     */
    @Transactional
    public Integer incrementVersion(String storeId, String userId, String reason) {
        try {
            byte[] storeIdBytes = encryptUtils.uuidToBytes(storeId);
            byte[] userIdBytes = encryptUtils.uuidToBytes(userId);

            // 查询工作关系
            WorkRelation workRelation = workRelationRepository
                    .findByStoreIdAndUserIdAndStatus(storeIdBytes, userIdBytes, 1)
                    .orElseThrow(() -> new BusinessException(50002, "员工未关联该店铺"));

            // 递增版本号
            Integer oldVersion = workRelation.getTokenVersion();
            Integer newVersion = oldVersion + 1;
            workRelation.setTokenVersion(newVersion);

            // 保存
            workRelationRepository.save(workRelation);

            log.info("工作令牌版本号递增: storeId={}, userId={}, reason={}, oldVersion={}, newVersion={}",
                    storeId, userId, reason, oldVersion, newVersion);

            return newVersion;
            
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("递增工作令牌版本号失败: storeId={}, userId={}, reason={}", storeId, userId, reason, e);
            throw new RuntimeException("递增工作令牌版本号失败", e);
        }
    }

    /**
     * 查询当前工作令牌版本号
     * 
     * @param storeId 店铺ID（UUID字符串）
     * @param userId  员工用户ID（UUID字符串）
     * @return 当前版本号
     */
    public Integer getCurrentVersion(String storeId, String userId) {
        try {
            byte[] storeIdBytes = encryptUtils.uuidToBytes(storeId);
            byte[] userIdBytes = encryptUtils.uuidToBytes(userId);

            WorkRelation workRelation = workRelationRepository
                    .findByStoreIdAndUserIdAndStatus(storeIdBytes, userIdBytes, 1)
                    .orElseThrow(() -> new BusinessException(50002, "员工未关联该店铺"));

            return workRelation.getTokenVersion();
            
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("查询工作令牌版本号失败: storeId={}, userId={}", storeId, userId, e);
            throw new RuntimeException("查询工作令牌版本号失败", e);
        }
    }

    /**
     * 校验工作令牌版本号是否一致
     * 
     * @param storeId      店铺ID（UUID字符串）
     * @param userId       员工用户ID（UUID字符串）
     * @param tokenVersion 令牌中的版本号
     * @return true-版本号一致，false-版本号不一致（令牌已过期）
     */
    public boolean validateTokenVersion(String storeId, String userId, Integer tokenVersion) {
        try {
            Integer currentVersion = getCurrentVersion(storeId, userId);
            
            boolean isValid = currentVersion.equals(tokenVersion);
            
            if (!isValid) {
                log.warn("工作令牌版本号不一致: storeId={}, userId={}, tokenVersion={}, currentVersion={}",
                        storeId, userId, tokenVersion, currentVersion);
            }
            
            return isValid;
            
        } catch (Exception e) {
            log.error("校验工作令牌版本号失败: storeId={}, userId={}, tokenVersion={}", 
                    storeId, userId, tokenVersion, e);
            return false;
        }
    }
}

