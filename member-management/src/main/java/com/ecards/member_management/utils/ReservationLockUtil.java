package com.ecards.member_management.utils;

import com.ecards.member_management.common.ErrorCode;
import com.ecards.member_management.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * 预约资源并发控制工具类
 * 使用Redisson分布式锁确保同一资源同一天同一时间段只能被一个请求操作
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ReservationLockUtil {
    
    private final RedissonClient redissonClient;
    
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");
    
    /**
     * 生成锁键
     * 格式：resv:{YYYYMMDD}:{storeId}:{resourceId}
     * 
     * @param date 预约日期
     * @param storeId 店铺ID
     * @param resourceId 资源ID
     * @return 锁键
     */
    public String generateLockKey(LocalDate date, String storeId, Long resourceId) {
        String dateStr = date.format(DATE_FORMATTER);
        return String.format("resv:%s:%s:%d", dateStr, storeId, resourceId);
    }
    
    /**
     * 执行带锁的预约操作
     * 
     * @param lockKey 锁键
     * @param waitTime 等待获取锁的最大时间（秒）
     * @param leaseTime 锁自动释放时间（秒）
     * @param action 需要执行的业务逻辑
     * @param <T> 返回值类型
     * @return 业务逻辑执行结果
     * @throws BusinessException 如果获取锁失败或执行过程中出错
     */
    public <T> T executeWithLock(String lockKey, int waitTime, int leaseTime, 
                                   Supplier<T> action) {
        RLock lock = redissonClient.getLock(lockKey);
        try {
            // 尝试获取锁，最多等待waitTime秒，锁自动释放时间leaseTime秒
            boolean acquired = lock.tryLock(waitTime, leaseTime, TimeUnit.SECONDS);
            if (!acquired) {
                log.warn("获取预约锁失败，锁键：{}", lockKey);
                throw new BusinessException(ErrorCode.RESERVATION_BUSY, "预约繁忙，请稍后重试");
            }
            
            log.debug("成功获取预约锁，锁键：{}", lockKey);
            
            // 执行业务逻辑
            return action.get();
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("获取预约锁被中断，锁键：{}", lockKey, e);
            throw new BusinessException(ErrorCode.RESERVATION_LOCK_ERROR, "预约锁获取失败");
        } finally {
            // 释放锁（仅当前线程持有时）
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
                log.debug("释放预约锁，锁键：{}", lockKey);
            }
        }
    }
}
