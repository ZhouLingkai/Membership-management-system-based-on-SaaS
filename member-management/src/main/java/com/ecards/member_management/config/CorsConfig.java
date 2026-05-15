package com.ecards.member_management.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.Collections;

/**
 * CORS（跨域资源共享）配置类
 * 允许前端（Web端和小程序端）跨域访问后端API
 */
@Configuration
public class CorsConfig {

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        
        // 允许的源（前端地址）
        // 开发环境：允许 localhost 的各种端口
        // 生产环境：需要修改为实际的前端域名
        configuration.setAllowedOrigins(Arrays.asList(
                "http://localhost:5173",      // Vite 开发服务器默认端口
                "http://localhost:3000",      // 备用端口
                "http://localhost:8080",      // 备用端口
                "http://127.0.0.1:5173",
                "http://127.0.0.1:3000",
                "http://127.0.0.1:8080"
                // 生产环境需要添加实际域名，例如：
                // "https://your-domain.com"
        ));
        
        // 允许的HTTP方法
        configuration.setAllowedMethods(Arrays.asList(
                "GET", 
                "POST", 
                "PUT", 
                "PATCH", 
                "DELETE", 
                "OPTIONS"
        ));
        
        // 允许的请求头
        configuration.setAllowedHeaders(Arrays.asList(
                "Authorization",           // 令牌
                "Content-Type",            // 内容类型
                "Device-ID",               // 设备ID
                "X-Device-ID",             // 设备ID（备用）
                "X-Request-ID",            // 请求ID
                "X-Device-Type",           // 设备类型
                "Accept",
                "Origin",
                "Access-Control-Request-Method",
                "Access-Control-Request-Headers"
        ));
        
        // 允许携带凭证（Cookie、Authorization header等）
        // 这对于自动登录令牌（HttpOnly Cookie）非常重要
        configuration.setAllowCredentials(true);
        
        // 暴露的响应头（前端可以访问）
        configuration.setExposedHeaders(Arrays.asList(
                "Authorization",
                "Set-Cookie",
                "Access-Control-Allow-Origin",
                "Access-Control-Allow-Credentials"
        ));
        
        // 预检请求的缓存时间（秒）
        // 减少OPTIONS请求的频率
        configuration.setMaxAge(3600L);
        
        // 应用到所有路径
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        
        return source;
    }
}
