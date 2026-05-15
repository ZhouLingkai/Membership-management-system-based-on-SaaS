package com.ecards.member_management.service;

import com.ecards.member_management.constants.AdminConstants;
import com.ecards.member_management.context.AdminContext;
import com.ecards.member_management.dto.request.MerchantBanRequest;
import com.ecards.member_management.dto.request.MerchantUnbanRequest;
import com.ecards.member_management.dto.request.MerchantWarnRequest;
import com.ecards.member_management.entity.Admin;
import com.ecards.member_management.entity.AdminOperationLog;
import com.ecards.member_management.entity.MerchantExtend;
import com.ecards.member_management.entity.User;
import com.ecards.member_management.exception.BusinessException;
import com.ecards.member_management.repository.AdminOperationLogRepository;
import com.ecards.member_management.repository.AdminRepository;
import com.ecards.member_management.repository.MerchantExtendRepository;
import com.ecards.member_management.repository.UserRepository;
import com.ecards.member_management.utils.EncryptUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 管理员商户管理服务
 * 
 * @author Ecards Team
 * @since 2025-10-29
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminMerchantManagementService {

    private final MerchantExtendRepository merchantExtendRepository;
    private final UserRepository userRepository;
    private final AdminRepository adminRepository;
    private final AdminOperationLogRepository operationLogRepository;
    private final RedisTemplate<String, String> redisTemplate;
    private final EncryptUtils encryptUtils;

    /**
     * 商户警告（将certification改为6）
     * 仅限certification=1（已认证）的商户
     */
    @Transactional
    public void warnMerchant(MerchantWarnRequest request) {
        log.info("管理员警告商户: adminId={}, phone={}", AdminContext.getAdminId(), request.getPhone());

        // 1. 根据手机号查询用户
        User user = userRepository.findByPhone(request.getPhone())
                .orElseThrow(() -> new BusinessException(404, "手机号对应的用户不存在"));

        // 2. 查询商户信息
        MerchantExtend merchant = merchantExtendRepository.findByUserId(user.getUserId())
                .orElseThrow(() -> new BusinessException(404, "该用户不是商户"));

        // 3. 检查当前状态（仅限certification=1）
        if (merchant.getCertification() != 1) {
            String statusDesc = getCertificationDesc(merchant.getCertification());
            throw new BusinessException(400, "当前商户状态为【" + statusDesc + "】，仅【已认证】状态的商户可以被警告");
        }

        // 4. 更新certification为6（警告状态）
        merchant.setCertification(6);
        merchantExtendRepository.save(merchant);

        // 5. 清除Redis缓存，立即生效
        String userId = encryptUtils.bytesToUuid(merchant.getUserId());
        clearMerchantCache(userId);

        // 6. 记录操作日志
        String merchantId = encryptUtils.bytesToUuid(merchant.getMerchantId());
        logOperation(AdminConstants.OperationType.WARN_MERCHANT, 
                "警告商户，手机号: " + request.getPhone(), merchantId);

        log.info("商户警告成功: phone={}, merchantId={}", request.getPhone(), merchantId);
    }

    /**
     * 商户封禁（将certification改为7）
     * 仅限certification∈{1,6}（已认证、警告中）的商户
     */
    @Transactional
    public void banMerchant(MerchantBanRequest request) {
        log.info("管理员封禁商户: adminId={}, phone={}", AdminContext.getAdminId(), request.getPhone());

        // 1. 验证二级密码
        byte[] adminId = encryptUtils.uuidToBytes(AdminContext.getAdminId());
        Admin admin = adminRepository.findById(adminId)
                .orElseThrow(() -> new BusinessException(404, "管理员不存在"));

        if (!encryptUtils.verifyPassword(request.getSndPassword(), admin.getSndPswd())) {
            throw new BusinessException(1011, "二级密码错误");
        }

        // 2. 根据手机号查询用户
        User user = userRepository.findByPhone(request.getPhone())
                .orElseThrow(() -> new BusinessException(404, "手机号对应的用户不存在"));

        // 3. 查询商户信息
        MerchantExtend merchant = merchantExtendRepository.findByUserId(user.getUserId())
                .orElseThrow(() -> new BusinessException(404, "该用户不是商户"));

        // 4. 检查当前状态（仅限certification∈{1,6}）
        if (merchant.getCertification() != 1 && merchant.getCertification() != 6) {
            String statusDesc = getCertificationDesc(merchant.getCertification());
            throw new BusinessException(400, "当前商户状态为【" + statusDesc + "】，仅【已认证】和【警告中】状态的商户可以被封禁");
        }

        // 5. 更新certification为7（封禁状态）
        merchant.setCertification(7);
        merchantExtendRepository.save(merchant);

        // 6. 清除Redis缓存，立即生效
        String userId = encryptUtils.bytesToUuid(merchant.getUserId());
        clearMerchantCache(userId);

        // 7. 记录操作日志
        String merchantId = encryptUtils.bytesToUuid(merchant.getMerchantId());
        logOperation(AdminConstants.OperationType.BAN_MERCHANT, 
                "封禁商户，手机号: " + request.getPhone(), merchantId);

        log.info("商户封禁成功: phone={}, merchantId={}", request.getPhone(), merchantId);
    }

    /**
     * 解除商户封禁（将certification改为1）
     * 仅限certification∈{6,7}（警告中、封禁中）的商户
     */
    @Transactional
    public void unbanMerchant(MerchantUnbanRequest request) {
        log.info("管理员解除商户封禁: adminId={}, phone={}", AdminContext.getAdminId(), request.getPhone());

        // 1. 验证二级密码
        byte[] adminId = encryptUtils.uuidToBytes(AdminContext.getAdminId());
        Admin admin = adminRepository.findById(adminId)
                .orElseThrow(() -> new BusinessException(404, "管理员不存在"));

        if (!encryptUtils.verifyPassword(request.getSndPassword(), admin.getSndPswd())) {
            throw new BusinessException(1011, "二级密码错误");
        }

        // 2. 根据手机号查询用户
        User user = userRepository.findByPhone(request.getPhone())
                .orElseThrow(() -> new BusinessException(404, "手机号对应的用户不存在"));

        // 3. 查询商户信息
        MerchantExtend merchant = merchantExtendRepository.findByUserId(user.getUserId())
                .orElseThrow(() -> new BusinessException(404, "该用户不是商户"));

        // 4. 检查当前状态（仅限certification∈{6,7}）
        if (merchant.getCertification() != 6 && merchant.getCertification() != 7) {
            String statusDesc = getCertificationDesc(merchant.getCertification());
            throw new BusinessException(400, "当前商户状态为【" + statusDesc + "】，仅【警告中】和【封禁中】状态的商户可以被解封");
        }

        // 5. 更新certification为1（已认证状态，恢复正常）
        merchant.setCertification(1);
        merchantExtendRepository.save(merchant);

        // 6. 清除Redis缓存，立即生效
        String userId = encryptUtils.bytesToUuid(merchant.getUserId());
        clearMerchantCache(userId);

        // 7. 记录操作日志
        String merchantId = encryptUtils.bytesToUuid(merchant.getMerchantId());
        logOperation(AdminConstants.OperationType.UNBAN_MERCHANT, 
                "解除商户封禁，手机号: " + request.getPhone(), merchantId);

        log.info("商户解封成功: phone={}, merchantId={}", request.getPhone(), merchantId);
    }

    /**
     * 清除商户Redis缓存
     */
    private void clearMerchantCache(String userId) {
        try {
            redisTemplate.delete("merchant:certification:" + userId);
            redisTemplate.delete("merchant:level:" + userId);
            log.info("商户缓存清除成功: userId={}", userId);
        } catch (Exception e) {
            log.error("清除商户缓存失败: userId={}", userId, e);
        }
    }

    /**
     * 记录操作日志
     */
    private void logOperation(String operationType, String operationDesc, String targetId) {
        try {
            AdminOperationLog log = new AdminOperationLog();
            log.setAdminId(encryptUtils.uuidToBytes(AdminContext.getAdminId()));
            log.setAdminAccount(AdminContext.getAccount());
            log.setOperationType(operationType);
            log.setOperationDesc(operationDesc);
            log.setTargetId(encryptUtils.uuidToBytes(targetId));
            log.setOperationIp(AdminContext.getLoginIp());
            log.setDeviceId(AdminContext.getDeviceId());
            log.setResult(1); // 成功
            log.setTargetType("MERCHANT");

            operationLogRepository.save(log);
        } catch (Exception e) {
            log.error("记录操作日志失败", e);
        }
    }

    /**
     * 获取商户认证状态描述
     */
    private String getCertificationDesc(Integer certification) {
        return switch (certification) {
            case 1 -> "已认证";
            case 2 -> "未认证测试中";
            case 3 -> "审核中";
            case 4 -> "审核拒绝";
            case 5 -> "未认证测试期过/过期后待审核";
            case 6 -> "警告中";
            case 7 -> "封禁中";
            case 13 -> "测试过期后审核中";
            case 14 -> "测试过期后审核拒绝";
            default -> "未知状态(" + certification + ")";
        };
    }
}

