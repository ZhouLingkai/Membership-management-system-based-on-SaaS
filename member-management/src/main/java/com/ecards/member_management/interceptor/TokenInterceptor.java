package com.ecards.member_management.interceptor;

import com.ecards.member_management.constants.TokenConstants;
import com.ecards.member_management.context.TokenContext;
import com.ecards.member_management.service.TokenRedisService;
import com.ecards.member_management.utils.JwtUtils;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import java.util.List;

/**
 * 令牌验证拦截器
 * 负责验证JWT令牌的有效性，并将令牌信息存储到ThreadLocal
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TokenInterceptor implements HandlerInterceptor {

    private final TokenRedisService tokenRedisService;
    private final JwtUtils jwtUtils;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 打印请求信息（调试用）
        log.debug("拦截请求: {} {}", request.getMethod(), request.getRequestURI());

        try {
            // 1. 提取Authorization请求头
            String authorization = request.getHeader(TokenConstants.AUTHORIZATION_HEADER);
            if (authorization == null || authorization.isEmpty()) {
                log.warn("请求头中未找到Authorization");
                sendErrorResponse(response, 401, "未提供认证令牌");
                return false;
            }

            // 2. 提取Bearer令牌
            if (!authorization.startsWith(TokenConstants.BEARER_PREFIX)) {
                log.warn("Authorization格式错误: {}", authorization);
                sendErrorResponse(response, 401, "令牌格式错误");
                return false;
            }

            String token = authorization.substring(TokenConstants.BEARER_PREFIX.length());

            // 3. 解析JWT令牌
            String userId;
            String role;
            String merchantId;
            String storeId;
            String deviceId;
            String jti;
            Integer tokenType;
            List<String> permissions;
            String loginIp;

            try {
                userId = jwtUtils.extractUserId(token);
                role = jwtUtils.extractRole(token);
                merchantId = jwtUtils.extractMerchantId(token);
                storeId = jwtUtils.extractStoreId(token);
                deviceId = jwtUtils.extractDeviceId(token);
                jti = jwtUtils.extractJti(token);
                tokenType = jwtUtils.extractTokenType(token);
                permissions = jwtUtils.extractPermissions(token);
                loginIp = request.getRemoteAddr();
            } catch (JwtException e) {
                log.warn("JWT令牌解析失败: {}", e.getMessage());
                sendErrorResponse(response, 401, "令牌无效或已过期");
                return false;
            }

            // 4. 提取Device-ID请求头
            String requestDeviceId = request.getHeader(TokenConstants.DEVICE_ID_HEADER);
            if (requestDeviceId == null || requestDeviceId.isEmpty()) {
                log.warn("请求头中未找到Device-ID");
                sendErrorResponse(response, 401, "未提供设备ID");
                return false;
            }

            // 5. 验证Device-Id是否匹配
            if (!requestDeviceId.equals(deviceId)) {
                log.warn("设备ID不匹配: request={}, token={}", requestDeviceId, deviceId);
                sendErrorResponse(response, 401, "设备ID不匹配");
                return false;
            }

            // 6. 检查令牌是否在黑名单中
            if (tokenRedisService.isInBlacklist(jti)) {
                log.warn("令牌在黑名单中: jti={}", jti);
                sendErrorResponse(response, 401, "令牌已失效");
                return false;
            }

            // 7. 如果是管理令牌，检查使用次数
            if (tokenType != null && tokenType == 4) { // 管理令牌
                Long usedCount = tokenRedisService.getManagerTokenUsage(jti);
                Integer maxCount = 5; // 从配置读取更好
                
                if (usedCount >= maxCount) {
                    log.warn("管理令牌使用次数超限: jti={}, usedCount={}", jti, usedCount);
                    sendErrorResponse(response, 401, "令牌使用次数已达上限");
                    return false;
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

            // 9. 将令牌信息存储到ThreadLocal
            TokenContext.TokenInfo tokenInfo = TokenContext.TokenInfo.builder()
                    .userId(userId)
                    .role(role)
                    .merchantId(merchantId)
                    .storeId(storeId)
                    .deviceId(deviceId)
                    .jti(jti)
                    .tokenType(tokenType)
                    .permissions(permissions)
                    .loginIp(loginIp)
                    .build();

            TokenContext.set(tokenInfo);

            log.debug("令牌验证通过: userId={}, role={}, jti={}", userId, role, jti);
            return true;

        } catch (Exception e) {
            log.error("令牌验证异常", e);
            sendErrorResponse(response, 500, "令牌验证失败");
            return false;
        }
    }

    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) throws Exception {
        // 请求处理完成后的逻辑（如果需要）
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        // 请求完成后，清除ThreadLocal（非常重要！）
        TokenContext.clear();
        log.debug("已清除TokenContext");
    }

    /**
     * 发送错误响应
     */
    private void sendErrorResponse(HttpServletResponse response, int statusCode, String message) throws Exception {
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

