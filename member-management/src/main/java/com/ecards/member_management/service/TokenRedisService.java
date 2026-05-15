package com.ecards.member_management.service;

import com.ecards.member_management.common.ErrorCode;
import com.ecards.member_management.constants.TokenConstants;
import com.ecards.member_management.entity.User;
import com.ecards.member_management.entity.WorkRelation;
import com.ecards.member_management.exception.BusinessException;
import com.ecards.member_management.repository.UserRepository;
import com.ecards.member_management.repository.WorkRelationRepository;
import com.ecards.member_management.utils.EncryptUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * 令牌Redis服务
 * 管理令牌在Redis中的存储，包括黑名单、自动登录令牌、管理令牌使用次数
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TokenRedisService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final UserRepository userRepository;
    private final WorkRelationRepository workRelationRepository;
    private final EncryptUtils encryptUtils;

    private static final String LOGIN_FAIL_PREFIX = "login:fail:";
    private static final String LOGIN_LOCK_PREFIX = "login:lock:";
    private static final int MAX_LOGIN_ATTEMPTS = 5;
    private static final int LOCK_TIME_SECONDS = 300; // 5分钟

    // ==================== 登录防爆破管理 ====================

    /**
     * 检查是否被锁定
     * @param phone 手机号
     */
    public void checkLoginLock(String phone) {
        String lockKey = LOGIN_LOCK_PREFIX + phone;
        Long expire = redisTemplate.getExpire(lockKey, TimeUnit.SECONDS);
        if (expire != null && expire > 0) {
            throw new BusinessException(ErrorCode.PASSWORD_ERROR, 
                String.format("保护中，请%d秒后重试", expire));
        }
    }

    /**
     * 记录登录失败
     * @param phone 手机号
     * @return 剩余次数
     */
    public int recordLoginFailure(String phone) {
        String failKey = LOGIN_FAIL_PREFIX + phone;
        Long count = redisTemplate.opsForValue().increment(failKey);
        
        // 第一次失败设置过期时间（比如5分钟内连续输错）
        if (count != null && count == 1) {
            redisTemplate.expire(failKey, LOCK_TIME_SECONDS, TimeUnit.SECONDS);
        }

        if (count != null && count >= MAX_LOGIN_ATTEMPTS) {
            // 达到最大尝试次数，锁定
            String lockKey = LOGIN_LOCK_PREFIX + phone;
            redisTemplate.opsForValue().set(lockKey, "locked", LOCK_TIME_SECONDS, TimeUnit.SECONDS);
            redisTemplate.delete(failKey); // 可选：锁定后清除计数
            throw new BusinessException(ErrorCode.PASSWORD_ERROR, 
                String.format("保护中，请%d秒后重试", LOCK_TIME_SECONDS));
        }

        return MAX_LOGIN_ATTEMPTS - (count != null ? count.intValue() : 0);
    }

    /**
     * 清除登录失败记录
     * @param phone 手机号
     */
    public void clearLoginFailure(String phone) {
        String failKey = LOGIN_FAIL_PREFIX + phone;
        String lockKey = LOGIN_LOCK_PREFIX + phone;
        redisTemplate.delete(Set.of(failKey, lockKey));
    }

    // ==================== 黑名单管理 ====================

    /**
     * 将令牌JTI加入黑名单
     *
     * @param jti           令牌唯一ID
     * @param expireSeconds 过期时间（秒）
     */
    public void addToBlacklist(String jti, Long expireSeconds) {
        try {
            String key = TokenConstants.BLACKLIST_PREFIX + jti;
            redisTemplate.opsForValue().set(key, "1", expireSeconds, TimeUnit.SECONDS);
            log.info("令牌已加入黑名单: jti={}, expireSeconds={}", jti, expireSeconds);
        } catch (Exception e) {
            log.error("加入黑名单失败: jti={}", jti, e);
            throw new RuntimeException("加入黑名单失败", e);
        }
    }

    /**
     * 检查令牌JTI是否在黑名单中
     *
     * @param jti 令牌唯一ID
     * @return true-在黑名单中，false-不在黑名单中
     */
    public boolean isInBlacklist(String jti) {
        try {
            String key = TokenConstants.BLACKLIST_PREFIX + jti;
            return Boolean.TRUE.equals(redisTemplate.hasKey(key));
        } catch (Exception e) {
            log.error("检查黑名单失败: jti={}", jti, e);
            // 出错时保守处理，认为在黑名单中
            return true;
        }
    }

    /**
     * 从黑名单中移除令牌JTI（一般不需要，自动过期即可）
     *
     * @param jti 令牌唯一ID
     */
    public void removeFromBlacklist(String jti) {
        try {
            String key = TokenConstants.BLACKLIST_PREFIX + jti;
            redisTemplate.delete(key);
            log.info("令牌已从黑名单移除: jti={}", jti);
        } catch (Exception e) {
            log.error("从黑名单移除失败: jti={}", jti, e);
        }
    }

    // ==================== 自动登录令牌管理 ====================

    /**
     * 保存自动登录令牌
     *
     * @param userId        用户ID
     * @param platform      平台类型（MINI_PROGRAM/WEB）
     * @param deviceId      设备ID
     * @param jti           令牌唯一ID
     * @param expireSeconds 过期时间（秒）
     */
    public void saveAutoLoginToken(String userId, String platform, String deviceId, String jti, Long expireSeconds) {
        try {
            String key = buildAutoLoginKey(userId, platform, deviceId);
            redisTemplate.opsForValue().set(key, jti, expireSeconds, TimeUnit.SECONDS);
            log.info("自动登录令牌已保存: userId={}, platform={}, deviceId={}", userId, platform, deviceId);
        } catch (Exception e) {
            log.error("保存自动登录令牌失败: userId={}, platform={}, deviceId={}", userId, platform, deviceId, e);
            throw new RuntimeException("保存自动登录令牌失败", e);
        }
    }

    /**
     * 获取自动登录令牌JTI
     *
     * @param userId   用户ID
     * @param platform 平台类型
     * @param deviceId 设备ID
     * @return 令牌JTI，不存在返回null
     */
    public String getAutoLoginJti(String userId, String platform, String deviceId) {
        try {
            String key = buildAutoLoginKey(userId, platform, deviceId);
            Object jti = redisTemplate.opsForValue().get(key);
            return jti != null ? jti.toString() : null;
        } catch (Exception e) {
            log.error("获取自动登录令牌失败: userId={}, platform={}, deviceId={}", userId, platform, deviceId, e);
            return null;
        }
    }

    /**
     * 移除自动登录令牌
     *
     * @param userId   用户ID
     * @param platform 平台类型
     * @param deviceId 设备ID
     */
    public void removeAutoLoginToken(String userId, String platform, String deviceId) {
        try {
            String key = buildAutoLoginKey(userId, platform, deviceId);
            redisTemplate.delete(key);
            log.info("自动登录令牌已移除: userId={}, platform={}, deviceId={}", userId, platform, deviceId);
        } catch (Exception e) {
            log.error("移除自动登录令牌失败: userId={}, platform={}, deviceId={}", userId, platform, deviceId, e);
        }
    }

    /**
     * 检查自动登录令牌是否存在
     *
     * @param userId   用户ID
     * @param platform 平台类型
     * @param deviceId 设备ID
     * @return true-存在，false-不存在
     */
    public boolean hasAutoLoginToken(String userId, String platform, String deviceId) {
        try {
            String key = buildAutoLoginKey(userId, platform, deviceId);
            return Boolean.TRUE.equals(redisTemplate.hasKey(key));
        } catch (Exception e) {
            log.error("检查自动登录令牌失败: userId={}, platform={}, deviceId={}", userId, platform, deviceId, e);
            return false;
        }
    }

    /**
     * 构建自动登录Key
     *
     * @param userId   用户ID
     * @param platform 平台类型
     * @param deviceId 设备ID
     * @return Redis Key
     */
    private String buildAutoLoginKey(String userId, String platform, String deviceId) {
        return TokenConstants.AUTO_LOGIN_PREFIX + userId + ":" + platform + ":" + deviceId;
    }

    // ==================== 自动登录令牌使用次数管理 ====================

    /**
     * 构建自动登录令牌使用次数的Redis Key
     *
     * @param userId   用户ID
     * @param platform 平台类型
     * @param deviceId 设备ID
     * @return Redis Key
     */
    private String buildAutoLoginUsageKey(String userId, String platform, String deviceId) {
        return "auto:login:usage:" + userId + ":" + platform + ":" + deviceId;
    }

    /**
     * 递增自动登录令牌使用次数
     * 用于实现"3次使用+令牌轮换"安全机制
     *
     * @param userId        用户ID
     * @param platform      平台类型
     * @param deviceId      设备ID
     * @param expireSeconds 过期时间（秒，与令牌同步）
     * @return 当前使用次数
     */
    public Long incrementAutoLoginUsage(String userId, String platform, String deviceId, Long expireSeconds) {
        try {
            String key = buildAutoLoginUsageKey(userId, platform, deviceId);
            Long count = redisTemplate.opsForValue().increment(key);
            
            if (count != null && count == 1) {
                // 首次使用，设置过期时间（与令牌同步）
                redisTemplate.expire(key, expireSeconds, TimeUnit.SECONDS);
            }
            
            log.info("自动登录令牌使用次数递增: userId={}, platform={}, deviceId={}, count={}", 
                    userId, platform, deviceId, count);
            return count;
        } catch (Exception e) {
            log.error("递增自动登录令牌使用次数失败: userId={}, platform={}, deviceId={}", 
                    userId, platform, deviceId, e);
            // 失败时返回1，允许继续使用（保证服务可用性）
            return 1L;
        }
    }

    /**
     * 获取自动登录令牌使用次数
     *
     * @param userId   用户ID
     * @param platform 平台类型
     * @param deviceId 设备ID
     * @return 使用次数，不存在返回0
     */
    public Long getAutoLoginUsage(String userId, String platform, String deviceId) {
        try {
            String key = buildAutoLoginUsageKey(userId, platform, deviceId);
            Object count = redisTemplate.opsForValue().get(key);
            return count != null ? Long.parseLong(count.toString()) : 0L;
        } catch (Exception e) {
            log.error("获取自动登录令牌使用次数失败: userId={}, platform={}, deviceId={}", 
                    userId, platform, deviceId, e);
            return 0L;
        }
    }

    /**
     * 重置自动登录令牌使用次数
     * 在令牌轮换时调用，为新令牌重置计数器
     *
     * @param userId   用户ID
     * @param platform 平台类型
     * @param deviceId 设备ID
     */
    public void resetAutoLoginUsage(String userId, String platform, String deviceId) {
        try {
            String key = buildAutoLoginUsageKey(userId, platform, deviceId);
            redisTemplate.delete(key);
            log.info("自动登录令牌使用次数已重置: userId={}, platform={}, deviceId={}", 
                    userId, platform, deviceId);
        } catch (Exception e) {
            log.error("重置自动登录令牌使用次数失败: userId={}, platform={}, deviceId={}", 
                    userId, platform, deviceId, e);
        }
    }

    // ==================== 管理令牌使用次数管理 ====================

    /**
     * 增加管理令牌使用次数
     *
     * @param jti 令牌唯一ID
     * @return 当前使用次数
     */
    public Long incrementManagerTokenUsage(String jti) {
        try {
            String key = TokenConstants.MANAGER_TOKEN_USAGE_PREFIX + jti;
            Long count = redisTemplate.opsForValue().increment(key);
            
            // 设置过期时间为5分钟（与管理令牌过期时间一致）
            if (count != null && count == 1) {
                redisTemplate.expire(key, 300, TimeUnit.SECONDS);
            }
            
            log.info("管理令牌使用次数+1: jti={}, count={}", jti, count);
            return count;
        } catch (Exception e) {
            log.error("增加管理令牌使用次数失败: jti={}", jti, e);
            throw new RuntimeException("增加管理令牌使用次数失败", e);
        }
    }

    /**
     * 获取管理令牌使用次数
     *
     * @param jti 令牌唯一ID
     * @return 使用次数，不存在返回0
     */
    public Long getManagerTokenUsage(String jti) {
        try {
            String key = TokenConstants.MANAGER_TOKEN_USAGE_PREFIX + jti;
            Object count = redisTemplate.opsForValue().get(key);
            return count != null ? Long.parseLong(count.toString()) : 0L;
        } catch (Exception e) {
            log.error("获取管理令牌使用次数失败: jti={}", jti, e);
            return 0L;
        }
    }

    /**
     * 检查管理令牌是否超过最大使用次数
     *
     * @param jti      令牌唯一ID
     * @param maxCount 最大使用次数
     * @return true-已超限，false-未超限
     */
    public boolean isManagerTokenExceeded(String jti, Integer maxCount) {
        Long currentCount = getManagerTokenUsage(jti);
        boolean exceeded = currentCount >= maxCount;
        
        if (exceeded) {
            log.warn("管理令牌使用次数已超限: jti={}, currentCount={}, maxCount={}", jti, currentCount, maxCount);
        }
        
        return exceeded;
    }

    /**
     * 重置管理令牌使用次数（一般不需要）
     *
     * @param jti 令牌唯一ID
     */
    public void resetManagerTokenUsage(String jti) {
        try {
            String key = TokenConstants.MANAGER_TOKEN_USAGE_PREFIX + jti;
            redisTemplate.delete(key);
            log.info("管理令牌使用次数已重置: jti={}", jti);
        } catch (Exception e) {
            log.error("重置管理令牌使用次数失败: jti={}", jti, e);
        }
    }

    // ==================== 通用方法 ====================

    /**
     * 清除所有令牌相关缓存（慎用，仅用于测试或特殊情况）
     *
     * @param userId 用户ID
     */
    public void clearAllTokenCache(String userId) {
        try {
            // 这里可以根据需要实现清除指定用户的所有令牌缓存
            // 由于Redis的key模式匹配性能较差，暂时保留接口，后续根据需要实现
            log.warn("清除令牌缓存: userId={}", userId);
        } catch (Exception e) {
            log.error("清除令牌缓存失败: userId={}", userId, e);
        }
    }

    /**
     * 删除自动登录令牌
     *
     * @param key 自动登录令牌的Redis key
     */
    public void deleteAutoLoginToken(String key) {
        try {
            redisTemplate.delete(key);
            log.info("已删除自动登录令牌: key={}", key);
        } catch (Exception e) {
            log.error("删除自动登录令牌失败: key={}", key, e);
        }
    }

    /**
     * 批量删除自动登录令牌（通过模式匹配）
     *
     * @param pattern Redis key 模式
     */
    public void deleteAutoLoginTokensByPattern(String pattern) {
        try {
            Set<String> keys = redisTemplate.keys(pattern);
            if (keys != null && !keys.isEmpty()) {
                redisTemplate.delete(keys);
                log.info("已批量删除自动登录令牌: pattern={}, count={}", pattern, keys.size());
            }
        } catch (Exception e) {
            log.error("批量删除自动登录令牌失败: pattern={}", pattern, e);
        }
    }

    // ==================== 令牌版本号管理 ====================

    /**
     * 检查令牌版本号是否有效
     * 使用Redis缓存用户的token_version，避免每次都查数据库
     *
     * @param userId       用户ID
     * @param tokenVersion 令牌中的版本号
     * @return true-有效，false-无效
     */
    public boolean isTokenVersionValid(String userId, Integer tokenVersion) {
        try {
            // 令牌版本号为null，视为旧令牌，拒绝访问
            if (tokenVersion == null) {
                log.warn("令牌版本号为null，拒绝访问: userId={}", userId);
                return false;
            }

            // 从Redis获取用户当前的token_version（缓存5分钟）
            String key = "user:token:version:" + userId;
            Integer currentVersion = (Integer) redisTemplate.opsForValue().get(key);

            if (currentVersion == null) {
                // 缓存未命中，查询数据库
                User user = userRepository.findById(encryptUtils.uuidToBytes(userId))
                        .orElse(null);

                if (user == null) {
                    log.warn("用户不存在: userId={}", userId);
                    return false;
                }

                currentVersion = user.getTokenVersion();

                // 缓存到Redis（5分钟）
                redisTemplate.opsForValue().set(key, currentVersion, 5, TimeUnit.MINUTES);
            }

            // 令牌版本号必须等于当前版本号
            boolean valid = tokenVersion.equals(currentVersion);
            if (!valid) {
                log.warn("令牌版本号过期: userId={}, tokenVersion={}, currentVersion={}",
                        userId, tokenVersion, currentVersion);
            }

            return valid;

        } catch (Exception e) {
            log.error("检查令牌版本号失败: userId={}, tokenVersion={}", userId, tokenVersion, e);
            // 出错时拒绝访问（安全优先）
            return false;
        }
    }

    /**
     * 更新用户令牌版本号（递增）并更新Redis缓存
     *
     * @param userId 用户ID
     * @return 新的版本号
     */
    public Integer incrementTokenVersion(String userId) {
        try {
            User user = userRepository.findById(encryptUtils.uuidToBytes(userId))
                    .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_EXIST));

            // 递增版本号
            Integer newVersion = user.getTokenVersion() + 1;
            user.setTokenVersion(newVersion);
            userRepository.save(user);

            // 更新Redis缓存
            String key = "user:token:version:" + userId;
            redisTemplate.opsForValue().set(key, newVersion, 5, TimeUnit.MINUTES);

            log.info("已更新用户令牌版本号: userId={}, newVersion={}", userId, newVersion);

            return newVersion;

        } catch (Exception e) {
            log.error("更新用户令牌版本号失败: userId={}", userId, e);
            throw e;
        }
    }

    /**
     * 清除用户令牌版本号缓存
     * 用于登录时清除缓存，确保从数据库读取最新版本号
     *
     * @param userId 用户ID
     */
    public void clearTokenVersionCache(String userId) {
        try {
            String key = "user:token:version:" + userId;
            redisTemplate.delete(key);
            log.info("已清除用户令牌版本号缓存: userId={}", userId);
        } catch (Exception e) {
            log.error("清除用户令牌版本号缓存失败: userId={}", userId, e);
            throw e;
        }
    }

    /**
     * 更新Redis中的令牌版本号缓存
     *
     * @param userId       用户ID
     * @param tokenVersion 版本号
     */
    public void updateTokenVersionCache(String userId, Integer tokenVersion) {
        try {
            String key = "user:token:version:" + userId;
            redisTemplate.opsForValue().set(key, tokenVersion, 5, TimeUnit.MINUTES);
            log.debug("已更新令牌版本号缓存: userId={}, version={}", userId, tokenVersion);
        } catch (Exception e) {
            log.error("更新令牌版本号缓存失败: userId={}, version={}", userId, tokenVersion, e);
        }
    }

    // ==================== 工作令牌版本号管理 ====================

    /**
     * 验证工作令牌版本号是否有效
     * 工作令牌的版本号存储在 work_relation 表中，与 user 表的 token_version 独立
     *
     * @param userId       用户ID
     * @param storeId      店铺ID
     * @param tokenVersion 令牌中的版本号
     * @return 是否有效
     */
    public boolean isWorkTokenVersionValid(String userId, String storeId, Integer tokenVersion) {
        try {
            // 令牌版本号为null，视为旧令牌，拒绝访问
            if (tokenVersion == null) {
                log.warn("工作令牌版本号为null，拒绝访问: userId={}, storeId={}", userId, storeId);
                return false;
            }

            // 从Redis获取工作关系的token_version（缓存5分钟）
            String key = "work:token:version:" + userId + ":" + storeId;
            Integer currentVersion = (Integer) redisTemplate.opsForValue().get(key);

            if (currentVersion == null) {
                // 缓存未命中，查询数据库
                byte[] userIdBytes = encryptUtils.uuidToBytes(userId);
                byte[] storeIdBytes = encryptUtils.uuidToBytes(storeId);

                WorkRelation workRelation = workRelationRepository
                        .findByStoreIdAndUserIdAndStatus(storeIdBytes, userIdBytes, 1)
                        .orElse(null);

                if (workRelation == null) {
                    log.warn("工作关系不存在或已解绑: userId={}, storeId={}", userId, storeId);
                    return false;
                }

                currentVersion = workRelation.getTokenVersion();

                // 缓存到Redis（5分钟）
                redisTemplate.opsForValue().set(key, currentVersion, 5, TimeUnit.MINUTES);
            }

            // 令牌版本号必须等于当前版本号
            boolean valid = tokenVersion.equals(currentVersion);
            if (!valid) {
                log.warn("工作令牌版本号不匹配: userId={}, storeId={}, tokenVersion={}, currentVersion={}",
                        userId, storeId, tokenVersion, currentVersion);
            }

            return valid;
        } catch (Exception e) {
            log.error("验证工作令牌版本号失败: userId={}, storeId={}", userId, storeId, e);
            return false;
        }
    }

    /**
     * 清除工作令牌版本缓存
     * 当工作关系的tokenVersion更新时（角色调整、权限修改等），需要清除缓存
     *
     * @param userId  用户ID
     * @param storeId 店铺ID
     */
    public void clearWorkTokenVersionCache(String userId, String storeId) {
        try {
            String key = "work:token:version:" + userId + ":" + storeId;
            redisTemplate.delete(key);
            log.info("已清除工作令牌版本缓存: userId={}, storeId={}", userId, storeId);
        } catch (Exception e) {
            log.error("清除工作令牌版本缓存失败: userId={}, storeId={}", userId, storeId, e);
        }
    }
}

