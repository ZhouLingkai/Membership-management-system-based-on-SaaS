package com.ecards.member_management.service;

import com.ecards.member_management.entity.MerchantExtend;
import com.ecards.member_management.entity.User;
import com.ecards.member_management.repository.MerchantExtendRepository;
import com.ecards.member_management.repository.UserRepository;
import com.ecards.member_management.repository.WorkRelationRepository;
import com.ecards.member_management.utils.EncryptUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 身份验证服务（Mock实现）
 * 用于令牌模块的临时验证逻辑，后续用户管理模块会完善实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthenticationService {

    private final UserRepository userRepository;
    private final MerchantExtendRepository merchantExtendRepository;
    private final WorkRelationRepository workRelationRepository;
    private final EncryptUtils encryptUtils;

    /**
     * 验证手机号和验证码
     * TODO: 后续实现真实的验证码验证逻辑
     *
     * @param phone 手机号（明文）
     * @param code  验证码
     * @return 用户信息，验证失败返回null
     */
    public User validatePhoneAndCode(String phone, String code) {
        try {
            // Mock实现：验证码固定为"123456"
            if (!"123456".equals(code)) {
                log.warn("验证码错误: code={}", code);
                return null;
            }

            return userRepository.findByPhone(phone).orElse(null);
        } catch (Exception e) {
            log.error("验证手机号和验证码失败", e);
            return null;
        }
    }

    /**
     * 验证手机号和密码
     *
     * @param phone        手机号（明文）
     * @param rawPassword  密码（明文）
     * @return 用户信息，验证失败返回null
     */
    public User validatePhoneAndPassword(String phone, String rawPassword) {
        try {
            User user = userRepository.findByPhone(phone).orElse(null);
            if (user == null) {
                log.warn("用户不存在: phone={}", phone);
                return null;
            }

            // 验证密码
            if (!encryptUtils.verifyPassword(rawPassword, user.getPassword())) {
                log.warn("密码错误: userId={}", encryptUtils.bytesToUuid(user.getUserId()));
                return null;
            }

            return user;
        } catch (Exception e) {
            log.error("验证手机号和密码失败", e);
            return null;
        }
    }

    /**
     * 验证自动登录令牌
     * TODO: 后续增加更多安全校验（设备ID、平台等）
     *
     * @param userId   用户ID
     * @param deviceId 设备ID
     * @return 用户信息，验证失败返回null
     */
    public User validateAutoLoginToken(String userId, String deviceId) {
        try {
            byte[] userIdBytes = encryptUtils.uuidToBytes(userId);
            return userRepository.findById(userIdBytes).orElse(null);
        } catch (Exception e) {
            log.error("验证自动登录令牌失败: userId={}", userId, e);
            return null;
        }
    }

    /**
     * 验证商家二级密码
     *
     * @param merchantId     商家ID
     * @param rawSndPswd     二级密码（明文）
     * @return true-验证通过，false-验证失败
     */
    public boolean validateSecondaryPassword(String merchantId, String rawSndPswd) {
        try {
            byte[] merchantIdBytes = encryptUtils.uuidToBytes(merchantId);
            MerchantExtend merchant = merchantExtendRepository.findById(merchantIdBytes).orElse(null);
            
            if (merchant == null) {
                log.warn("商家不存在: merchantId={}", merchantId);
                return false;
            }

            // 验证二级密码
            if (!encryptUtils.verifyPassword(rawSndPswd, merchant.getSndPswd())) {
                log.warn("二级密码错误: merchantId={}", merchantId);
                return false;
            }

            return true;
        } catch (Exception e) {
            log.error("验证二级密码失败: merchantId={}", merchantId, e);
            return false;
        }
    }

    /**
     * 验证用户与店铺的工作关系
     *
     * @param userId  用户ID
     * @param storeId 店铺ID
     * @return true-有效工作关系，false-无效或不存在
     */
    public boolean validateWorkRelation(String userId, String storeId) {
        try {
            byte[] userIdBytes = encryptUtils.uuidToBytes(userId);
            byte[] storeIdBytes = encryptUtils.uuidToBytes(storeId);
            
            // 检查是否存在在职的工作关系
            return workRelationRepository.existsByStoreIdAndUserIdAndStatus(storeIdBytes, userIdBytes, 1);
        } catch (Exception e) {
            log.error("验证工作关系失败: userId={}, storeId={}", userId, storeId, e);
            return false;
        }
    }

    /**
     * 获取用户信息
     *
     * @param userId 用户ID（UUID字符串）
     * @return 用户信息
     */
    public User getUserById(String userId) {
        try {
            byte[] userIdBytes = encryptUtils.uuidToBytes(userId);
            return userRepository.findById(userIdBytes).orElse(null);
        } catch (Exception e) {
            log.error("获取用户信息失败: userId={}", userId, e);
            return null;
        }
    }

    /**
     * 获取商家信息
     *
     * @param userId 用户ID（UUID字符串）
     * @return 商家扩展信息
     */
    public MerchantExtend getMerchantByUserId(String userId) {
        try {
            byte[] userIdBytes = encryptUtils.uuidToBytes(userId);
            return merchantExtendRepository.findByUserId(userIdBytes).orElse(null);
        } catch (Exception e) {
            log.error("获取商家信息失败: userId={}", userId, e);
            return null;
        }
    }

    /**
     * 保存商家信息
     *
     * @param merchant 商家扩展信息
     * @return 保存后的商家信息
     */
    public MerchantExtend saveMerchant(MerchantExtend merchant) {
        try {
            return merchantExtendRepository.save(merchant);
        } catch (Exception e) {
            log.error("保存商家信息失败", e);
            return null;
        }
    }
}

