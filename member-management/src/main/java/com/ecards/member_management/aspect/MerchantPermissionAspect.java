package com.ecards.member_management.aspect;

import com.ecards.member_management.annotation.CheckMemberLimit;
import com.ecards.member_management.annotation.CheckStoreLimit;
import com.ecards.member_management.annotation.RequireMerchantActive;
import com.ecards.member_management.annotation.RequireMerchantLevel;
import com.ecards.member_management.context.TokenContext;
import com.ecards.member_management.entity.MerchantExtend;
import com.ecards.member_management.exception.BusinessException;
import com.ecards.member_management.repository.MerchantExtendRepository;
import com.ecards.member_management.repository.StoreRepository;
import com.ecards.member_management.utils.EncryptUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * 商户权限检查切面
 * 拦截带有权限注解的方法，进行权限验证
 * 
 * @author Ecards Team
 * @since 2025-10-29
 */
@Aspect
@Component
@Slf4j
@RequiredArgsConstructor
public class MerchantPermissionAspect {
    
    private final MerchantExtendRepository merchantExtendRepository;
    private final StoreRepository storeRepository;
    private final RedisTemplate<String, String> redisTemplate;
    private final EncryptUtils encryptUtils;
    
    /**
     * 拦截带有 @RequireMerchantActive 注解的方法
     * 检查商户是否被封禁
     */
    @Before("@annotation(requireMerchantActive)")
    public void checkMerchantActive(JoinPoint joinPoint, RequireMerchantActive requireMerchantActive) {
        String userId = TokenContext.getCurrentUserId();
        if (userId == null) {
            throw new BusinessException(401, "用户未登录");
        }
        
        log.debug("检查商户活跃状态: userId={}", userId);
        
        // 先从Redis缓存获取
        String cacheKey = "merchant:certification:" + userId;
        String certificationStr = redisTemplate.opsForValue().get(cacheKey);
        
        Integer certification;
        if (certificationStr != null) {
            certification = Integer.parseInt(certificationStr);
            log.debug("从缓存获取certification: userId={}, certification={}", userId, certification);
        } else {
            // 缓存未命中，查询数据库
            MerchantExtend merchant = merchantExtendRepository.findByUserId(
                encryptUtils.uuidToBytes(userId)
            ).orElse(null);
            
            if (merchant == null) {
                throw new BusinessException(404, "商户信息不存在");
            }
            
            certification = merchant.getCertification();
            // 缓存5分钟
            redisTemplate.opsForValue().set(cacheKey, String.valueOf(certification), 
                Duration.ofMinutes(5));
            log.debug("从数据库获取certification并缓存: userId={}, certification={}", userId, certification);
        }
        
        // 检查是否被封禁 (certification == 7)
        if (certification == 7) {
            log.warn("商户已被封禁: userId={}", userId);
            throw new BusinessException(403, requireMerchantActive.message());
        }
    }
    
    /**
     * 拦截带有 @RequireMerchantLevel 注解的方法
     * 检查商户等级是否满足要求
     */
    @Before("@annotation(requireMerchantLevel)")
    public void checkMerchantLevel(JoinPoint joinPoint, RequireMerchantLevel requireMerchantLevel) {
        String userId = TokenContext.getCurrentUserId();
        if (userId == null) {
            throw new BusinessException(401, "用户未登录");
        }
        
        log.debug("检查商户等级: userId={}, 要求最低等级={}", userId, requireMerchantLevel.minLevel());
        
        // 先从Redis缓存获取
        String cacheKey = "merchant:level:" + userId;
        String levelStr = redisTemplate.opsForValue().get(cacheKey);
        
        Integer merchantLevel;
        if (levelStr != null) {
            merchantLevel = Integer.parseInt(levelStr);
            log.debug("从缓存获取merchantLevel: userId={}, merchantLevel={}", userId, merchantLevel);
        } else {
            // 查询数据库
            MerchantExtend merchant = merchantExtendRepository.findByUserId(
                encryptUtils.uuidToBytes(userId)
            ).orElseThrow(() -> new BusinessException(404, "商户信息不存在"));
            
            merchantLevel = merchant.getMerchantLevel();
            // 缓存5分钟
            redisTemplate.opsForValue().set(cacheKey, String.valueOf(merchantLevel), 
                Duration.ofMinutes(5));
            log.debug("从数据库获取merchantLevel并缓存: userId={}, merchantLevel={}", userId, merchantLevel);
        }
        
        // 检查等级是否满足要求
        if (merchantLevel < requireMerchantLevel.minLevel()) {
            log.warn("商户等级不足: userId={}, 当前等级={}, 要求等级={}", 
                    userId, merchantLevel, requireMerchantLevel.minLevel());
            throw new BusinessException(403, requireMerchantLevel.message());
        }
    }
    
    /**
     * 拦截带有 @CheckMemberLimit 注解的方法
     * 检查会员数量是否超限
     * 
     * 注意：此功能暂未实现，因为系统中还没有会员管理功能
     * 当添加会员管理功能后，需要实现getMemberCount方法
     */
    @Before("@annotation(checkMemberLimit)")
    public void checkMemberLimit(JoinPoint joinPoint, CheckMemberLimit checkMemberLimit) {
        String userId = TokenContext.getCurrentUserId();
        if (userId == null) {
            throw new BusinessException(401, "用户未登录");
        }
        
        log.debug("检查会员数量限制: userId={}", userId);
        
        // 查询商户等级
        MerchantExtend merchant = merchantExtendRepository.findByUserId(
            encryptUtils.uuidToBytes(userId)
        ).orElseThrow(() -> new BusinessException(404, "商户信息不存在"));
        
        // TODO: 暂时不实现会员数量检查，等会员管理功能开发后再补充
        // 当前仅做等级判断，不做实际数量检查
        
        // 示例逻辑（需要在MerchantService中实现getMemberCount方法）:
        // int currentMemberCount = merchantService.getMemberCount(merchant.getMerchantId());
        // int maxMembers = switch (merchant.getMerchantLevel()) {
        //     case 1 -> 200;   // 普通商户最多200个会员
        //     case 2 -> 1000;  // VIP商户最多1000个会员
        //     case 3 -> -1;    // SVIP无限制
        //     default -> 200;
        // };
        // if (maxMembers != -1 && currentMemberCount >= maxMembers) {
        //     throw new BusinessException(403, checkMemberLimit.message());
        // }
        
        log.debug("会员数量检查功能暂未实现，跳过检查");
    }
    
    /**
     * 拦截带有 @CheckStoreLimit 注解的方法
     * 检查店铺数量是否超限
     */
    @Before("@annotation(checkStoreLimit)")
    public void checkStoreLimit(JoinPoint joinPoint, CheckStoreLimit checkStoreLimit) {
        String userId = TokenContext.getCurrentUserId();
        if (userId == null) {
            throw new BusinessException(401, "用户未登录");
        }
        
        log.debug("检查店铺数量限制: userId={}", userId);
        
        // 查询商户信息
        MerchantExtend merchant = merchantExtendRepository.findByUserId(
            encryptUtils.uuidToBytes(userId)
        ).orElseThrow(() -> new BusinessException(404, "商户信息不存在"));
        
        // 查询当前店铺数量（所有状态的店铺）
        Long currentStoreCount = storeRepository.countByMerchantId(merchant.getMerchantId());
        
        // 根据商户等级判断店铺数量限制
        int maxStores;
        if (merchant.getMerchantLevel() == 1) {
            // 普通商户：固定上限2个店铺
            maxStores = 2;
        } else {
            // VIP和SVIP商户：从数据库读取maximum_store_limit
            maxStores = merchant.getMaximumStoreLimit();
        }
        
        // 检查是否已达上限
        if (currentStoreCount >= maxStores) {
            String levelText = switch (merchant.getMerchantLevel()) {
                case 1 -> "普通";
                case 2 -> "VIP";
                case 3 -> "SVIP";
                default -> "普通";
            };
            log.warn("店铺数量已达上限: userId={}, 当前等级={}, 店铺数={}/{}", 
                    userId, levelText, currentStoreCount, maxStores);
            throw new BusinessException(40004, 
                String.format("%s商户最多可创建%d个店铺，当前已有%d个", 
                    levelText, maxStores, currentStoreCount));
        }
        
        log.debug("店铺数量检查通过: userId={}, 当前店铺数={}, 上限={}", 
                userId, currentStoreCount, maxStores);
    }
}

