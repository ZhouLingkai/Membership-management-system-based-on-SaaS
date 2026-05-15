package com.ecards.member_management.controller;

import com.ecards.member_management.common.Result;
import com.ecards.member_management.entity.*;
import com.ecards.member_management.service.TestDataService;
import com.ecards.member_management.utils.EncryptUtils;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * 测试控制器
 * 提供测试数据创建接口，仅用于开发测试阶段
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/test")
@RequiredArgsConstructor
public class TestController {

    private final TestDataService testDataService;
    private final EncryptUtils encryptUtils;

    /**
     * 创建测试用户
     */
    @PostMapping("/users")
    public Result<String> createTestUser(@RequestBody CreateUserRequest request) {
        try {
            String userId = testDataService.createTestUser(
                request.getPhone(),
                request.getPassword(),
                request.getNickname(),
                request.getUserType()
            );
            
            if (userId == null) {
                return Result.fail("创建用户失败：手机号可能已存在");
            }
            
            return Result.success("创建用户成功", userId);
        } catch (Exception e) {
            log.error("创建测试用户失败", e);
            return Result.fail("创建用户失败：" + e.getMessage());
        }
    }

    /**
     * 创建测试商家
     */
    @PostMapping("/merchants")
    public Result<String> createTestMerchant(@RequestBody CreateMerchantRequest request) {
        try {
            String merchantId = testDataService.createTestMerchant(
                request.getUserId(),
                request.getMerchantName(),
                request.getSndPswd()
            );
            
            if (merchantId == null) {
                return Result.fail("创建商家失败：用户可能不存在或已开通商家");
            }
            
            return Result.success("创建商家成功", merchantId);
        } catch (Exception e) {
            log.error("创建测试商家失败", e);
            return Result.fail("创建商家失败：" + e.getMessage());
        }
    }

    /**
     * 创建测试店铺
     */
    @PostMapping("/stores")
    public Result<String> createTestStore(@RequestBody CreateStoreRequest request) {
        try {
            String storeId = testDataService.createTestStore(
                request.getMerchantId(),
                request.getStoreName(),
                request.getAddress()
            );
            
            if (storeId == null) {
                return Result.fail("创建店铺失败：商家可能不存在");
            }
            
            return Result.success("创建店铺成功", storeId);
        } catch (Exception e) {
            log.error("创建测试店铺失败", e);
            return Result.fail("创建店铺失败：" + e.getMessage());
        }
    }

    /**
     * 创建工作关系
     */
    @PostMapping("/work-relations")
    public Result<Long> createWorkRelation(@RequestBody CreateWorkRelationRequest request) {
        try {
            Long relationId = testDataService.createWorkRelation(
                request.getStoreId(),
                request.getUserId(),
                request.getRole(),
                request.getName()
            );
            
            if (relationId == null) {
                return Result.fail("创建工作关系失败：店铺或用户可能不存在，或关系已存在");
            }
            
            return Result.success("创建工作关系成功", relationId);
        } catch (Exception e) {
            log.error("创建工作关系失败", e);
            return Result.fail("创建工作关系失败：" + e.getMessage());
        }
    }

    /**
     * 查询用户信息
     */
    @GetMapping("/users/{userId}")
    public Result<UserInfo> getUserById(@PathVariable String userId) {
        try {
            User user = testDataService.getUserById(userId);
            if (user == null) {
                return Result.fail("用户不存在");
            }
            
            UserInfo userInfo = new UserInfo();
            userInfo.setUserId(userId);
            userInfo.setPhone("加密存储，不直接返回");
            userInfo.setNickname(user.getNickname());
            userInfo.setUserType(user.getUserType());
            userInfo.setRegisterTime(user.getRegisterTime().toString());
            
            return Result.success(userInfo);
        } catch (Exception e) {
            log.error("查询用户失败", e);
            return Result.fail("查询用户失败：" + e.getMessage());
        }
    }

    /**
     * 查询商家信息
     */
    @GetMapping("/merchants/{merchantId}")
    public Result<MerchantInfo> getMerchantById(@PathVariable String merchantId) {
        try {
            MerchantExtend merchant = testDataService.getMerchantById(merchantId);
            if (merchant == null) {
                return Result.fail("商家不存在");
            }
            
            MerchantInfo merchantInfo = new MerchantInfo();
            merchantInfo.setMerchantId(merchantId);
            merchantInfo.setUserId(encryptUtils.bytesToUuid(merchant.getUserId()));
            merchantInfo.setMerchantName(merchant.getMerchantName());
            merchantInfo.setMerchantLevel(merchant.getMerchantLevel());
            merchantInfo.setPrivilegeExpireTime(merchant.getPrivilegeExpireTime().toString());
            
            return Result.success(merchantInfo);
        } catch (Exception e) {
            log.error("查询商家失败", e);
            return Result.fail("查询商家失败：" + e.getMessage());
        }
    }

    /**
     * 查询店铺信息
     */
    @GetMapping("/stores/{storeId}")
    public Result<StoreInfo> getStoreById(@PathVariable String storeId) {
        try {
            Store store = testDataService.getStoreById(storeId);
            if (store == null) {
                return Result.fail("店铺不存在");
            }
            
            StoreInfo storeInfo = new StoreInfo();
            storeInfo.setStoreId(storeId);
            storeInfo.setMerchantId(encryptUtils.bytesToUuid(store.getMerchantId()));
            storeInfo.setStoreName(store.getStoreName());
            storeInfo.setAddress(store.getAddress());
            storeInfo.setStatus(store.getStatus());
            
            return Result.success(storeInfo);
        } catch (Exception e) {
            log.error("查询店铺失败", e);
            return Result.fail("查询店铺失败：" + e.getMessage());
        }
    }

    // ==================== 请求DTO ====================
    
    @Data
    public static class CreateUserRequest {
        private String phone;       // 明文手机号
        private String password;    // 明文密码
        private String nickname;    // 昵称
        private Integer userType;   // 用户类型：1-普通用户，2-商家，3-员工
    }

    @Data
    public static class CreateMerchantRequest {
        private String userId;      // 用户ID（UUID字符串）
        private String merchantName;// 商家名称
        private String sndPswd;     // 二级密码（明文）
    }

    @Data
    public static class CreateStoreRequest {
        private String merchantId;  // 商家ID（UUID字符串）
        private String storeName;   // 店铺名称
        private String address;     // 店铺地址
    }

    @Data
    public static class CreateWorkRelationRequest {
        private String storeId;     // 店铺ID（UUID字符串）
        private String userId;      // 用户ID（UUID字符串）
        private String role;        // 角色：manager/employee
        private String name;        // 员工姓名
    }

    // ==================== 响应DTO ====================
    
    @Data
    public static class UserInfo {
        private String userId;
        private String phone;
        private String nickname;
        private Integer userType;
        private String registerTime;
    }

    @Data
    public static class MerchantInfo {
        private String merchantId;
        private String userId;
        private String merchantName;
        private Integer merchantLevel;
        private String privilegeExpireTime;
    }

    @Data
    public static class StoreInfo {
        private String storeId;
        private String merchantId;
        private String storeName;
        private String address;
        private Integer status;
    }
}

