package com.ecards.member_management.service;

import com.ecards.member_management.constants.AdminConstants;
import com.ecards.member_management.utils.EncryptUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * 管理员Token Redis缓存服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminTokenRedisService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final EncryptUtils encryptUtils;

    /**
     * 存储AdminToken的jti
     *
     * @param adminId   管理员ID（UUID字符串）
     * @param deviceId  设备ID
     * @param jti       Token唯一标识
     * @param expirationSeconds Token过期时间（秒）
     */
    public void storeTokenJti(String adminId, String deviceId, String jti, long expirationSeconds) {
        String key = AdminConstants.RedisKey.buildTokenKey(adminId, deviceId);
        redisTemplate.opsForValue().set(key, jti, expirationSeconds, TimeUnit.SECONDS);
    }

    /**
     * 验证Token的jti是否有效
     *
     * @param adminId  管理员ID
     * @param deviceId 设备ID
     * @param jti      Token唯一标识
     * @return boolean
     */
    public boolean validateTokenJti(String adminId, String deviceId, String jti) {
        String key = AdminConstants.RedisKey.buildTokenKey(adminId, deviceId);
        Object storedJti = redisTemplate.opsForValue().get(key);
        
        if (storedJti == null) {
            log.warn("Token不存在或已过期: key={}", key);
            return false;
        }
        
        return jti.equals(storedJti.toString());
    }

    /**
     * 删除Token（登出）
     *
     * @param adminId  管理员ID
     * @param deviceId 设备ID
     */
    public void deleteToken(String adminId, String deviceId) {
        String key = AdminConstants.RedisKey.buildTokenKey(adminId, deviceId);
        redisTemplate.delete(key);
        log.info("删除AdminToken: key={}", key);
    }

    /**
     * 存储Token版本号
     *
     * @param adminId 管理员ID
     * @param version 版本号
     */
    public void storeTokenVersion(String adminId, Integer version) {
        String key = AdminConstants.RedisKey.buildTokenVersionKey(adminId);
        redisTemplate.opsForValue().set(key, version);
    }

    /**
     * 获取Token版本号
     *
     * @param adminId 管理员ID
     * @return Integer 版本号，不存在则返回null
     */
    public Integer getTokenVersion(String adminId) {
        String key = AdminConstants.RedisKey.buildTokenVersionKey(adminId);
        Object version = redisTemplate.opsForValue().get(key);
        return version != null ? Integer.parseInt(version.toString()) : null;
    }

    /**
     * 验证Token版本是否有效
     *
     * @param adminId      管理员ID
     * @param tokenVersion Token中的版本号
     * @return boolean
     */
    public boolean isTokenVersionValid(String adminId, Integer tokenVersion) {
        Integer storedVersion = getTokenVersion(adminId);
        
        if (storedVersion == null) {
            log.warn("Token版本不存在: adminId={}", adminId);
            return false;
        }
        
        return tokenVersion.equals(storedVersion);
    }

    /**
     * 清除管理员的所有Token（用于密码修改后）
     *
     * @param adminId 管理员ID
     */
    public void clearAllTokens(String adminId) {
        // 删除所有该管理员的Token（通过模式匹配）
        String pattern = AdminConstants.RedisKey.ADMIN_TOKEN_PREFIX + adminId + ":*";
        var keys = redisTemplate.keys(pattern);
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
            log.info("清除管理员所有Token: adminId={}, count={}", adminId, keys.size());
        }
    }
}

