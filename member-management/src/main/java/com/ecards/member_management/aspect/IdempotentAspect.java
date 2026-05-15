package com.ecards.member_management.aspect;

import com.ecards.member_management.annotation.Idempotent;
import com.ecards.member_management.common.ErrorCode;
import com.ecards.member_management.exception.BusinessException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.concurrent.TimeUnit;

/**
 * 幂等性切面
 * 使用Redis的SET NX实现幂等性校验，防止重复提交
 * 
 * 工作原理：
 * 1. 从请求头获取X-Request-ID
 * 2. 使用Redis的SETNX原子操作尝试设置key
 * 3. 如果设置成功，说明是首次请求，执行业务逻辑
 * 4. 如果设置失败，说明是重复请求，抛出异常
 * 
 * @author Ecards Team
 * @since 2025-11-04
 */
@Aspect
@Component
@Slf4j
@RequiredArgsConstructor
public class IdempotentAspect {
    
    private final StringRedisTemplate redisTemplate;
    
    @Around("@annotation(idempotent)")
    public Object around(ProceedingJoinPoint joinPoint, Idempotent idempotent) throws Throwable {
        // 1. 获取Request-ID
        ServletRequestAttributes attributes = (ServletRequestAttributes) 
            RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "无法获取请求上下文");
        }
        
        HttpServletRequest request = attributes.getRequest();
        String requestId = request.getHeader("X-Request-ID");
        
        if (!StringUtils.hasText(requestId)) {
            throw new BusinessException(ErrorCode.REQUEST_ID_MISSING);
        }
        
        // 2. 构建Redis Key
        String methodName = joinPoint.getSignature().getName();
        String className = joinPoint.getTarget().getClass().getSimpleName();
        String redisKey = String.format("idempotent:%s:%s:%s", className, methodName, requestId);
        
        // 3. 尝试设置Redis（原子操作，防止并发）
        // SET NX EX：如果不存在则设置，过期时间
        Boolean success = redisTemplate.opsForValue()
            .setIfAbsent(redisKey, "1", idempotent.timeout(), TimeUnit.SECONDS);
        
        // 4. 如果设置失败，说明已经有相同请求正在处理或已处理
        if (Boolean.FALSE.equals(success)) {
            String businessDesc = StringUtils.hasText(idempotent.value()) ? 
                idempotent.value() : methodName;
            log.warn("幂等性拦截：重复请求 method={}, requestId={}, business={}", 
                methodName, requestId, businessDesc);
            throw new BusinessException(ErrorCode.DUPLICATE_REQUEST);
        }
        
        // 5. 执行业务逻辑
        log.info("幂等性检查通过：method={}, requestId={}", methodName, requestId);
        return joinPoint.proceed();
    }
}

