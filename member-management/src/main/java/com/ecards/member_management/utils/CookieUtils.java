package com.ecards.member_management.utils;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Cookie工具类
 * 用于处理自动登录令牌的Cookie操作
 */
@Slf4j
@Component
public class CookieUtils {

    /**
     * 自动登录令牌Cookie名称
     */
    public static final String AUTO_LOGIN_COOKIE_NAME = "autoLoginToken";

    @Value("${cookie.domain:}")
    private String cookieDomain;

    @Value("${cookie.secure:false}")
    private boolean cookieSecure;

    @Value("${cookie.same-site:Lax}")
    private String cookieSameSite;

    /**
     * 设置自动登录Cookie
     *
     * @param response HTTP响应
     * @param token    自动登录令牌（包含Bearer前缀）
     * @param maxAge   过期时间（秒）
     */
    public void setAutoLoginCookie(HttpServletResponse response, String token, int maxAge) {
        try {
            Cookie cookie = new Cookie(AUTO_LOGIN_COOKIE_NAME, token);
            cookie.setPath("/");
            cookie.setHttpOnly(true);
            cookie.setMaxAge(maxAge);
            cookie.setSecure(cookieSecure);

            // 设置Domain（如果配置了）
            if (cookieDomain != null && !cookieDomain.trim().isEmpty()) {
                cookie.setDomain(cookieDomain);
            }

            // 添加SameSite属性（通过响应头设置，因为Cookie类不直接支持）
            String cookieHeader = buildCookieHeader(AUTO_LOGIN_COOKIE_NAME, token, maxAge);
            response.addHeader("Set-Cookie", cookieHeader);

            log.info("设置自动登录Cookie成功: maxAge={}, secure={}, sameSite={}", 
                    maxAge, cookieSecure, cookieSameSite);
        } catch (Exception e) {
            log.error("设置自动登录Cookie失败", e);
        }
    }

    /**
     * 清除自动登录Cookie
     *
     * @param response HTTP响应
     */
    public void clearAutoLoginCookie(HttpServletResponse response) {
        try {
            // 方法1：设置过期时间为过去时间
            Cookie cookie = new Cookie(AUTO_LOGIN_COOKIE_NAME, "");
            cookie.setPath("/");
            cookie.setHttpOnly(true);
            cookie.setMaxAge(0); // 立即过期
            cookie.setSecure(cookieSecure);

            if (cookieDomain != null && !cookieDomain.trim().isEmpty()) {
                cookie.setDomain(cookieDomain);
            }

            // 方法2：通过响应头设置（更可靠）
            String cookieHeader = buildClearCookieHeader(AUTO_LOGIN_COOKIE_NAME);
            response.addHeader("Set-Cookie", cookieHeader);

            log.info("清除自动登录Cookie成功");
        } catch (Exception e) {
            log.error("清除自动登录Cookie失败", e);
        }
    }

    /**
     * 从请求中获取自动登录令牌
     *
     * @param request HTTP请求
     * @return 自动登录令牌，如果不存在返回null
     */
    public String getAutoLoginTokenFromCookie(HttpServletRequest request) {
        try {
            Cookie[] cookies = request.getCookies();
            if (cookies != null) {
                for (Cookie cookie : cookies) {
                    if (AUTO_LOGIN_COOKIE_NAME.equals(cookie.getName())) {
                        String token = cookie.getValue();
                        if (token != null && !token.trim().isEmpty()) {
                            log.debug("从Cookie获取自动登录令牌成功");
                            return token;
                        }
                    }
                }
            }
            log.debug("Cookie中未找到自动登录令牌");
            return null;
        } catch (Exception e) {
            log.error("从Cookie获取自动登录令牌失败", e);
            return null;
        }
    }

    /**
     * 构建完整的Cookie头（包含SameSite属性）
     *
     * @param name   Cookie名称
     * @param value  Cookie值
     * @param maxAge 过期时间（秒）
     * @return Cookie头字符串
     */
    private String buildCookieHeader(String name, String value, int maxAge) {
        StringBuilder sb = new StringBuilder();
        sb.append(name).append("=").append(value);
        sb.append("; Path=/");
        sb.append("; HttpOnly");
        sb.append("; Max-Age=").append(maxAge);
        
        if (cookieSecure) {
            sb.append("; Secure");
        }
        
        if (cookieSameSite != null && !cookieSameSite.trim().isEmpty()) {
            sb.append("; SameSite=").append(cookieSameSite);
        }
        
        if (cookieDomain != null && !cookieDomain.trim().isEmpty()) {
            sb.append("; Domain=").append(cookieDomain);
        }
        
        return sb.toString();
    }

    /**
     * 构建清除Cookie的头
     *
     * @param name Cookie名称
     * @return Cookie头字符串
     */
    private String buildClearCookieHeader(String name) {
        StringBuilder sb = new StringBuilder();
        sb.append(name).append("=");
        sb.append("; Path=/");
        sb.append("; HttpOnly");
        sb.append("; expires=Thu, 01 Jan 1970 00:00:00 GMT"); // 设置为过去时间
        
        if (cookieSecure) {
            sb.append("; Secure");
        }
        
        if (cookieSameSite != null && !cookieSameSite.trim().isEmpty()) {
            sb.append("; SameSite=").append(cookieSameSite);
        }
        
        if (cookieDomain != null && !cookieDomain.trim().isEmpty()) {
            sb.append("; Domain=").append(cookieDomain);
        }
        
        return sb.toString();
    }

    /**
     * 检查请求是否来自Web端（通过User-Agent判断）
     *
     * @param request HTTP请求
     * @return true-Web端，false-其他端
     */
    public boolean isWebRequest(HttpServletRequest request) {
        String userAgent = request.getHeader("User-Agent");
        if (userAgent == null) {
            return false;
        }
        
        // 简单判断：包含浏览器标识的为Web端
        String ua = userAgent.toLowerCase();
        return ua.contains("mozilla") || ua.contains("chrome") || ua.contains("safari") || 
               ua.contains("firefox") || ua.contains("edge") || ua.contains("opera");
    }
}
