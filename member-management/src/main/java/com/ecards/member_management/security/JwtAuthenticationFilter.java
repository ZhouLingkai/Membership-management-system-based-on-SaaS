package com.ecards.member_management.security;

import com.ecards.member_management.constants.TokenConstants;
import com.ecards.member_management.context.TokenContext;
import com.ecards.member_management.enums.TokenType;
import com.ecards.member_management.service.TokenRedisService;
import com.ecards.member_management.utils.JwtUtils;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

/**
 * JWT认证过滤器
 * 继承OncePerRequestFilter，确保每个请求只执行一次
 */
@Slf4j
@org.springframework.stereotype.Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final TokenRedisService tokenRedisService;
    private final JwtUtils jwtUtils;

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                     @NonNull HttpServletResponse response,
                                     @NonNull FilterChain filterChain) throws ServletException, IOException {
        String requestURI = request.getRequestURI();
        
        // ✅ 管理员接口跳过用户JWT验证，交由AdminJwtAuthenticationFilter处理
        if (requestURI.startsWith("/api/v1/admin")) {
            filterChain.doFilter(request, response);
            return;
        }
        
        try {
            // 1. 提取Authorization请求头
            String authorization = request.getHeader(TokenConstants.AUTHORIZATION_HEADER);
            
            if (authorization == null || !authorization.startsWith(TokenConstants.BEARER_PREFIX)) {
                // 没有令牌，继续过滤链（白名单路径会放行）
                filterChain.doFilter(request, response);
                return;
            }

            String token = authorization.substring(TokenConstants.BEARER_PREFIX.length());

            // 2. 解析JWT令牌
            String userId;
            String role;
            String merchantId;
            String storeId;
            String deviceId;
            String jti;
            Integer tokenType;
            List<String> permissions;
            Integer tokenVersion;

            try {
                // 解析令牌获取所有信息
                io.jsonwebtoken.Claims claims = jwtUtils.parseToken(token);
                
                userId = jwtUtils.extractUserId(token);
                tokenType = jwtUtils.extractTokenType(token);
                
                // 根据令牌类型采用不同的解析策略
                if (tokenType != null && tokenType == TokenType.AUTO_LOGIN.getCode()) {
                    // 自动登录令牌：只解析基础字段，不解析业务字段
                    role = null; // 自动登录令牌不包含role
                    merchantId = null;
                    storeId = null;
                    permissions = null;
                    deviceId = jwtUtils.extractDeviceId(token);
                    jti = jwtUtils.extractJti(token);
                    tokenVersion = claims.get("token_version", Integer.class);
                    log.debug("解析自动登录令牌: userId={}, jti={}", userId, jti);
                } else {
                    // 其他令牌：正常解析所有字段
                    role = jwtUtils.extractRole(token);
                    merchantId = jwtUtils.extractMerchantId(token);
                    storeId = jwtUtils.extractStoreId(token);
                    deviceId = jwtUtils.extractDeviceId(token);
                    jti = jwtUtils.extractJti(token);
                    permissions = jwtUtils.extractPermissions(token);
                    tokenVersion = claims.get("token_version", Integer.class);
                    log.debug("解析业务令牌: userId={}, role={}, tokenType={}", userId, role, tokenType);
                }
            } catch (JwtException e) {
                log.warn("JWT令牌解析失败: {}", e.getMessage());
                sendErrorResponse(response, 401, "令牌无效或已过期");
                return;
            }

            // 3. 提取Device-ID请求头
            String requestDeviceId = request.getHeader(TokenConstants.DEVICE_ID_HEADER);
            if (requestDeviceId == null || requestDeviceId.isEmpty()) {
                log.warn("请求头中未找到Device-ID");
                sendErrorResponse(response, 401, "未提供设备ID");
                return;
            }

            // 4. 验证Device-Id是否匹配
            if (!requestDeviceId.equals(deviceId)) {
                log.warn("设备ID不匹配: request={}, token={}", requestDeviceId, deviceId);
                sendErrorResponse(response, 401, "设备ID不匹配");
                return;
            }

            // 5. 检查令牌是否在黑名单中
            if (tokenRedisService.isInBlacklist(jti)) {
                log.warn("令牌在黑名单中: jti={}", jti);
                sendErrorResponse(response, 401, "令牌已失效");
                return;
            }

            // 6. 检查令牌版本号是否有效
            // 根据令牌类型使用不同的验证逻辑
            if (tokenType != null && tokenType == TokenType.AUTO_LOGIN.getCode()) {
                // 自动登录令牌：验证 user 表的 token_version
                if (!tokenRedisService.isTokenVersionValid(userId, tokenVersion)) {
                    log.warn("自动登录令牌版本号无效: userId={}, tokenVersion={}", userId, tokenVersion);
                    sendErrorResponse(response, 401, "自动登录令牌已过期，请重新登录");
                    return;
                }
                log.debug("自动登录令牌版本验证通过: userId={}, tokenVersion={}", userId, tokenVersion);
            } else if (tokenType != null && tokenType == 3) {
                // 工作令牌：根据角色区分验证逻辑
                if ("merchant".equals(role)) {
                    // 商家工作令牌：跳过版本号检查
                    // 原因：
                    // 1. 商家不在 t_work_relation 表中，无法验证 work token version
                    // 2. 商家拥有最高权限，权限变更不是核心场景
                    // 3. 已有其他安全机制：@RequireMerchantActive 会检查 certification 状态
                    // 4. 工作令牌1小时过期，配合 Redis 缓存清除，风险可控
                    log.debug("商家工作令牌跳过版本号检查: userId={}, role=merchant", userId);
                    // 不执行版本号检查，直接放行
                } else {
                    // 员工/店长工作令牌：验证 work_relation 表的 token_version
                    if (!tokenRedisService.isWorkTokenVersionValid(userId, storeId, tokenVersion)) {
                        log.warn("员工工作令牌版本号无效: userId={}, storeId={}, tokenVersion={}", 
                                userId, storeId, tokenVersion);
                        sendErrorResponse(response, 401, "工作令牌已过期，请重新获取");
                        return;
                    }
                }
            } else {
                // 普通令牌、特权令牌、管理令牌：验证 user 表的 token_version
                if (!tokenRedisService.isTokenVersionValid(userId, tokenVersion)) {
                    log.warn("令牌版本号无效: userId={}, tokenVersion={}", userId, tokenVersion);
                    sendErrorResponse(response, 401, "令牌版本已过期，请重新登录");
                    return;
                }
            }

            // 7. 如果是管理令牌，检查使用次数
            if (tokenType != null && tokenType == 4) { // 管理令牌
                Long usedCount = tokenRedisService.getManagerTokenUsage(jti);
                Integer maxCount = 5;
                
                if (usedCount >= maxCount) {
                    log.warn("管理令牌使用次数超限: jti={}, usedCount={}", jti, usedCount);
                    sendErrorResponse(response, 401, "令牌使用次数已达上限");
                    return;
                }

                // 增加使用次数
                tokenRedisService.incrementManagerTokenUsage(jti);
            }

            // 8. 如果是特权令牌（单次使用），立即加入黑名单
            if (tokenType != null && tokenType == 2) { // 特权令牌
                Long remainingTime = jwtUtils.getRemainingTime(token);
                tokenRedisService.addToBlacklist(jti, remainingTime / 1000);
                log.info("特权令牌已使用，加入黑名单: jti={}", jti);
            }

            // 9. 根据令牌类型设置不同的认证信息
            String userRole;
            if (tokenType != null && tokenType == TokenType.AUTO_LOGIN.getCode()) {
                // 自动登录令牌：使用特殊的角色标识
                userRole = "AUTO_LOGIN_USER";
                log.debug("设置自动登录令牌认证信息: userId={}, role={}", userId, userRole);
            } else {
                // 其他令牌：使用正常的角色逻辑
                userRole = (role != null && !role.trim().isEmpty()) ? role : "NORMAL_USER";
            }
            
            // 将令牌信息存储到ThreadLocal
            TokenContext.TokenInfo tokenInfo = TokenContext.TokenInfo.builder()
                    .userId(userId)
                    .role(userRole) // 使用处理后的role
                    .merchantId(merchantId)
                    .storeId(storeId)
                    .deviceId(deviceId)
                    .jti(jti)
                    .tokenType(tokenType)
                    .permissions(permissions)
                    .loginIp(request.getRemoteAddr())
                    .build();

            TokenContext.set(tokenInfo);

            // 设置Spring Security认证信息
            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                    userId,
                    null,
                    Collections.singletonList(new SimpleGrantedAuthority(userRole))
            );
            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(authentication);

            log.debug("令牌验证通过: userId={}, role={}, tokenType={}, jti={}", userId, userRole, tokenType, jti);

            // 10. 继续过滤链
            filterChain.doFilter(request, response);

        } catch (Exception e) {
            String requestUri = request.getRequestURI();
            String method = request.getMethod();
            log.error("JWT过滤器异常: {} {}, 错误: {}", method, requestUri, e.getMessage(), e);
            sendErrorResponse(response, 500, "令牌验证失败");
        } finally {
            // 清除ThreadLocal（非常重要！）
            TokenContext.clear();
        }
    }

    /**
     * 发送错误响应
     */
    private void sendErrorResponse(HttpServletResponse response, int statusCode, String message) throws IOException {
        response.setStatus(statusCode);
        response.setContentType("application/json;charset=UTF-8");
        
        String json = String.format(
            "{\"code\":%d,\"message\":\"%s\",\"data\":null,\"timestamp\":%d}",
            statusCode,
            message,
            System.currentTimeMillis()
        );
        
        response.getWriter().write(json);
        response.getWriter().flush();
    }
}

