package com.ecards.member_management.controller;

import com.ecards.member_management.common.ErrorCode;
import com.ecards.member_management.common.Result;
import com.ecards.member_management.constants.TokenConstants;
import com.ecards.member_management.dto.request.*;
import com.ecards.member_management.dto.response.*;
import com.ecards.member_management.exception.BusinessException;
import com.ecards.member_management.service.TokenService;
import com.ecards.member_management.utils.EncryptUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 令牌控制器
 * 提供令牌相关的API接口
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/tokens")
@RequiredArgsConstructor
public class TokenController {

    private final TokenService tokenService;
    private final EncryptUtils encryptUtils;

    /**
     * 获取普通令牌
     * POST /api/v1/tokens/normal
     * 
     * 支持三种登录方式：
     * 1. 验证码登录（loginType=1）
     * 2. 密码登录（loginType=2）
     * 3. 自动登录（loginType=3）
     */
    @PostMapping("/normal")
    public Result<NormalTokenResponse> getNormalToken(
            @Valid @RequestBody NormalTokenRequest request,
            @RequestHeader(TokenConstants.DEVICE_ID_HEADER) String deviceId,
            HttpServletRequest httpRequest) {
        
        try {
            String loginIp = getClientIp(httpRequest);
            Map<String, Object> result;

            switch (request.getLoginType()) {
                case 1:
                    // 验证码登录 - 解密手机号
                    String plainPhoneForCode = encryptUtils.decryptAES(request.getPhone());
                    if (plainPhoneForCode == null || plainPhoneForCode.isEmpty()) {
                        throw new BusinessException(ErrorCode.PARAM_ERROR, "手机号格式错误");
                    }
                    result = tokenService.generateNormalTokenByCode(
                            plainPhoneForCode,
                            request.getVerifyCode(),
                            request.getPlatform(),
                            deviceId,
                            loginIp
                    );
                    break;
                case 2:
                    // 密码登录 - 解密手机号
                    String plainPhoneForPassword = encryptUtils.decryptAES(request.getPhone());
                    if (plainPhoneForPassword == null || plainPhoneForPassword.isEmpty()) {
                        throw new BusinessException(ErrorCode.PARAM_ERROR, "手机号格式错误");
                    }
                    result = tokenService.generateNormalTokenByPassword(
                            plainPhoneForPassword,
                            request.getPassword(),
                            request.getPlatform(),
                            deviceId,
                            loginIp
                    );
                    break;
                default:
                    return Result.fail("不支持的登录方式，请使用验证码登录(1)或密码登录(2)");
            }

            // 构建响应
            NormalTokenResponse response = NormalTokenResponse.builder()
                    .token((String) result.get("token"))
                    .expireTime((String) result.get("expireTime"))
                    .jti((String) result.get("jti"))
                    .userRole((String) result.get("userRole"))
                    .autoExpireTime((String) result.get("autoExpireTime"))
                    .build();

            return Result.success("获取普通令牌成功", response);
        } catch (Exception e) {
            log.error("获取普通令牌失败", e);
            return Result.fail("获取普通令牌失败：" + e.getMessage());
        }
    }

    /**
     * 获取特权令牌
     * POST /api/v1/tokens/privilege
     * 
     * 需要携带普通令牌（Authorization: Bearer xxx）
     */
    @PostMapping("/privilege")
    public Result<PrivilegeTokenResponse> getPrivilegeToken(
            @Valid @RequestBody PrivilegeTokenRequest request,
            @RequestHeader(TokenConstants.AUTHORIZATION_HEADER) String authorization,
            @RequestHeader(TokenConstants.DEVICE_ID_HEADER) String deviceId) {
        
        try {
            // 提取Bearer令牌
            String normalToken = extractBearerToken(authorization);
            if (normalToken == null) {
                return Result.fail("Authorization格式错误");
            }

            // 生成特权令牌
            Map<String, Object> result = tokenService.generatePrivilegeToken(
                    normalToken,
                    request.getTargetOperate(),
                    deviceId
            );

            // 构建响应
            PrivilegeTokenResponse response = PrivilegeTokenResponse.builder()
                    .token((String) result.get("token"))
                    .expireTime((String) result.get("expireTime"))
                    .jti((String) result.get("jti"))
                    .singleUse((Boolean) result.get("singleUse"))
                    .build();

            return Result.success("获取特权令牌成功", response);
        } catch (Exception e) {
            log.error("获取特权令牌失败", e);
            return Result.fail("获取特权令牌失败：" + e.getMessage());
        }
    }

    /**
     * 获取工作令牌
     * POST /api/v1/tokens/work
     * 
     * 需要携带普通令牌（Authorization: Bearer xxx）
     */
    @PostMapping("/work")
    public Result<WorkTokenResponse> getWorkToken(
            @Valid @RequestBody WorkTokenRequest request,
            @RequestHeader(TokenConstants.AUTHORIZATION_HEADER) String authorization,
            @RequestHeader(TokenConstants.DEVICE_ID_HEADER) String deviceId,
            HttpServletRequest httpRequest) {
        
        try {
            // 提取Bearer令牌
            String normalToken = extractBearerToken(authorization);
            if (normalToken == null) {
                return Result.fail("Authorization格式错误");
            }

            String loginIp = getClientIp(httpRequest);

            // 生成工作令牌（权限自动从数据库读取）
            Map<String, Object> result = tokenService.generateWorkToken(
                    normalToken,
                    request.getStoreId(),
                    deviceId,
                    loginIp
            );

            // 构建响应
            @SuppressWarnings("unchecked")
            java.util.List<String> permissionsList = (java.util.List<String>) result.get("permissions");
            
            WorkTokenResponse response = WorkTokenResponse.builder()
                    .token((String) result.get("token"))
                    .expireTime((String) result.get("expireTime"))
                    .jti((String) result.get("jti"))
                    .storeId((String) result.get("storeId"))
                    .merchantId((String) result.get("merchantId"))
                    .role((String) result.get("role"))
                    .permissions(permissionsList)
                    .tokenVersion((Integer) result.get("tokenVersion"))
                    .build();

            return Result.success("获取工作令牌成功", response);
        } catch (Exception e) {
            log.error("获取工作令牌失败", e);
            return Result.fail("获取工作令牌失败：" + e.getMessage());
        }
    }

    /**
     * 获取管理令牌
     * POST /api/v1/tokens/manager
     * 
     * 需要携带普通令牌（Authorization: Bearer xxx）
     */
    @PostMapping("/manager")
    public Result<ManagerTokenResponse> getManagerToken(
            @Valid @RequestBody ManagerTokenRequest request,
            @RequestHeader(TokenConstants.AUTHORIZATION_HEADER) String authorization,
            @RequestHeader(TokenConstants.DEVICE_ID_HEADER) String deviceId,
            HttpServletRequest httpRequest) {
        
        try {
            // 提取Bearer令牌
            String normalToken = extractBearerToken(authorization);
            if (normalToken == null) {
                return Result.fail("Authorization格式错误");
            }

            String loginIp = getClientIp(httpRequest);

            // 生成管理令牌
            Map<String, Object> result = tokenService.generateManagerToken(
                    normalToken,
                    request.getMerchantId(),
                    request.getSndPswd(),
                    request.getStoreId(),
                    deviceId,
                    loginIp
            );

            // 构建响应
            ManagerTokenResponse response = ManagerTokenResponse.builder()
                    .token((String) result.get("token"))
                    .expireTime((String) result.get("expireTime"))
                    .jti((String) result.get("jti"))
                    .maxUseCount((Integer) result.get("maxUseCount"))
                    .usedCount((Integer) result.get("usedCount"))
                    .build();

            return Result.success("获取管理令牌成功", response);
        } catch (Exception e) {
            log.error("获取管理令牌失败", e);
            return Result.fail("获取管理令牌失败：" + e.getMessage());
        }
    }

    /**
     * 刷新令牌
     * PUT /api/v1/tokens/refresh
     * 
     * 需要携带旧令牌（Authorization: Bearer xxx）
     * 支持刷新普通令牌（tokenType=1）和工作令牌（tokenType=3）
     */
    @PutMapping("/refresh")
    public Result<RefreshTokenResponse> refreshToken(
            @Valid @RequestBody RefreshTokenRequest request,
            @RequestHeader(TokenConstants.AUTHORIZATION_HEADER) String authorization,
            @RequestHeader(TokenConstants.DEVICE_ID_HEADER) String deviceId,
            HttpServletRequest httpRequest) {
        
        try {
            // 提取Bearer令牌
            String oldToken = extractBearerToken(authorization);
            if (oldToken == null) {
                return Result.fail("Authorization格式错误");
            }

            String loginIp = getClientIp(httpRequest);

            // 刷新令牌
            Map<String, Object> result = tokenService.refreshToken(
                    oldToken,
                    request.getTokenType(),
                    deviceId,
                    loginIp
            );

            // 构建响应
            RefreshTokenResponse response = RefreshTokenResponse.builder()
                    .newToken((String) result.get("newToken"))
                    .newExpireTime((String) result.get("newExpireTime"))
                    .oldJti((String) result.get("oldJti"))
                    .newJti((String) result.get("newJti"))
                    .build();

            return Result.success("刷新令牌成功", response);
        } catch (Exception e) {
            log.error("刷新令牌失败", e);
            return Result.fail("刷新令牌失败：" + e.getMessage());
        }
    }

    /**
     * 注销令牌
     * DELETE /api/v1/tokens
     * 
     * 需要携带令牌（Authorization: Bearer xxx）
     */
    @DeleteMapping
    public Result<Void> revokeToken(
            @RequestHeader(TokenConstants.AUTHORIZATION_HEADER) String authorization,
            @RequestHeader(TokenConstants.DEVICE_ID_HEADER) String deviceId) {
        
        try {
            // 提取Bearer令牌
            String token = extractBearerToken(authorization);
            if (token == null) {
                return Result.fail("Authorization格式错误");
            }

            // 注销令牌
            tokenService.revokeToken(token, deviceId);

            return Result.success("注销令牌成功");
        } catch (Exception e) {
            log.error("注销令牌失败", e);
            return Result.fail("注销令牌失败：" + e.getMessage());
        }
    }

    // ==================== 辅助方法 ====================

    /**
     * 从Authorization请求头提取Bearer令牌
     */
    private String extractBearerToken(String authorization) {
        if (authorization == null || !authorization.startsWith(TokenConstants.BEARER_PREFIX)) {
            return null;
        }
        return authorization.substring(TokenConstants.BEARER_PREFIX.length());
    }

    /**
     * 获取客户端IP地址
     */
    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        // 处理多个IP的情况（取第一个）
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }
}

