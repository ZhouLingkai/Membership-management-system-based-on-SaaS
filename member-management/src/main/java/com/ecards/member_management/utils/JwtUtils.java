package com.ecards.member_management.utils;

import com.ecards.member_management.constants.TokenConstants;
import com.ecards.member_management.enums.TokenType;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * JWT工具类
 * 提供JWT令牌的生成、解析、验证功能
 */
@Slf4j
@Component
public class JwtUtils {

    /**
     * JWT签名密钥（从配置文件注入）
     */
    @Value("${jwt.secret-key}")
    private String secretKey;

    /**
     * 获取签名密钥
     */
    private SecretKey getSigningKey() {
        byte[] keyBytes = secretKey.getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    /**
     * 生成普通令牌
     *
     * @param userId       用户ID（UUID字符串）
     * @param role         用户角色
     * @param merchantId   商家ID（可为null）
     * @param deviceId     设备ID
     * @param loginIp      登录IP
     * @param tokenVersion 令牌版本号
     * @param jti          令牌唯一ID
     * @param expiration   过期时间（毫秒）
     * @return JWT令牌
     */
    public String generateNormalToken(String userId, String role, String merchantId,
                                      String deviceId, String loginIp, Integer tokenVersion,
                                      String jti, Long expiration) {
        Map<String, Object> claims = new HashMap<>();
        claims.put(TokenConstants.CLAIM_USER_ID, userId);
        claims.put(TokenConstants.CLAIM_ROLE, role);
        claims.put(TokenConstants.CLAIM_MERCHANT_ID, merchantId);
        claims.put(TokenConstants.CLAIM_DEVICE_ID, deviceId);
        claims.put(TokenConstants.CLAIM_LOGIN_IP, loginIp);
        claims.put(TokenConstants.CLAIM_TOKEN_TYPE, TokenType.NORMAL.getCode());
        claims.put("token_version", tokenVersion);

        return buildToken(claims, jti, expiration);
    }

    /**
     * 生成特权令牌
     *
     * @param userId       用户ID（UUID字符串）
     * @param role         用户角色
     * @param permissions  权限列表
     * @param deviceId     设备ID
     * @param tokenVersion 令牌版本号
     * @param jti          令牌唯一ID
     * @param expiration   过期时间（毫秒）
     * @return JWT令牌
     */
    public String generatePrivilegeToken(String userId, String role, List<String> permissions,
                                         String deviceId, Integer tokenVersion, String jti, Long expiration) {
        Map<String, Object> claims = new HashMap<>();
        claims.put(TokenConstants.CLAIM_USER_ID, userId);
        claims.put(TokenConstants.CLAIM_ROLE, role);
        claims.put(TokenConstants.CLAIM_PERMISSIONS, permissions);
        claims.put(TokenConstants.CLAIM_DEVICE_ID, deviceId);
        claims.put(TokenConstants.CLAIM_SINGLE_USE, true);
        claims.put(TokenConstants.CLAIM_TOKEN_TYPE, TokenType.PRIVILEGE.getCode());
        claims.put("token_version", tokenVersion);

        return buildToken(claims, jti, expiration);
    }

    /**
     * 生成工作令牌
     *
     * @param userId       用户ID（UUID字符串）
     * @param role         用户角色
     * @param merchantId   商家ID
     * @param storeId      店铺ID
     * @param permissions  权限列表
     * @param deviceId     设备ID
     * @param loginIp      登录IP
     * @param tokenVersion 令牌版本号
     * @param jti          令牌唯一ID
     * @param expiration   过期时间（毫秒）
     * @return JWT令牌
     */
    public String generateWorkToken(String userId, String role, String merchantId, String storeId,
                                    List<String> permissions, String deviceId, String loginIp,
                                    Integer tokenVersion, String jti, Long expiration) {
        Map<String, Object> claims = new HashMap<>();
        claims.put(TokenConstants.CLAIM_USER_ID, userId);
        claims.put(TokenConstants.CLAIM_ROLE, role);
        claims.put(TokenConstants.CLAIM_MERCHANT_ID, merchantId);
        claims.put(TokenConstants.CLAIM_STORE_ID, storeId);
        claims.put(TokenConstants.CLAIM_PERMISSIONS, permissions);
        claims.put(TokenConstants.CLAIM_DEVICE_ID, deviceId);
        claims.put(TokenConstants.CLAIM_LOGIN_IP, loginIp);
        claims.put(TokenConstants.CLAIM_TOKEN_TYPE, TokenType.WORK.getCode());
        claims.put("token_version", tokenVersion);

        return buildToken(claims, jti, expiration);
    }

    /**
     * 生成管理令牌
     *
     * @param userId       用户ID（UUID字符串）
     * @param role         用户角色
     * @param merchantId   商家ID
     * @param storeId      店铺ID（可为null）
     * @param deviceId     设备ID
     * @param loginIp      登录IP
     * @param tokenVersion 令牌版本号
     * @param jti          令牌唯一ID
     * @param expiration   过期时间（毫秒）
     * @return JWT令牌
     */
    public String generateManagerToken(String userId, String role, String merchantId, String storeId,
                                       String deviceId, String loginIp, Integer tokenVersion,
                                       String jti, Long expiration) {
        Map<String, Object> claims = new HashMap<>();
        claims.put(TokenConstants.CLAIM_USER_ID, userId);
        claims.put(TokenConstants.CLAIM_ROLE, role);
        claims.put(TokenConstants.CLAIM_MERCHANT_ID, merchantId);
        claims.put(TokenConstants.CLAIM_STORE_ID, storeId);
        claims.put(TokenConstants.CLAIM_DEVICE_ID, deviceId);
        claims.put(TokenConstants.CLAIM_LOGIN_IP, loginIp);
        claims.put(TokenConstants.CLAIM_TOKEN_TYPE, TokenType.MANAGER.getCode());
        claims.put("token_version", tokenVersion);

        return buildToken(claims, jti, expiration);
    }

    /**
     * 生成自动登录令牌
     *
     * @param userId       用户ID（UUID字符串）
     * @param platform     平台类型（MINI_PROGRAM/WEB）
     * @param deviceId     设备ID
     * @param loginIp      登录IP
     * @param tokenVersion 令牌版本号
     * @param jti          令牌唯一ID
     * @param expiration   过期时间（毫秒）
     * @return JWT令牌
     */
    public String generateAutoLoginToken(String userId, String platform, String deviceId,
                                         String loginIp, Integer tokenVersion, String jti, Long expiration) {
        Map<String, Object> claims = new HashMap<>();
        claims.put(TokenConstants.CLAIM_USER_ID, userId);
        claims.put(TokenConstants.CLAIM_PLATFORM, platform);
        claims.put(TokenConstants.CLAIM_DEVICE_ID, deviceId);
        claims.put(TokenConstants.CLAIM_LOGIN_IP, loginIp);
        claims.put(TokenConstants.CLAIM_LAST_RENEW_TIME, System.currentTimeMillis());
        claims.put(TokenConstants.CLAIM_ALLOW_RENEW, true);
        claims.put(TokenConstants.CLAIM_RENEW_TIMES, 0);
        claims.put(TokenConstants.CLAIM_TOKEN_TYPE, TokenType.AUTO_LOGIN.getCode());
        claims.put("token_version", tokenVersion);

        return buildToken(claims, jti, expiration);
    }

    /**
     * 构建JWT令牌
     *
     * @param claims     声明
     * @param jti        令牌唯一ID
     * @param expiration 过期时间（毫秒）
     * @return JWT令牌
     */
    private String buildToken(Map<String, Object> claims, String jti, Long expiration) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expiration);

        return Jwts.builder()
                .setClaims(claims)
                .setId(jti)
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * 解析JWT令牌
     *
     * @param token JWT令牌
     * @return Claims对象
     * @throws JwtException 解析失败或令牌无效
     */
    public Claims parseToken(String token) {
        try {
            // 移除Bearer前缀
            if (token.startsWith(TokenConstants.BEARER_PREFIX)) {
                token = token.substring(TokenConstants.BEARER_PREFIX.length());
            }
            
            // 去除前后空格，防止因空格导致解析失败
            token = token.trim();

            return Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (ExpiredJwtException e) {
            log.warn("JWT令牌已过期: {}", e.getMessage());
            throw e;
        } catch (UnsupportedJwtException e) {
            log.error("不支持的JWT令牌: {}", e.getMessage());
            throw e;
        } catch (MalformedJwtException e) {
            log.error("JWT令牌格式错误: {}", e.getMessage());
            throw e;
        } catch (SignatureException e) {
            log.error("JWT签名验证失败: {}", e.getMessage());
            throw e;
        } catch (IllegalArgumentException e) {
            log.error("JWT令牌为空: {}", e.getMessage());
            throw e;
        }
    }

    /**
     * 验证JWT令牌是否有效
     *
     * @param token JWT令牌
     * @return true-有效，false-无效
     */
    public boolean validateToken(String token) {
        try {
            parseToken(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    /**
     * 从令牌中提取用户ID
     *
     * @param token JWT令牌
     * @return 用户ID
     */
    public String extractUserId(String token) {
        Claims claims = parseToken(token);
        return claims.get(TokenConstants.CLAIM_USER_ID, String.class);
    }

    /**
     * 从令牌中提取角色
     *
     * @param token JWT令牌
     * @return 用户角色
     */
    public String extractRole(String token) {
        Claims claims = parseToken(token);
        return claims.get(TokenConstants.CLAIM_ROLE, String.class);
    }

    /**
     * 从令牌中提取JTI
     *
     * @param token JWT令牌
     * @return JTI
     */
    public String extractJti(String token) {
        Claims claims = parseToken(token);
        return claims.getId();
    }

    /**
     * 从令牌中提取设备ID
     *
     * @param token JWT令牌
     * @return 设备ID
     */
    public String extractDeviceId(String token) {
        Claims claims = parseToken(token);
        return claims.get(TokenConstants.CLAIM_DEVICE_ID, String.class);
    }

    /**
     * 从令牌中提取商家ID
     *
     * @param token JWT令牌
     * @return 商家ID（可能为null）
     */
    public String extractMerchantId(String token) {
        Claims claims = parseToken(token);
        return claims.get(TokenConstants.CLAIM_MERCHANT_ID, String.class);
    }

    /**
     * 从令牌中提取店铺ID
     *
     * @param token JWT令牌
     * @return 店铺ID（可能为null）
     */
    public String extractStoreId(String token) {
        Claims claims = parseToken(token);
        return claims.get(TokenConstants.CLAIM_STORE_ID, String.class);
    }

    /**
     * 从令牌中提取权限列表
     *
     * @param token JWT令牌
     * @return 权限列表（可能为null）
     */
    @SuppressWarnings("unchecked")
    public List<String> extractPermissions(String token) {
        Claims claims = parseToken(token);
        return claims.get(TokenConstants.CLAIM_PERMISSIONS, List.class);
    }

    /**
     * 从令牌中提取令牌类型
     *
     * @param token JWT令牌
     * @return 令牌类型代码
     */
    public Integer extractTokenType(String token) {
        Claims claims = parseToken(token);
        return claims.get(TokenConstants.CLAIM_TOKEN_TYPE, Integer.class);
    }

    /**
     * 判断令牌是否已过期
     *
     * @param token JWT令牌
     * @return true-已过期，false-未过期
     */
    public boolean isTokenExpired(String token) {
        try {
            Claims claims = parseToken(token);
            return claims.getExpiration().before(new Date());
        } catch (ExpiredJwtException e) {
            return true;
        }
    }

    /**
     * 获取令牌剩余有效时间（毫秒）
     *
     * @param token JWT令牌
     * @return 剩余有效时间（毫秒），如果已过期返回0
     */
    public Long getRemainingTime(String token) {
        try {
            Claims claims = parseToken(token);
            Date expiration = claims.getExpiration();
            long remaining = expiration.getTime() - System.currentTimeMillis();
            return remaining > 0 ? remaining : 0;
        } catch (ExpiredJwtException e) {
            return 0L;
        }
    }

    /**
     * 获取令牌过期时间
     *
     * @param token JWT令牌
     * @return 过期时间
     */
    public Date getExpiration(String token) {
        Claims claims = parseToken(token);
        return claims.getExpiration();
    }

    /**
     * 检查令牌是否为单次使用令牌
     *
     * @param token JWT令牌
     * @return true-单次使用，false-可多次使用
     */
    public boolean isSingleUse(String token) {
        Claims claims = parseToken(token);
        Boolean singleUse = claims.get(TokenConstants.CLAIM_SINGLE_USE, Boolean.class);
        return singleUse != null && singleUse;
    }

    /**
     * 移除Bearer前缀
     *
     * @param token 带Bearer前缀的令牌
     * @return 纯令牌字符串
     */
    public String removeBearerPrefix(String token) {
        if (token != null && token.startsWith(TokenConstants.BEARER_PREFIX)) {
            return token.substring(TokenConstants.BEARER_PREFIX.length());
        }
        return token;
    }
}

