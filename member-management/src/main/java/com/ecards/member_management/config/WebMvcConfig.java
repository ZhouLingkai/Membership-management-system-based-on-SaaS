package com.ecards.member_management.config;

import com.ecards.member_management.interceptor.TokenInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Spring MVC配置类
 * 注册拦截器等配置
 * 
 * 注意：此配置已被Spring Security替代，暂时禁用
 */
// @Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final TokenInterceptor tokenInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 已被Spring Security的JwtAuthenticationFilter替代
        /*
        registry.addInterceptor(tokenInterceptor)
                // 拦截所有/api/v1/tokens/**路径（除了获取普通令牌）
                .addPathPatterns("/api/v1/tokens/**")
                // 排除获取普通令牌的接口（不需要携带令牌）
                .excludePathPatterns("/api/v1/tokens/normal")
                // 排除测试接口（不需要验证令牌）
                .excludePathPatterns("/api/v1/test/**");

        // 注意：如果后续有其他需要令牌验证的接口，可以添加更多路径
        // 例如：.addPathPatterns("/api/v1/users/**", "/api/v1/merchants/**")
        */
    }
}

