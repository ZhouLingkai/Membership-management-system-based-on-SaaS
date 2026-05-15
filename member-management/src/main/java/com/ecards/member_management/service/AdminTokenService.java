package com.ecards.member_management.service;

import com.ecards.member_management.config.AdminProperties;
import com.ecards.member_management.constants.AdminConstants;
import com.ecards.member_management.entity.Admin;
import com.ecards.member_management.utils.EncryptUtils;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;

/**
 * 管理员Token服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminTokenService {

    private final AdminProperties adminProperties;
    private final AdminTokenRedisService adminTokenRedisService;
    private final EncryptUtils encryptUtils;

    /**
     * 生成AdminToken
     *
     * @param admin    管理员信息
     * @param deviceId 设备ID
     * @param loginIp  登录IP
     * @return JWT Token
     */
    public String generateAdminToken(Admin admin, String deviceId, String loginIp) {
        String adminId = encryptUtils.bytesToUuid(admin.getAdminId());
        String jti = UUID.randomUUID().toString();
        long expirationMillis = adminProperties.getJwt().getExpiration();
        Date now = new Date();
        Date expiration = new Date(now.getTime() + expirationMillis);

        // 构建JWT
        String token = Jwts.builder()
                .claim(AdminConstants.JwtClaims.ADMIN_ID, adminId)
                .claim(AdminConstants.JwtClaims.ADMIN_ROLE, admin.getAdminRole())
                .claim(AdminConstants.JwtClaims.ROLE_CODE, AdminConstants.Role.getRoleCode(admin.getAdminRole()))
                .claim(AdminConstants.JwtClaims.LOGIN_IP, loginIp)
                .claim(AdminConstants.JwtClaims.DEVICE_ID, deviceId)
                .claim(AdminConstants.JwtClaims.TOKEN_VERSION, admin.getTokenVersion())
                .setSubject(admin.getAccount())  // 设置账号为JWT的subject
                .setId(jti)
                .setIssuedAt(now)
                .setExpiration(expiration)
                .signWith(getSecretKey(), SignatureAlgorithm.HS256)
                .compact();

        // 存储到Redis
        long expirationSeconds = expirationMillis / 1000;
        adminTokenRedisService.storeTokenJti(adminId, deviceId, jti, expirationSeconds);
        adminTokenRedisService.storeTokenVersion(adminId, admin.getTokenVersion());

        log.info("生成AdminToken成功: adminId={}, role={}, deviceId={}", 
                adminId, admin.getAdminRole(), deviceId);
        
        return AdminConstants.Header.TOKEN_PREFIX + token;
    }

    /**
     * 验证并解析Token
     *
     * @param token Token字符串（带Bearer前缀）
     * @return Claims 或 null
     */
    public Claims validateAndParseToken(String token) {
        try {
            // 移除Bearer前缀
            if (token.startsWith(AdminConstants.Header.TOKEN_PREFIX)) {
                token = token.substring(AdminConstants.Header.TOKEN_PREFIX.length());
            }

            // 解析JWT
            Claims claims = Jwts.parser()
                    .verifyWith(getSecretKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            // 验证jti
            String adminId = claims.get(AdminConstants.JwtClaims.ADMIN_ID, String.class);
            String deviceId = claims.get(AdminConstants.JwtClaims.DEVICE_ID, String.class);
            String jti = claims.getId();
            
            if (!adminTokenRedisService.validateTokenJti(adminId, deviceId, jti)) {
                log.warn("Token jti验证失败: adminId={}, deviceId={}", adminId, deviceId);
                return null;
            }

            // 验证Token版本
            Integer tokenVersion = claims.get(AdminConstants.JwtClaims.TOKEN_VERSION, Integer.class);
            if (!adminTokenRedisService.isTokenVersionValid(adminId, tokenVersion)) {
                log.warn("Token版本验证失败: adminId={}, tokenVersion={}", adminId, tokenVersion);
                return null;
            }

            return claims;

        } catch (Exception e) {
            log.error("Token验证失败: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 从Token中提取管理员ID
     *
     * @param token Token字符串
     * @return 管理员ID（UUID字符串）
     */
    public String extractAdminId(String token) {
        Claims claims = validateAndParseToken(token);
        return claims != null ? claims.get(AdminConstants.JwtClaims.ADMIN_ID, String.class) : null;
    }

    /**
     * 从Token中提取管理员角色
     *
     * @param token Token字符串
     * @return 角色ID
     */
    public Integer extractAdminRole(String token) {
        Claims claims = validateAndParseToken(token);
        return claims != null ? claims.get(AdminConstants.JwtClaims.ADMIN_ROLE, Integer.class) : null;
    }

    /**
     * 获取JWT密钥
     *
     * @return SecretKey
     */
    private SecretKey getSecretKey() {
        String secret = adminProperties.getJwt().getSecret();
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}

