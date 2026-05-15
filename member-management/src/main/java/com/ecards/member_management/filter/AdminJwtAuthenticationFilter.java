package com.ecards.member_management.filter;

import com.ecards.member_management.constants.AdminConstants;
import com.ecards.member_management.context.AdminContext;
import com.ecards.member_management.service.AdminTokenService;
import io.jsonwebtoken.Claims;
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
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

/**
 * 管理员JWT认证过滤器
 * 
 * 功能：
 * 1. 拦截所有 /api/v1/admin/** 请求（登录接口除外）
 * 2. 从请求头提取AdminToken
 * 3. 验证Token有效性（JWT签名、过期时间、JTI、版本号）
 * 4. 解析Token中的管理员信息，设置到AdminContext
 * 5. 请求结束后清理AdminContext
 * 
 * 注意：
 * - 本过滤器只负责Token解析和上下文设置
 * - 具体的权限验证由 AdminAuthAspect 完成
 * 
 * @author Ecards Team
 * @since 2025-10-28
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AdminJwtAuthenticationFilter extends OncePerRequestFilter {

    private final AdminTokenService adminTokenService;

    /**
     * 白名单路径（不需要Token验证）
     */
    private static final String[] WHITE_LIST = {
            "/api/v1/admin/login",                          // 管理员登录接口
            "/api/v1/admin/password-recovery/reset-password" // 找回登录密码（无需Token）
            // 注意：找回二级密码需要Token验证，因此不在白名单中
    };

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {

        String requestURI = request.getRequestURI();
        
        try {
            // 1. 检查是否是管理员接口
            if (!requestURI.startsWith("/api/v1/admin")) {
                // 不是管理员接口，直接放行
                filterChain.doFilter(request, response);
                return;
            }

            // 2. 检查是否在白名单中
            if (isWhiteListed(requestURI)) {
                filterChain.doFilter(request, response);
                return;
            }

            // 3. 从请求头获取Token
            String token = extractToken(request);
            
            if (token == null || token.isEmpty()) {
                log.warn("管理员接口缺少Token: {}", requestURI);
                sendUnauthorizedResponse(response, "缺少管理员Token");
                return;
            }

            // 4. 验证并解析Token
            Claims claims = adminTokenService.validateAndParseToken(token);
            
            if (claims == null) {
                log.warn("管理员Token验证失败: uri={}", requestURI);
                sendUnauthorizedResponse(response, "Token无效或已过期");
                return;
            }

            // 5. 从Claims中提取管理员信息
            String adminId = claims.get(AdminConstants.JwtClaims.ADMIN_ID, String.class);
            String account = claims.getSubject(); // 如果存储了account
            Integer adminRole = claims.get(AdminConstants.JwtClaims.ADMIN_ROLE, Integer.class);
            String roleCode = claims.get(AdminConstants.JwtClaims.ROLE_CODE, String.class);
            String deviceId = claims.get(AdminConstants.JwtClaims.DEVICE_ID, String.class);
            String loginIp = claims.get(AdminConstants.JwtClaims.LOGIN_IP, String.class);
            Integer tokenVersion = claims.get(AdminConstants.JwtClaims.TOKEN_VERSION, Integer.class);

            // 6. 设置AdminContext
            AdminContext.AdminInfo adminInfo = AdminContext.AdminInfo.builder()
                    .adminId(adminId)
                    .account(account)
                    .adminRole(adminRole)
                    .roleCode(roleCode)
                    .deviceId(deviceId)
                    .loginIp(loginIp)
                    .tokenVersion(tokenVersion)
                    .build();

            AdminContext.setAdminInfo(adminInfo);

            // 7. 设置Spring Security的认证信息（关键：让Spring Security知道用户已认证）
            List<SimpleGrantedAuthority> authorities = Collections.singletonList(
                    new SimpleGrantedAuthority("ROLE_ADMIN_" + roleCode)
            );
            
            UsernamePasswordAuthenticationToken authentication = 
                    new UsernamePasswordAuthenticationToken(
                            adminId,        // principal（认证主体：管理员ID）
                            null,           // credentials（凭证：已认证，无需密码）
                            authorities     // authorities（权限）
                    );
            
            // 设置请求详细信息（IP地址等）
            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            
            // 将认证信息存入Spring Security上下文
            SecurityContextHolder.getContext().setAuthentication(authentication);
            
            log.info("✅ Spring Security认证设置成功: adminId={}, role={}", adminId, roleCode);

            // 8. 继续执行后续过滤器和Controller
            filterChain.doFilter(request, response);
            
            // 9. 请求成功完成后清理上下文（防止内存泄漏）
            AdminContext.clear();
            SecurityContextHolder.clearContext();

        } catch (Exception e) {
            log.error("管理员Token验证异常: uri={}, error={}", requestURI, e.getMessage(), e);
            AdminContext.clear(); // 异常时也要清理
            SecurityContextHolder.clearContext(); // 清理Spring Security上下文
            sendUnauthorizedResponse(response, "Token验证异常");
        }
    }

    /**
     * 从请求头提取Token
     * 支持两种方式：
     * 1. Authorization: Bearer {token}
     * 2. AdminToken: {token}
     */
    private String extractToken(HttpServletRequest request) {
        // 方式1: Authorization头
        String authHeader = request.getHeader(AdminConstants.Header.AUTHORIZATION);
        if (authHeader != null && authHeader.startsWith(AdminConstants.Header.TOKEN_PREFIX)) {
            return authHeader; // 返回完整的 "Bearer {token}"
        }

        // 方式2: AdminToken头（兼容）
        String adminToken = request.getHeader("AdminToken");
        if (adminToken != null && !adminToken.isEmpty()) {
            return AdminConstants.Header.TOKEN_PREFIX + adminToken; // 添加Bearer前缀
        }

        return null;
    }

    /**
     * 检查URI是否在白名单中
     */
    private boolean isWhiteListed(String uri) {
        for (String whiteUri : WHITE_LIST) {
            if (uri.equals(whiteUri) || uri.startsWith(whiteUri + "/")) {
                return true;
            }
        }
        return false;
    }

    /**
     * 发送401未认证响应
     */
    private void sendUnauthorizedResponse(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json;charset=UTF-8");
        
        String jsonResponse = String.format(
                "{\"code\":401,\"message\":\"%s\",\"data\":null}", 
                message
        );
        
        response.getWriter().write(jsonResponse);
        response.getWriter().flush();
    }
}


