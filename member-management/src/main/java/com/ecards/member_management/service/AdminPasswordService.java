package com.ecards.member_management.service;

import com.ecards.member_management.constants.AdminConstants;
import com.ecards.member_management.context.AdminContext;
import com.ecards.member_management.dto.request.AdminPasswordUpdateRequest;
import com.ecards.member_management.dto.request.AdminSndPasswordUpdateRequest;
import com.ecards.member_management.entity.Admin;
import com.ecards.member_management.entity.AdminOperationLog;
import com.ecards.member_management.exception.BusinessException;
import com.ecards.member_management.repository.AdminOperationLogRepository;
import com.ecards.member_management.repository.AdminRepository;
import com.ecards.member_management.service.AdminTokenRedisService;
import com.ecards.member_management.utils.EncryptUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 管理员密码管理服务
 * 
 * @author Ecards Team
 * @since 2025-10-29
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminPasswordService {

    private final AdminRepository adminRepository;
    private final AdminOperationLogRepository operationLogRepository;
    private final AdminTokenRedisService tokenRedisService;
    private final EncryptUtils encryptUtils;

    /**
     * 修改登录密码
     */
    @Transactional
    public void updatePassword(AdminPasswordUpdateRequest request) {
        log.info("管理员修改登录密码: adminId={}", AdminContext.getAdminId());

        // 1. 查询管理员
        byte[] adminId = encryptUtils.uuidToBytes(AdminContext.getAdminId());
        Admin admin = adminRepository.findById(adminId)
                .orElseThrow(() -> new BusinessException(404, "管理员不存在"));

        // 2. 验证旧密码
        if (!encryptUtils.verifyPassword(request.getOldPassword(), admin.getPassword())) {
            throw new BusinessException(1001, "旧密码错误");
        }

        // 3. 检查新旧密码是否相同
        if (request.getOldPassword().equals(request.getNewPassword())) {
            throw new BusinessException(1002, "新旧密码不能相同");
        }

        // 4. 更新密码
        String newPasswordHash = encryptUtils.encryptPassword(request.getNewPassword());
        admin.setPassword(newPasswordHash);

        // 5. token_version递增，使旧Token失效
        admin.setTokenVersion(admin.getTokenVersion() + 1);

        adminRepository.save(admin);

        // 6. 清除Redis中的Token缓存
        tokenRedisService.deleteToken(AdminContext.getAdminId(), AdminContext.getDeviceId());

        // 7. 记录操作日志
        logOperation(AdminConstants.OperationType.UPDATE_PASSWORD, "修改登录密码");

        log.info("管理员登录密码修改成功: adminId={}, newTokenVersion={}",
                AdminContext.getAdminId(), admin.getTokenVersion());
    }

    /**
     * 修改二级密码
     */
    @Transactional
    public void updateSndPassword(AdminSndPasswordUpdateRequest request) {
        log.info("管理员修改二级密码: adminId={}", AdminContext.getAdminId());

        // 1. 查询管理员
        byte[] adminId = encryptUtils.uuidToBytes(AdminContext.getAdminId());
        Admin admin = adminRepository.findById(adminId)
                .orElseThrow(() -> new BusinessException(404, "管理员不存在"));

        // 2. 验证旧二级密码
        if (!encryptUtils.verifyPassword(request.getOldSndPassword(), admin.getSndPswd())) {
            throw new BusinessException(1011, "旧二级密码错误");
        }

        // 3. 检查新旧二级密码是否相同
        if (request.getOldSndPassword().equals(request.getNewSndPassword())) {
            throw new BusinessException(1012, "新旧二级密码不能相同");
        }

        // 4. 更新二级密码
        String newSndPasswordHash = encryptUtils.encryptPassword(request.getNewSndPassword());
        admin.setSndPswd(newSndPasswordHash);

        adminRepository.save(admin);

        // 5. 记录操作日志
        logOperation(AdminConstants.OperationType.UPDATE_SND_PASSWORD, "修改二级密码");

        log.info("管理员二级密码修改成功: adminId={}", AdminContext.getAdminId());
    }

    /**
     * 记录操作日志
     */
    private void logOperation(String operationType, String operationDesc) {
        try {
            AdminOperationLog log = new AdminOperationLog();
            log.setAdminId(encryptUtils.uuidToBytes(AdminContext.getAdminId()));
            log.setAdminAccount(AdminContext.getAccount());
            log.setOperationType(operationType);
            log.setOperationDesc(operationDesc);
            log.setOperationIp(AdminContext.getLoginIp());
            log.setDeviceId(AdminContext.getDeviceId());
            log.setResult(1); // 成功
            log.setTargetType("ADMIN");

            operationLogRepository.save(log);
        } catch (Exception e) {
            log.error("记录操作日志失败", e);
        }
    }
}

