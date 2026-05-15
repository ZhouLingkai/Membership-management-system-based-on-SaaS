package com.ecards.member_management.service;

import com.ecards.member_management.constants.AdminConstants;
import com.ecards.member_management.dto.request.AdminPasswordResetRequest;
import com.ecards.member_management.dto.request.AdminSndPasswordResetRequest;
import com.ecards.member_management.entity.Admin;
import com.ecards.member_management.entity.AdminOperationLog;
import com.ecards.member_management.exception.BusinessException;
import com.ecards.member_management.repository.AdminOperationLogRepository;
import com.ecards.member_management.repository.AdminRepository;
import com.ecards.member_management.utils.EncryptUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * 管理员密码找回服务
 * 
 * @author Ecards Team
 * @since 2025-10-29
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminPasswordRecoveryService {

    private final AdminRepository adminRepository;
    private final AdminOperationLogRepository operationLogRepository;
    private final VerifyCodeService verifyCodeService;
    private final AdminTokenRedisService tokenRedisService;
    private final EncryptUtils encryptUtils;

    /**
     * 找回登录密码
     * 找回成功后递增token_version，使所有Token失效
     */
    @Transactional
    public void resetPassword(AdminPasswordResetRequest request) {
        log.info("管理员找回登录密码: phone=加密");

        // 1. 解密手机号
        String plainPhone = encryptUtils.decryptAES(request.getPhone());
        if (plainPhone == null || plainPhone.isEmpty()) {
            throw new BusinessException(400, "手机号格式错误");
        }

        // 2. 验证验证码（使用加密手机号，因为Redis中存的key是加密手机号）
        if (!verifyCodeService.verifyCode(request.getPhone(), request.getVerifyCode(), request.getDeviceId())) {
            throw new BusinessException(1005, "验证码错误或已过期");
        }

        // 3. 查询管理员（使用明文手机号）
        Admin admin = adminRepository.findByPhone(plainPhone)
                .orElseThrow(() -> new BusinessException(404, "该手机号未注册管理员账号"));

        // 3. 加密新密码
        String newPasswordHash = encryptUtils.encryptPassword(request.getNewPassword());
        admin.setPassword(newPasswordHash);

        // 4. 递增token_version，使所有Token失效
        admin.setTokenVersion(admin.getTokenVersion() + 1);
        adminRepository.save(admin);

        // 5. 清除Redis中的所有Token（可选，因为token_version不匹配会自动拒绝）
        String adminId = encryptUtils.bytesToUuid(admin.getAdminId());
        try {
            tokenRedisService.deleteToken(adminId, request.getDeviceId());
        } catch (Exception e) {
            log.warn("清除Token缓存失败，但不影响密码重置", e);
        }

        // 6. 记录操作日志
        logOperation(admin.getAdminId(), admin.getAccount(), 
                AdminConstants.OperationType.UPDATE_PASSWORD, 
                "找回登录密码（手机号: " + plainPhone.substring(0, 3) + "****" + plainPhone.substring(7) + "）",
                request.getDeviceId());

        log.info("管理员登录密码找回成功: phone={}****, account={}, newTokenVersion={}", 
                plainPhone.substring(0, 3), admin.getAccount(), admin.getTokenVersion());
    }

    /**
     * 找回二级密码
     * 找回成功后不递增token_version
     */
    @Transactional
    public void resetSndPassword(AdminSndPasswordResetRequest request) {
        log.info("管理员找回二级密码: phone=加密");

        // 1. 解密手机号
        String plainPhone = encryptUtils.decryptAES(request.getPhone());
        if (plainPhone == null || plainPhone.isEmpty()) {
            throw new BusinessException(400, "手机号格式错误");
        }

        // 2. 验证验证码（使用加密手机号，因为Redis中存的key是加密手机号）
        if (!verifyCodeService.verifyCode(request.getPhone(), request.getVerifyCode(), request.getDeviceId())) {
            throw new BusinessException(1005, "验证码错误或已过期");
        }

        // 3. 查询管理员（使用明文手机号）
        Admin admin = adminRepository.findByPhone(plainPhone)
                .orElseThrow(() -> new BusinessException(404, "该手机号未注册管理员账号"));

        // 3. 加密新二级密码
        String newSndPasswordHash = encryptUtils.encryptPassword(request.getNewSndPassword());
        admin.setSndPswd(newSndPasswordHash);

        // 4. 保存（不递增token_version）
        adminRepository.save(admin);

        // 5. 记录操作日志
        logOperation(admin.getAdminId(), admin.getAccount(), 
                AdminConstants.OperationType.UPDATE_SND_PASSWORD, 
                "找回二级密码（手机号: " + plainPhone.substring(0, 3) + "****" + plainPhone.substring(7) + "）",
                request.getDeviceId());

        log.info("管理员二级密码找回成功: phone={}****, account={}", plainPhone.substring(0, 3), admin.getAccount());
    }

    /**
     * 记录操作日志
     */
    private void logOperation(byte[] adminId, String adminAccount, String operationType, 
                             String operationDesc, String deviceId) {
        try {
            AdminOperationLog log = new AdminOperationLog();
            log.setAdminId(adminId);
            log.setAdminAccount(adminAccount);
            log.setOperationType(operationType);
            log.setOperationDesc(operationDesc);
            log.setOperationIp("0.0.0.0"); // 找回密码操作无法获取真实IP
            log.setDeviceId(deviceId);
            log.setResult(1); // 成功
            log.setTargetType("ADMIN");

            operationLogRepository.save(log);
        } catch (Exception e) {
            log.error("记录操作日志失败", e);
        }
    }
}

