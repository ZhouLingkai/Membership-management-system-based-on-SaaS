package com.ecards.member_management.service;

import com.ecards.member_management.entity.*;
import com.ecards.member_management.repository.*;
import com.ecards.member_management.utils.EncryptUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 测试数据Service
 * 提供创建测试数据的方法，仅用于开发测试阶段
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TestDataService {

    private final UserRepository userRepository;
    private final MerchantExtendRepository merchantExtendRepository;
    private final StoreRepository storeRepository;
    private final WorkRelationRepository workRelationRepository;
    private final EncryptUtils encryptUtils;

    /**
     * 创建测试用户
     *
     * @param phone    手机号（明文）
     * @param password 密码（明文）
     * @param nickname 昵称
     * @param userType 用户类型
     * @return 用户ID（UUID字符串）
     */
    @Transactional
    public String createTestUser(String phone, String password, String nickname, Integer userType) {
        try {
            // 检查手机号是否已存在（使用明文）
            if (userRepository.existsByPhone(phone)) {
                log.warn("手机号已存在: {}", phone);
                return null;
            }

            // 加密密码
            String encryptedPassword = encryptUtils.encryptPassword(password);

            // 生成UUID
            String userIdStr = UUID.randomUUID().toString();
            byte[] userId = encryptUtils.uuidToBytes(userIdStr);

            // 创建用户
            User user = new User();
            user.setUserId(userId);
            user.setPhone(phone); // 存储明文手机号
            user.setPassword(encryptedPassword);
            user.setNickname(nickname);
            user.setUserType(userType);
            user.setRegisterTime(LocalDateTime.now());
            user.setUpdateTime(LocalDateTime.now());

            userRepository.save(user);
            log.info("创建测试用户成功: userId={}, phone={}", userIdStr, phone);
            
            return userIdStr;
        } catch (Exception e) {
            log.error("创建测试用户失败", e);
            throw new RuntimeException("创建测试用户失败: " + e.getMessage());
        }
    }

    /**
     * 创建测试商家
     *
     * @param userId       用户ID（UUID字符串）
     * @param merchantName 商家名称
     * @param sndPswd      二级密码（明文）
     * @return 商家ID（UUID字符串）
     */
    @Transactional
    public String createTestMerchant(String userId, String merchantName, String sndPswd) {
        try {
            byte[] userIdBytes = encryptUtils.uuidToBytes(userId);
            
            // 检查用户是否存在
            if (!userRepository.existsById(userIdBytes)) {
                log.warn("用户不存在: {}", userId);
                return null;
            }

            // 检查是否已开通商家
            if (merchantExtendRepository.existsByUserId(userIdBytes)) {
                log.warn("用户已开通商家: {}", userId);
                return null;
            }

            // 加密二级密码
            String encryptedSndPswd = encryptUtils.encryptPassword(sndPswd);

            // 生成商家UUID
            String merchantIdStr = UUID.randomUUID().toString();
            byte[] merchantId = encryptUtils.uuidToBytes(merchantIdStr);

            // 创建商家扩展信息
            MerchantExtend merchant = new MerchantExtend();
            merchant.setMerchantId(merchantId);
            merchant.setUserId(userIdBytes);
            merchant.setMerchantName(merchantName);
            merchant.setMerchantLevel(1); // 默认普通商家
            merchant.setSndPswd(encryptedSndPswd);
            merchant.setPrivilegeExpireTime(LocalDateTime.now().plusYears(1)); // 默认1年有效期
            merchant.setRemainingNoticeCount(100); // 默认100次通知
            merchant.setCreateTime(LocalDateTime.now());
            merchant.setUpdateTime(LocalDateTime.now());

            merchantExtendRepository.save(merchant);
            
            // 更新用户类型为商家
            User user = userRepository.findById(userIdBytes).orElse(null);
            if (user != null) {
                user.setUserType(2); // 商家用户
                userRepository.save(user);
            }

            log.info("创建测试商家成功: merchantId={}, userId={}", merchantIdStr, userId);
            return merchantIdStr;
        } catch (Exception e) {
            log.error("创建测试商家失败", e);
            throw new RuntimeException("创建测试商家失败: " + e.getMessage());
        }
    }

    /**
     * 创建测试店铺
     *
     * @param merchantId 商家ID（UUID字符串）
     * @param storeName  店铺名称
     * @param address    店铺地址
     * @return 店铺ID（UUID字符串）
     */
    @Transactional
    public String createTestStore(String merchantId, String storeName, String address) {
        try {
            byte[] merchantIdBytes = encryptUtils.uuidToBytes(merchantId);
            
            // 检查商家是否存在
            if (!merchantExtendRepository.existsById(merchantIdBytes)) {
                log.warn("商家不存在: {}", merchantId);
                return null;
            }

            // 生成店铺UUID
            String storeIdStr = UUID.randomUUID().toString();
            byte[] storeId = encryptUtils.uuidToBytes(storeIdStr);

            // 创建店铺
            Store store = new Store();
            store.setStoreId(storeId);
            store.setMerchantId(merchantIdBytes);
            store.setStoreName(storeName);
            store.setAddress(address);
            store.setStorePhotos("http://example.com/photo.jpg"); // 测试数据
            store.setBusinessLicense("http://example.com/license.jpg"); // 测试数据
            store.setContactPhone(encryptUtils.encryptAES("13800138000")); // 测试手机号
            store.setStatus(1); // 正常营业
            store.setBusinessTime("9:00-21:00");
            store.setOpenStoreTime(LocalDateTime.now());
            store.setCreateTime(LocalDateTime.now());
            store.setUpdateTime(LocalDateTime.now());

            storeRepository.save(store);
            log.info("创建测试店铺成功: storeId={}, merchantId={}", storeIdStr, merchantId);
            
            return storeIdStr;
        } catch (Exception e) {
            log.error("创建测试店铺失败", e);
            throw new RuntimeException("创建测试店铺失败: " + e.getMessage());
        }
    }

    /**
     * 创建工作关系
     *
     * @param storeId 店铺ID（UUID字符串）
     * @param userId  用户ID（UUID字符串）
     * @param role    角色（manager/employee）
     * @param name    员工姓名
     * @return 关系ID
     */
    @Transactional
    public Long createWorkRelation(String storeId, String userId, String role, String name) {
        try {
            byte[] storeIdBytes = encryptUtils.uuidToBytes(storeId);
            byte[] userIdBytes = encryptUtils.uuidToBytes(userId);
            
            // 检查店铺和用户是否存在
            if (!storeRepository.existsById(storeIdBytes)) {
                log.warn("店铺不存在: {}", storeId);
                return null;
            }
            if (!userRepository.existsById(userIdBytes)) {
                log.warn("用户不存在: {}", userId);
                return null;
            }

            // 检查是否已存在工作关系
            if (workRelationRepository.existsByStoreIdAndUserIdAndStatus(storeIdBytes, userIdBytes, 1)) {
                log.warn("工作关系已存在: storeId={}, userId={}", storeId, userId);
                return null;
            }

            // 创建工作关系
            WorkRelation workRelation = new WorkRelation();
            workRelation.setStoreId(storeIdBytes);
            workRelation.setUserId(userIdBytes);
            workRelation.setRole(role);
            workRelation.setName(name);
            
            // 默认权限
            String defaultPermission = role.equals("manager") 
                ? "{\"permissions\": [\"staff_add\", \"member_card_create\", \"transaction_recharge\"]}"
                : "{\"permissions\": [\"member_card_create\"]}";
            workRelation.setPermission(defaultPermission);
            
            workRelation.setStatus(1); // 在职
            workRelation.setEntryTime(LocalDateTime.now());
            workRelation.setUpdateTime(LocalDateTime.now());

            WorkRelation saved = workRelationRepository.save(workRelation);
            
            // 更新用户类型为员工
            User user = userRepository.findById(userIdBytes).orElse(null);
            if (user != null && user.getUserType() == 1) {
                user.setUserType(3); // 员工用户
                userRepository.save(user);
            }

            log.info("创建工作关系成功: id={}, storeId={}, userId={}", saved.getId(), storeId, userId);
            return saved.getId();
        } catch (Exception e) {
            log.error("创建工作关系失败", e);
            throw new RuntimeException("创建工作关系失败: " + e.getMessage());
        }
    }

    /**
     * 获取用户信息（用于验证）
     */
    public User getUserById(String userId) {
        byte[] userIdBytes = encryptUtils.uuidToBytes(userId);
        return userRepository.findById(userIdBytes).orElse(null);
    }

    /**
     * 获取商家信息（用于验证）
     */
    public MerchantExtend getMerchantById(String merchantId) {
        byte[] merchantIdBytes = encryptUtils.uuidToBytes(merchantId);
        return merchantExtendRepository.findById(merchantIdBytes).orElse(null);
    }

    /**
     * 获取店铺信息（用于验证）
     */
    public Store getStoreById(String storeId) {
        byte[] storeIdBytes = encryptUtils.uuidToBytes(storeId);
        return storeRepository.findById(storeIdBytes).orElse(null);
    }
}

