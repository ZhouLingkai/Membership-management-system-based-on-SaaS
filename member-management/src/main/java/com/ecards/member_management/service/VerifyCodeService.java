package com.ecards.member_management.service;

import com.ecards.member_management.common.ErrorCode;
import com.ecards.member_management.exception.BusinessException;
import com.ecards.member_management.utils.EncryptUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.TimeUnit;

/**
 * 验证码服务
 * 负责验证码的生成、存储、验证和频率限制
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VerifyCodeService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final EncryptUtils encryptUtils;

    private static final String VERIFY_CODE_PREFIX = "verify:code:";
    private static final String LIMIT_MINUTE_PREFIX = "verify:limit:minute:";
    private static final String LIMIT_DAILY_PREFIX = "verify:limit:daily:";
    
    private static final int CODE_LENGTH = 6;
    private static final int CODE_EXPIRE_SECONDS = 300; // 5分钟
    private static final int MINUTE_LIMIT_SECONDS = 60; // 1分钟
    private static final int DAILY_MAX_COUNT = 5; // 每天最多5次
    
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * 生成并发送验证码
     *
     * @param encryptedPhone 加密后的手机号
     * @param deviceId       设备ID
     * @param platform       平台类型
     * @return 发送时间和剩余次数
     */
    public Map<String, Object> generateAndSendCode(String encryptedPhone, String deviceId, String platform) {
        // 0. 解密手机号，使用明文作为Redis key（解决前端每次加密结果不同的问题）
        String plainPhone = encryptUtils.decryptAES(encryptedPhone);
        if (plainPhone == null || plainPhone.isEmpty()) {
            log.error("手机号解密失败: encryptedPhone={}", encryptedPhone.substring(0, Math.min(10, encryptedPhone.length())));
            throw new BusinessException(ErrorCode.PARAM_ERROR, "手机号格式错误");
        }
        
        // 1. 检查1分钟频率限制
        checkMinuteLimit(plainPhone);

        // 2. 检查每日次数限制
        int remainingCount = checkDailyLimit(plainPhone);

        // 3. 生成6位随机验证码
        String code = generateRandomCode();

        // 4. 打印验证码到控制台（替代阿里云短信发送）
        log.info("=".repeat(60));
        log.info("【验证码】手机号: {}, 验证码: {}", plainPhone, code);
        log.info("=".repeat(60));

        // 5. 存储验证码到Redis（使用明文手机号）
        storeVerifyCode(plainPhone, code, deviceId);

        // 6. 记录1分钟限制
        recordMinuteLimit(plainPhone);

        // 7. 增加每日计数
        incrementDailyCount(plainPhone);

        // 8. 返回结果
        Map<String, Object> result = new HashMap<>();
        result.put("sendTime", LocalDateTime.now(ZoneId.of("Asia/Shanghai")).format(DATETIME_FORMATTER));
        result.put("expireSeconds", CODE_EXPIRE_SECONDS);
        result.put("remainingRetries", remainingCount - 1);
        
        return result;
    }

    /**
     * 验证验证码（默认验证成功后删除）
     *
     * @param encryptedPhone 加密后的手机号
     * @param code           验证码
     * @param deviceId       设备ID
     * @return true-验证通过，false-验证失败
     */
    public boolean verifyCode(String encryptedPhone, String code, String deviceId) {
        return verifyCode(encryptedPhone, code, deviceId, true);
    }

    /**
     * 验证验证码
     *
     * @param encryptedPhone 加密后的手机号
     * @param code           验证码
     * @param deviceId       设备ID
     * @param deleteOnSuccess 验证成功后是否删除
     * @return true-验证通过，false-验证失败
     */
    public boolean verifyCode(String encryptedPhone, String code, String deviceId, boolean deleteOnSuccess) {
        try {
            // 解密手机号，使用明文作为Redis key（解决前端每次加密结果不同的问题）
            String plainPhone = encryptUtils.decryptAES(encryptedPhone);
            if (plainPhone == null || plainPhone.isEmpty()) {
                log.warn("验证码验证失败: 手机号解密失败");
                return false;
            }
            
            String key = VERIFY_CODE_PREFIX + plainPhone;
            
            // 获取存储的验证码信息
            Map<Object, Object> codeInfo = redisTemplate.opsForHash().entries(key);
            
            if (codeInfo.isEmpty()) {
                log.warn("验证码不存在或已过期: phone={}", plainPhone);
                return false;
            }

            String storedCode = (String) codeInfo.get("code");
            String storedDeviceId = (String) codeInfo.get("deviceId");

            // 验证验证码和设备ID
            boolean isValid = code.equals(storedCode) && deviceId.equals(storedDeviceId);

            if (isValid) {
                if (deleteOnSuccess) {
                    // 验证成功后删除验证码（一次性使用）
                    redisTemplate.delete(key);
                    log.info("验证码验证成功并删除: phone={}", plainPhone);
                } else {
                    log.info("验证码验证成功(保留): phone={}", plainPhone);
                }
            } else {
                log.warn("验证码验证失败: phone={}, inputCode={}, storedCode={}, deviceIdMatch={}", 
                        plainPhone, code, storedCode, deviceId.equals(storedDeviceId));
            }

            return isValid;
        } catch (Exception e) {
            log.error("验证码验证异常", e);
            return false;
        }
    }

    /**
     * 删除验证码
     * @param encryptedPhone 加密后的手机号
     */
    public void deleteCode(String encryptedPhone) {
        try {
            String plainPhone = encryptUtils.decryptAES(encryptedPhone);
            if (plainPhone != null && !plainPhone.isEmpty()) {
                String key = VERIFY_CODE_PREFIX + plainPhone;
                redisTemplate.delete(key);
                log.info("验证码已手动删除: phone={}", plainPhone);
            }
        } catch (Exception e) {
            log.error("删除验证码失败", e);
        }
    }

    /**
     * 生成随机6位数字验证码
     */
    private String generateRandomCode() {
        Random random = new Random();
        int code = 100000 + random.nextInt(900000);
        return String.valueOf(code);
    }

    /**
     * 存储验证码到Redis
     */
    private void storeVerifyCode(String plainPhone, String code, String deviceId) {
        String key = VERIFY_CODE_PREFIX + plainPhone;
        
        Map<String, String> codeInfo = new HashMap<>();
        codeInfo.put("code", code);
        codeInfo.put("deviceId", deviceId);
        codeInfo.put("timestamp", String.valueOf(System.currentTimeMillis()));

        redisTemplate.opsForHash().putAll(key, codeInfo);
        redisTemplate.expire(key, CODE_EXPIRE_SECONDS, TimeUnit.SECONDS);

        log.info("验证码已存储到Redis: phone={}, expireSeconds={}", plainPhone, CODE_EXPIRE_SECONDS);
    }

    /**
     * 检查1分钟频率限制
     */
    private void checkMinuteLimit(String plainPhone) {
        String key = LIMIT_MINUTE_PREFIX + plainPhone;
        
        if (Boolean.TRUE.equals(redisTemplate.hasKey(key))) {
            Long ttl = redisTemplate.getExpire(key, TimeUnit.SECONDS);
            throw new BusinessException(ErrorCode.PARAM_ERROR, 
                    String.format("请求过于频繁，请在%d秒后重试", ttl != null ? ttl : 60));
        }
    }

    /**
     * 记录1分钟限制
     */
    private void recordMinuteLimit(String plainPhone) {
        String key = LIMIT_MINUTE_PREFIX + plainPhone;
        redisTemplate.opsForValue().set(key, System.currentTimeMillis(), MINUTE_LIMIT_SECONDS, TimeUnit.SECONDS);
    }

    /**
     * 检查每日次数限制
     *
     * @return 剩余次数
     */
    private int checkDailyLimit(String plainPhone) {
        String today = LocalDate.now(ZoneId.of("Asia/Shanghai")).format(DATE_FORMATTER);
        String key = LIMIT_DAILY_PREFIX + plainPhone + ":" + today;

        Object countObj = redisTemplate.opsForValue().get(key);
        int currentCount = countObj != null ? Integer.parseInt(countObj.toString()) : 0;

        if (currentCount >= DAILY_MAX_COUNT) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "今日验证码请求次数已达上限（5次），请明天再试");
        }

        return DAILY_MAX_COUNT - currentCount;
    }

    /**
     * 增加每日计数
     */
    private void incrementDailyCount(String plainPhone) {
        String today = LocalDate.now(ZoneId.of("Asia/Shanghai")).format(DATE_FORMATTER);
        String key = LIMIT_DAILY_PREFIX + plainPhone + ":" + today;

        Long count = redisTemplate.opsForValue().increment(key);
        
        // 设置过期时间为当天23:59:59
        if (count != null && count == 1) {
            LocalDateTime endOfDay = LocalDate.now(ZoneId.of("Asia/Shanghai"))
                    .atTime(23, 59, 59);
            long secondsUntilEndOfDay = java.time.Duration.between(
                    LocalDateTime.now(ZoneId.of("Asia/Shanghai")), endOfDay).getSeconds();
            redisTemplate.expire(key, secondsUntilEndOfDay, TimeUnit.SECONDS);
        }

        log.info("每日验证码计数+1: phone={}, count={}", plainPhone, count);
    }
}

