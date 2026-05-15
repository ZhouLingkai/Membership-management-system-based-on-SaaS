package com.ecards.member_management.security;

import com.ecards.member_management.filter.AdminJwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfigurationSource;

/**
 * Spring Security配置类
 * 配置安全策略、JWT过滤器和白名单
 */
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    // 用户系统过滤器
    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    
    // 管理员系统过滤器
    private final AdminJwtAuthenticationFilter adminJwtAuthenticationFilter;
    
    // CORS配置源
    private final CorsConfigurationSource corsConfigurationSource;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // 启用CORS（跨域资源共享）
                .cors(cors -> cors.configurationSource(corsConfigurationSource))
                
                // 禁用CSRF（使用JWT不需要CSRF保护）
                .csrf(AbstractHttpConfigurer::disable)
                
                // 配置会话管理为无状态（使用JWT）
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                
                // 配置授权规则
                .authorizeHttpRequests(auth -> auth
                        // 白名单路径（不需要令牌）
                        .requestMatchers(
                                "/api/v1/users/verify-code",            // 获取验证码
                                "/api/v1/users/checkPhone",             // 检查手机号是否注册
                                "/api/v1/users/registration",           // 用户注册
                                "/api/v1/users/login",                  // 用户登录
                                "/api/v1/users/password/reset",         // 用户密码重置
                                "/api/v1/merchants/snd-pwd/reset",      // 商户二级密码重置
                                "/api/v1/tokens/normal",                // 获取普通令牌
                                "/api/v1/admin/login",                  // 管理员登录
                                "/api/v1/admin/password-recovery/**",   // 管理员密码找回（登录密码+二级密码）
                                "/api/v1/test/**",                      // 测试接口
                                "/test.html",                           // 测试页面
                                "/test_merchant.html",                  // 商户测试页面
                                "/test_admin.html",                     // 管理员测试页面
                                "/test_trans.html",                     // 交易测试页面
                                "/test_adresv.html",                    // 高级预约系统测试页面
                                "/error"                                // 错误页面
                        ).permitAll()
                        
                        // 其他所有请求都需要认证
                        .anyRequest().authenticated()
                )
                
                // 添加用户JWT过滤器（普通用户、商户）
                .addFilterBefore(
                        jwtAuthenticationFilter, 
                        UsernamePasswordAuthenticationFilter.class
                )
                
                // 添加管理员JWT过滤器（管理员系统）
                .addFilterBefore(
                        adminJwtAuthenticationFilter,
                        UsernamePasswordAuthenticationFilter.class
                );

        return http.build();
    }
}

