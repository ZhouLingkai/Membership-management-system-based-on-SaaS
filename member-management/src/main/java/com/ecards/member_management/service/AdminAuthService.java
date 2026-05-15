package com.ecards.member_management.service;

import com.ecards.member_management.common.ErrorCode;
import com.ecards.member_management.constants.AdminConstants;
import com.ecards.member_management.entity.Admin;
import com.ecards.member_management.exception.BusinessException;
import com.ecards.member_management.repository.AdminRepository;
import com.ecards.member_management.utils.EncryptUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 管理员认证服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminAuthService {

    private final AdminRepository adminRepository;
    private final AdminTokenService adminTokenService;
    private final EncryptUtils encryptUtils;

    /**
     * 管理员登录
     *
     * @param account  账号
     * @param password 密码
     * @param deviceId 设备ID
     * @param loginIp  登录IP
     * @return AdminToken
     */
    @Transactional
    public String login(String account, String password, String deviceId, String loginIp) {
        log.info("管理员登录请求: account={}, deviceId={}, loginIp={}", account, deviceId, loginIp);

        // 1. 查询管理员
        Admin admin = adminRepository.findByAccount(account)
                .orElseThrow(() -> {
                    log.warn("账号不存在: account={}", account);
                    return new BusinessException(1001, "账号不存在");
                });

        // 2. 验证密码
        if (!encryptUtils.verifyPassword(password, admin.getPassword())) {
            log.warn("密码错误: account={}", account);
            throw new BusinessException(1002, "密码错误");
        }

        // 3. 检查状态
        if (admin.getStatus() != AdminConstants.Status.ENABLED) {
            log.warn("账号已禁用: account={}", account);
            throw new BusinessException(1003, "账号已禁用");
        }

        // 4. 更新最后登录信息
        admin.setLastLoginTime(LocalDateTime.now());
        admin.setLastLoginIp(loginIp);
        adminRepository.save(admin);

        // 5. 生成Token
        String token = adminTokenService.generateAdminToken(admin, deviceId, loginIp);

        log.info("管理员登录成功: account={}, adminRole={}", account, admin.getAdminRole());
        return token;
    }

    /**
     * 根据ID查询管理员
     *
     * @param adminId 管理员ID（UUID字符串）
     * @return Admin
     */
    public Admin getAdminById(String adminId) {
        byte[] adminIdBytes = encryptUtils.uuidToBytes(adminId);
        return adminRepository.findById(adminIdBytes)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_EXIST));
    }

    /**
     * 根据账号查询管理员
     *
     * @param account 账号
     * @return Admin
     */
    public Admin getAdminByAccount(String account) {
        return adminRepository.findByAccount(account)
                .orElseThrow(() -> new BusinessException(1001, "账号不存在"));
    }
}

