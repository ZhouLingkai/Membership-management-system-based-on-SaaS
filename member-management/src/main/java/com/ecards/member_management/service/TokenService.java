package com.ecards.member_management.service;

import com.ecards.member_management.common.ErrorCode;
import com.ecards.member_management.constants.TokenConstants;
import com.ecards.member_management.entity.MerchantExtend;
import com.ecards.member_management.entity.Store;
import com.ecards.member_management.entity.User;
import com.ecards.member_management.entity.WorkRelation;
import com.ecards.member_management.enums.TokenType;
import com.ecards.member_management.enums.UserRole;
import com.ecards.member_management.exception.BusinessException;
import com.ecards.member_management.repository.MerchantExtendRepository;
import com.ecards.member_management.repository.StoreRepository;
import com.ecards.member_management.repository.UserRepository;
import com.ecards.member_management.repository.WorkRelationRepository;
import com.ecards.member_management.utils.EncryptUtils;
import com.ecards.member_management.utils.JwtUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * 令牌服务
 * 提供令牌的生成、刷新、注销等核心功能
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TokenService {

    private final AuthenticationService authenticationService;
    private final TokenRedisService tokenRedisService;
    private final WorkRelationRepository workRelationRepository;
    private final StoreRepository storeRepository;
    private final MerchantExtendRepository merchantExtendRepository;
    private final UserRepository userRepository;
    private final JwtUtils jwtUtils;
    private final EncryptUtils encryptUtils;

    @Value("${jwt.expiration.normal}")
    private Long normalExpiration;

    @Value("${jwt.expiration.privilege}")
    private Long privilegeExpiration;

    @Value("${jwt.expiration.work}")
    private Long workExpiration;

    @Value("${jwt.expiration.manager}")
    private Long managerExpiration;

    @Value("${jwt.expiration.auto-login}")
    private Long autoLoginExpiration;

    @Value("${jwt.expiration.auto-login-short:28800000}")
    private Long autoLoginShortExpiration;

    @Value("${jwt.manager.max-use-count}")
    private Integer managerMaxUseCount;

    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    // ==================== 生成普通令牌 ====================

    /**
     * 生成普通令牌（验证码登录）
     *
     * @param phone     手机号（明文）
     * @param code      验证码
     * @param platform  平台类型
     * @param deviceId  设备ID
     * @param loginIp   登录IP
     * @return 普通令牌信息（包含自动登录令牌）
     */
    public Map<String, Object> generateNormalTokenByCode(String phone, String code, String platform, 
                                                         String deviceId, String loginIp) {
        try {
            // 验证手机号和验证码（传入明文手机号）
            User user = authenticationService.validatePhoneAndCode(phone, code);
            if (user == null) {
                throw new BusinessException(ErrorCode.VERIFICATION_CODE_INVALID);
            }

            return generateNormalTokenInternal(user, platform, deviceId, loginIp, true, false);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("生成普通令牌失败（验证码登录）", e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "生成令牌失败");
        }
    }

    /**
     * 生成普通令牌（密码登录）
     *
     * @param phone     手机号（明文）
     * @param password  密码（明文）
     * @param platform  平台类型
     * @param deviceId  设备ID
     * @param loginIp   登录IP
     * @return 普通令牌信息（包含自动登录令牌）
     */
    public Map<String, Object> generateNormalTokenByPassword(String phone, String password, String platform,
                                                             String deviceId, String loginIp) {
        try {
            // 验证手机号和密码（传入明文手机号）
            User user = authenticationService.validatePhoneAndPassword(phone, password);
            if (user == null) {
                throw new BusinessException(ErrorCode.PASSWORD_ERROR);
            }

            return generateNormalTokenInternal(user, platform, deviceId, loginIp, true, false);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("生成普通令牌失败（密码登录）", e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "生成令牌失败");
        }
    }

    /**
     * 生成普通令牌（自动登录）
     *
     * @param autoLoginToken 自动登录令牌
     * @param platform       平台类型
     * @param deviceId       设备ID
     * @param loginIp        登录IP
     * @return 普通令牌信息（不包含新的自动登录令牌）
     */
    public Map<String, Object> generateNormalTokenByAutoLogin(String autoLoginToken, String platform,
                                                          String deviceId, String loginIp) {
        try {
            // 解析自动登录令牌
            String userId = jwtUtils.extractUserId(autoLoginToken);
            String tokenDeviceId = jwtUtils.extractDeviceId(autoLoginToken);
            String jti = jwtUtils.extractJti(autoLoginToken);

            // 验证设备ID
            if (!deviceId.equals(tokenDeviceId)) {
                throw new BusinessException(ErrorCode.DEVICE_NOT_MATCH);
            }

            // 验证令牌是否在黑名单中
            if (tokenRedisService.isInBlacklist(jti)) {
                throw new BusinessException(ErrorCode.TOKEN_INVALID);
            }
            
            // 验证Redis中是否存在该自动登录令牌
            // 自动登录令牌的失效由以下两种方式控制：
            // 1. 密码修改时主动删除Redis中的自动登录令牌
            // 2. 主动退出时主动删除Redis中的自动登录令牌
            // 这样可以确保身份改变（如商家审核通过、员工绑定）不影响自动登录功能
            String redisJti = tokenRedisService.getAutoLoginJti(userId, platform, deviceId);
            if (redisJti == null || !redisJti.equals(jti)) {
                throw new BusinessException(ErrorCode.TOKEN_INVALID);
            }

            // ✨ 新增：计算令牌剩余时间（用于轮换）
            long remainingMillis = getRemainingMillis(autoLoginToken);
            long remainingSeconds = remainingMillis / 1000;
            
            // ✨ 新增：递增使用次数（实现"3次使用+令牌轮换"安全机制）
            Long usageCount = tokenRedisService.incrementAutoLoginUsage(
                userId, platform, deviceId, remainingSeconds);
            
            log.info("自动登录令牌使用: userId={}, 当前使用次数={}/3, jti={}", userId, usageCount, jti);
            
            // ✨ 新增：检查使用次数限制
            if (usageCount > 3) {
                // 超过3次，令牌失效
                tokenRedisService.removeAutoLoginToken(userId, platform, deviceId);
                tokenRedisService.resetAutoLoginUsage(userId, platform, deviceId);
                log.warn("自动登录令牌超过使用上限: userId={}, usageCount={}", userId, usageCount);
                throw new BusinessException(ErrorCode.TOKEN_INVALID, 
                    "自动登录令牌已达使用上限，请重新登录");
            }

            // 验证用户
            User user = authenticationService.validateAutoLoginToken(userId, deviceId);
            if (user == null) {
                throw new BusinessException(ErrorCode.USER_NOT_EXIST);
            }

            // 生成普通令牌（不生成新的自动登录令牌）
            Map<String, Object> result = generateNormalTokenInternal(user, platform, deviceId, loginIp, false, true);
            
            // ✨ 新增：设置用户信息（自动登录接口需要返回用户信息）
            result.put("userInfo", buildUserInfo(user));
            
            // ✨ 新增：第3次使用时的处理逻辑
            if (usageCount == 3 && remainingMillis > 0) {
                // 判断是否为8小时令牌（短期令牌）
                boolean isShortToken = remainingMillis <= 28800000L; // 8小时 = 28800000毫秒
                
                if (isShortToken) {
                    // 8小时令牌：第3次使用后直接注销，不轮换
                    tokenRedisService.removeAutoLoginToken(userId, platform, deviceId);
                    tokenRedisService.resetAutoLoginUsage(userId, platform, deviceId);
                    log.info("8小时自动登录令牌达到使用上限，已注销: userId={}", userId);
                    result.put("tokenRotated", false);
                } else {
                    // 7天令牌：轮换机制
                    log.info("7天自动登录令牌达到使用上限，开始令牌轮换: userId={}, remainingTime={}ms", 
                            userId, remainingMillis);
                    
                    // 生成新令牌（继承剩余时间）
                    String newAutoJti = UUID.randomUUID().toString();
                    String newAutoLoginToken = jwtUtils.generateAutoLoginToken(
                        userId, platform, deviceId, loginIp, 
                        user.getTokenVersion(), newAutoJti, remainingMillis);
                    Date newAutoExpireTime = new Date(System.currentTimeMillis() + remainingMillis);
                    
                    // 删除旧令牌记录，保存新令牌
                    tokenRedisService.removeAutoLoginToken(userId, platform, deviceId);
                    tokenRedisService.saveAutoLoginToken(userId, platform, deviceId, 
                        newAutoJti, remainingSeconds);
                    
                    // 重置使用次数
                    tokenRedisService.resetAutoLoginUsage(userId, platform, deviceId);
                    
                    // ✨ 返回新的自动登录令牌
                    result.put("newAutoLoginToken", TokenConstants.BEARER_PREFIX + newAutoLoginToken);
                    result.put("newAutoExpireTime", formatDateTime(newAutoExpireTime));
                    result.put("tokenRotated", true);
                    
                    log.info("7天自动登录令牌轮换成功: userId={}, newJti={}, remainingTime={}ms", 
                            userId, newAutoJti, remainingMillis);
                }
            } else {
                // 未达到轮换条件
                result.put("tokenRotated", false);
            }
            
            return result;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("生成普通令牌失败（自动登录）", e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "生成令牌失败");
        }
    }

    /**
     * 内部方法：生成普通令牌
     */
    private Map<String, Object> generateNormalTokenInternal(User user, String platform, String deviceId,
                                                            String loginIp, boolean generateAutoLogin, boolean isAutoLogin) {
        String userId = encryptUtils.bytesToUuid(user.getUserId());
        String role = UserRole.fromUserType(user.getUserType()).getCode();
        Integer tokenVersion = user.getTokenVersion();

        // ⭐ 容错机制：清除Redis缓存，确保令牌版本校验时从数据库重新读取
        // 这可以防止缓存不一致导致用户无法登录的问题
        // 例如：角色调整等操作可能更新了数据库但Redis缓存未同步
        try {
            tokenRedisService.clearTokenVersionCache(userId);
            log.info("登录时清除令牌版本缓存，确保获取最新版本: userId={}", userId);
        } catch (Exception e) {
            log.warn("清除令牌版本缓存失败，继续执行: userId={}", userId, e);
        }

        // 获取商家ID（如果是商家）
        String merchantId = null;
        if (user.getUserType() == 2) {
            MerchantExtend merchant = authenticationService.getMerchantByUserId(userId);
            if (merchant != null) {
                merchantId = encryptUtils.bytesToUuid(merchant.getMerchantId());
                
                // ✅ 商户状态校验逻辑（自动登录时跳过，避免自动登录失败）
                if (!isAutoLogin) {
                    Integer certification = merchant.getCertification();
                    if (certification == 2) {
                        // 未认证测试中，校验是否超过7天
                        LocalDateTime createTime = merchant.getCreateTime();
                        LocalDateTime expireTime = createTime.plusDays(7);
                        if (LocalDateTime.now().isAfter(expireTime)) {
                            // 超过7天，更新状态为5（测试期过）
                            merchant.setCertification(5);
                            authenticationService.saveMerchant(merchant);
                            log.warn("商户测试期已过，状态已更新: merchantId={}", merchantId);
                            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "商户测试期已过，请补充资质认证或联系客服");
                        }
                    } else if (certification == 5) {
                        // 测试期已过，功能已锁定
                        throw new BusinessException(ErrorCode.SYSTEM_ERROR, "商户测试期已过，功能已锁定，请重新申请或联系客服");
                    }
                } else {
                    log.debug("自动登录场景，跳过商家状态校验: merchantId={}", merchantId);
                }
            }
        }

        // 生成普通令牌
        String jti = UUID.randomUUID().toString();
        String token = jwtUtils.generateNormalToken(userId, role, merchantId, deviceId, loginIp, 
                                                    tokenVersion, jti, normalExpiration);
        Date expireTime = new Date(System.currentTimeMillis() + normalExpiration);

        Map<String, Object> result = new HashMap<>();
        result.put("token", TokenConstants.BEARER_PREFIX + token);
        result.put("expireTime", formatDateTime(expireTime));
        result.put("jti", jti);
        result.put("userRole", role);

        // 生成自动登录令牌
        if (generateAutoLogin) {
            String autoJti = UUID.randomUUID().toString();
            String autoLoginToken = jwtUtils.generateAutoLoginToken(userId, platform, deviceId, loginIp, 
                                                                    tokenVersion, autoJti, autoLoginExpiration);
            Date autoExpireTime = new Date(System.currentTimeMillis() + autoLoginExpiration);

            // 保存到Redis
            tokenRedisService.saveAutoLoginToken(userId, platform, deviceId, autoJti, autoLoginExpiration / 1000);

            result.put("autoLoginToken", TokenConstants.BEARER_PREFIX + autoLoginToken);
            result.put("autoExpireTime", formatDateTime(autoExpireTime));
        }

        log.info("生成普通令牌成功: userId={}, jti={}", userId, jti);
        return result;
    }

    // ==================== 生成特权令牌 ====================

    /**
     * 生成特权令牌
     *
     * @param normalToken    普通令牌（用于提取用户信息）
     * @param permissions    权限列表
     * @param deviceId       设备ID
     * @return 特权令牌信息
     */
    public Map<String, Object> generatePrivilegeToken(String normalToken, List<String> permissions, String deviceId) {
        try {
            // 验证普通令牌
            validateNormalToken(normalToken, deviceId);

            // 提取用户信息
            String userId = jwtUtils.extractUserId(normalToken);
            String role = jwtUtils.extractRole(normalToken);
            
            // 提取令牌版本号
            io.jsonwebtoken.Claims claims = jwtUtils.parseToken(normalToken);
            Integer tokenVersion = claims.get("token_version", Integer.class);

            // 生成特权令牌
            String jti = UUID.randomUUID().toString();
            String token = jwtUtils.generatePrivilegeToken(userId, role, permissions, deviceId, 
                                                          tokenVersion, jti, privilegeExpiration);
            Date expireTime = new Date(System.currentTimeMillis() + privilegeExpiration);

            Map<String, Object> result = new HashMap<>();
            result.put("token", TokenConstants.BEARER_PREFIX + token);
            result.put("expireTime", formatDateTime(expireTime));
            result.put("jti", jti);
            result.put("singleUse", true);

            log.info("生成特权令牌成功: userId={}, jti={}, permissions={}", userId, jti, permissions);
            return result;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("生成特权令牌失败", e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "生成特权令牌失败");
        }
    }

    // ==================== 生成工作令牌 ====================

    /**
     * 生成工作令牌
     * 
     * 修改说明（2025-10-30）：
     * - 权限列表自动从数据库 t_work_relation 读取，前端无需传入
     * - 令牌版本号从数据库读取，确保版本一致性
     * - merchant_id 从冗余字段读取，减少关联查询
     *
     * @param normalToken    普通令牌
     * @param storeId        店铺ID
     * @param deviceId       设备ID
     * @param loginIp        登录IP
     * @return 工作令牌信息
     */
    public Map<String, Object> generateWorkToken(String normalToken, String storeId,
                                                 String deviceId, String loginIp) {
        try {
            // 验证普通令牌
            validateNormalToken(normalToken, deviceId);

            // 提取用户信息
            String userId = jwtUtils.extractUserId(normalToken);
            String role = jwtUtils.extractRole(normalToken);
            
            byte[] userIdBytes = encryptUtils.uuidToBytes(userId);
            byte[] storeIdBytes = encryptUtils.uuidToBytes(storeId);
            
            String merchantId;
            String workRole;
            Integer tokenVersion;
            List<String> permissions;
            
            // 区分商家和员工
            if ("MERCHANT".equals(role)) {
                // 商家获取工作令牌：验证店铺归属
                Store store = storeRepository.findById(storeIdBytes)
                        .orElseThrow(() -> new BusinessException(ErrorCode.STORE_NOT_EXIST));
                
                // 获取商家信息
                MerchantExtend merchantExtend = merchantExtendRepository.findByUserId(userIdBytes)
                        .orElseThrow(() -> new BusinessException(ErrorCode.MERCHANT_NOT_EXIST));
                
                merchantId = encryptUtils.bytesToUuid(merchantExtend.getMerchantId());
                
                // 验证店铺归属
                if (!merchantId.equals(encryptUtils.bytesToUuid(store.getMerchantId()))) {
                    throw new BusinessException(ErrorCode.PERMISSION_DENIED.getCode(), "店铺不属于当前商家");
                }
                
                workRole = "merchant";  // 商家工作令牌角色
                User user = userRepository.findById(userIdBytes)
                        .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_EXIST));
                tokenVersion = user.getTokenVersion();  // 使用用户的 tokenVersion
                permissions = Collections.emptyList();  // 商家拥有全部权限，无需显式列出
                
                log.info("商家获取工作令牌: userId={}, merchantId={}, storeId={}", userId, merchantId, storeId);
                
            } else {
                // 员工/店长获取工作令牌：查询工作关系
                WorkRelation workRelation = workRelationRepository
                        .findByStoreIdAndUserIdAndStatus(storeIdBytes, userIdBytes, 1)
                        .orElseThrow(() -> new BusinessException(50002, "你已不是本店员工"));

                // 从数据库读取关键信息
                workRole = workRelation.getRole();  // "manager" 或 "employee"
                tokenVersion = workRelation.getTokenVersion();
                merchantId = encryptUtils.bytesToUuid(workRelation.getMerchantId());
                
                // 从 permission JSON 字段解析权限列表
                permissions = parsePermissionsFromJson(workRelation.getPermission(), workRole);
                
                log.info("员工获取工作令牌: userId={}, role={}, storeId={}", userId, workRole, storeId);
            }

            // 生成工作令牌
            String jti = UUID.randomUUID().toString();
            String token = jwtUtils.generateWorkToken(userId, workRole, merchantId, storeId, permissions,
                                                     deviceId, loginIp, tokenVersion, jti, workExpiration);
            Date expireTime = new Date(System.currentTimeMillis() + workExpiration);

            Map<String, Object> result = new HashMap<>();
            result.put("token", TokenConstants.BEARER_PREFIX + token);
            result.put("expireTime", formatDateTime(expireTime));
            result.put("jti", jti);
            result.put("storeId", storeId);
            result.put("merchantId", merchantId);
            result.put("role", workRole);
            result.put("permissions", permissions);
            result.put("tokenVersion", tokenVersion);

            log.info("生成工作令牌成功: userId={}, storeId={}, role={}, tokenVersion={}, jti={}", 
                    userId, storeId, workRole, tokenVersion, jti);
            return result;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("生成工作令牌失败", e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "生成工作令牌失败");
        }
    }

    // ==================== 生成管理令牌 ====================

    /**
     * 生成管理令牌
     *
     * @param normalToken    普通令牌
     * @param merchantId     商家ID
     * @param sndPswd        二级密码
     * @param storeId        店铺ID（可选）
     * @param deviceId       设备ID
     * @param loginIp        登录IP
     * @return 管理令牌信息
     */
    public Map<String, Object> generateManagerToken(String normalToken, String merchantId, String sndPswd,
                                                    String storeId, String deviceId, String loginIp) {
        try {
            // 验证普通令牌
            validateNormalToken(normalToken, deviceId);

            // 提取用户信息
            String userId = jwtUtils.extractUserId(normalToken);
            String role = jwtUtils.extractRole(normalToken);
            
            // 提取令牌版本号
            io.jsonwebtoken.Claims claims = jwtUtils.parseToken(normalToken);
            Integer tokenVersion = claims.get("token_version", Integer.class);

            // 验证用户是否为商家
            if (!UserRole.MERCHANT.getCode().equals(role)) {
                throw new BusinessException(ErrorCode.TOKEN_PERMISSION_DENIED, "只有商家可以获取管理令牌");
            }

            // 验证二级密码
            if (!authenticationService.validateSecondaryPassword(merchantId, sndPswd)) {
                throw new BusinessException(ErrorCode.SECONDARY_PASSWORD_ERROR);
            }

            // 生成管理令牌
            String jti = UUID.randomUUID().toString();
            String token = jwtUtils.generateManagerToken(userId, role, merchantId, storeId, deviceId, loginIp,
                                                        tokenVersion, jti, managerExpiration);
            Date expireTime = new Date(System.currentTimeMillis() + managerExpiration);

            Map<String, Object> result = new HashMap<>();
            result.put("token", TokenConstants.BEARER_PREFIX + token);
            result.put("expireTime", formatDateTime(expireTime));
            result.put("jti", jti);
            result.put("maxUseCount", managerMaxUseCount);
            result.put("usedCount", 0);

            log.info("生成管理令牌成功: userId={}, merchantId={}, jti={}", userId, merchantId, jti);
            return result;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("生成管理令牌失败", e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "生成管理令牌失败");
        }
    }

    // ==================== 刷新令牌 ====================

    /**
     * 刷新令牌（支持普通令牌和工作令牌）
     *
     * @param oldToken  旧令牌
     * @param tokenType 令牌类型（1-普通，3-工作）
     * @param deviceId  设备ID
     * @param loginIp   登录IP
     * @return 新令牌信息
     */
    public Map<String, Object> refreshToken(String oldToken, Integer tokenType, String deviceId, String loginIp) {
        try {
            // 验证旧令牌基本有效性
            if (!jwtUtils.validateToken(oldToken)) {
                throw new BusinessException(ErrorCode.TOKEN_INVALID);
            }

            // 提取信息
            String userId = jwtUtils.extractUserId(oldToken);
            String role = jwtUtils.extractRole(oldToken);
            String oldJti = jwtUtils.extractJti(oldToken);
            String tokenDeviceId = jwtUtils.extractDeviceId(oldToken);
            Integer actualTokenType = jwtUtils.extractTokenType(oldToken);
            
            // 提取令牌版本号
            io.jsonwebtoken.Claims claims = jwtUtils.parseToken(oldToken);
            Integer tokenVersion = claims.get("token_version", Integer.class);

            // 验证设备ID
            if (!deviceId.equals(tokenDeviceId)) {
                throw new BusinessException(ErrorCode.DEVICE_NOT_MATCH);
            }

            // 验证令牌类型
            if (!tokenType.equals(actualTokenType)) {
                throw new BusinessException(ErrorCode.PARAM_ERROR, "令牌类型不匹配");
            }

            // 验证黑名单
            if (tokenRedisService.isInBlacklist(oldJti)) {
                throw new BusinessException(ErrorCode.TOKEN_INVALID);
            }

            // 将旧令牌加入黑名单
            Long remainingTime = jwtUtils.getRemainingTime(oldToken);
            tokenRedisService.addToBlacklist(oldJti, remainingTime / 1000);

            // 生成新令牌
            String newJti = UUID.randomUUID().toString();
            String newToken;
            Long expiration;

            if (TokenType.NORMAL.getCode().equals(tokenType)) {
                // 刷新普通令牌
                String merchantId = jwtUtils.extractMerchantId(oldToken);
                newToken = jwtUtils.generateNormalToken(userId, role, merchantId, deviceId, loginIp, 
                                                       tokenVersion, newJti, normalExpiration);
                expiration = normalExpiration;
            } else if (TokenType.WORK.getCode().equals(tokenType)) {
                // 刷新工作令牌（从数据库重新读取权限和版本号，确保数据一致性）
                String storeId = jwtUtils.extractStoreId(oldToken);
                
                // 查询工作关系，获取最新的权限、版本号、merchant_id
                byte[] userIdBytes = encryptUtils.uuidToBytes(userId);
                byte[] storeIdBytes = encryptUtils.uuidToBytes(storeId);
                
                WorkRelation workRelation = workRelationRepository
                        .findByStoreIdAndUserIdAndStatus(storeIdBytes, userIdBytes, 1)
                        .orElseThrow(() -> new BusinessException(50002, "你已不是本店员工"));
                
                // 从数据库读取最新信息
                String dbRole = workRelation.getRole();
                Integer dbTokenVersion = workRelation.getTokenVersion();
                String dbMerchantId = encryptUtils.bytesToUuid(workRelation.getMerchantId());
                List<String> dbPermissions = parsePermissionsFromJson(workRelation.getPermission(), dbRole);
                
                // 检查版本号是否一致
                if (!tokenVersion.equals(dbTokenVersion)) {
                    throw new BusinessException(10007, "工作令牌版本过期，请重新申请");
                }
                
                newToken = jwtUtils.generateWorkToken(userId, dbRole, dbMerchantId, storeId, dbPermissions,
                                                     deviceId, loginIp, dbTokenVersion, newJti, workExpiration);
                expiration = workExpiration;
            } else {
                throw new BusinessException(ErrorCode.PARAM_ERROR, "不支持刷新的令牌类型");
            }

            Date newExpireTime = new Date(System.currentTimeMillis() + expiration);

            Map<String, Object> result = new HashMap<>();
            result.put("newToken", TokenConstants.BEARER_PREFIX + newToken);
            result.put("newExpireTime", formatDateTime(newExpireTime));
            result.put("oldJti", oldJti);
            result.put("newJti", newJti);

            log.info("刷新令牌成功: userId={}, oldJti={}, newJti={}", userId, oldJti, newJti);
            return result;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("刷新令牌失败", e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "刷新令牌失败");
        }
    }

    // ==================== 注销令牌 ====================

    /**
     * 注销令牌
     *
     * @param token    令牌
     * @param deviceId 设备ID
     */
    public void revokeToken(String token, String deviceId) {
        try {
            // 验证令牌基本有效性
            if (!jwtUtils.validateToken(token)) {
                throw new BusinessException(ErrorCode.TOKEN_INVALID);
            }

            // 提取信息
            String jti = jwtUtils.extractJti(token);
            String tokenDeviceId = jwtUtils.extractDeviceId(token);
            Integer tokenType = jwtUtils.extractTokenType(token);

            // 验证设备ID
            if (!deviceId.equals(tokenDeviceId)) {
                throw new BusinessException(ErrorCode.DEVICE_NOT_MATCH);
            }

            // 检查是否已在黑名单
            if (tokenRedisService.isInBlacklist(jti)) {
                log.warn("令牌已在黑名单中: jti={}", jti);
                return;
            }

            // 加入黑名单
            Long remainingTime = jwtUtils.getRemainingTime(token);
            tokenRedisService.addToBlacklist(jti, remainingTime / 1000);

            // 如果是自动登录令牌，同时移除Redis中的记录
            if (TokenType.AUTO_LOGIN.getCode().equals(tokenType)) {
                String userId = jwtUtils.extractUserId(token);
                String platform = jwtUtils.extractTokenType(token).toString(); // 简化处理
                tokenRedisService.removeAutoLoginToken(userId, platform, deviceId);
            }

            log.info("注销令牌成功: jti={}, tokenType={}", jti, tokenType);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("注销令牌失败", e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "注销令牌失败");
        }
    }

    // ==================== 辅助方法 ====================

    /**
     * 验证普通令牌的有效性
     */
    private void validateNormalToken(String token, String deviceId) {
        if (!jwtUtils.validateToken(token)) {
            throw new BusinessException(ErrorCode.TOKEN_INVALID);
        }

        String jti = jwtUtils.extractJti(token);
        String tokenDeviceId = jwtUtils.extractDeviceId(token);
        Integer tokenType = jwtUtils.extractTokenType(token);

        // 验证设备ID
        if (!deviceId.equals(tokenDeviceId)) {
            throw new BusinessException(ErrorCode.DEVICE_NOT_MATCH);
        }

        // 验证令牌类型
        if (!TokenType.NORMAL.getCode().equals(tokenType)) {
            throw new BusinessException(ErrorCode.TOKEN_INVALID, "需要普通令牌");
        }

        // 验证黑名单
        if (tokenRedisService.isInBlacklist(jti)) {
            throw new BusinessException(ErrorCode.TOKEN_INVALID);
        }
    }

    /**
     * 计算令牌剩余有效时间（毫秒）
     * 
     * @param token JWT令牌
     * @return 剩余时间（毫秒），若已过期或无法解析则返回0
     */
    private long getRemainingMillis(String token) {
        try {
            Long remaining = jwtUtils.getRemainingTime(token);
            return remaining != null && remaining > 0 ? remaining : 0;
        } catch (Exception e) {
            log.warn("无法获取令牌剩余时间", e);
            return 0;
        }
    }

    /**
     * 格式化时间
     */
    private String formatDateTime(Date date) {
        LocalDateTime localDateTime = date.toInstant().atZone(ZoneId.of("Asia/Shanghai")).toLocalDateTime();
        return localDateTime.format(DATETIME_FORMATTER);
    }

    /**
     * 从JSON权限字段解析权限列表
     * 
     * @param permissionJson 权限JSON字符串
     *                       示例：{"manager": ["staff_add"], "employee": ["member_card_create", "transaction_recharge"]}
     * @param role 角色：manager 或 employee
     * @return 权限列表
     */
    private List<String> parsePermissionsFromJson(String permissionJson, String role) {
        try {
            if (permissionJson == null || permissionJson.trim().isEmpty()) {
                log.warn("权限JSON为空，返回空权限列表");
                return Collections.emptyList();
            }
            
            // 使用简单的JSON解析（避免引入重量级库）
            // 根据角色提取对应的权限数组
            String key = "manager".equals(role) ? "\"manager\"" : "\"employee\"";
            
            int keyIndex = permissionJson.indexOf(key);
            if (keyIndex == -1) {
                log.warn("权限JSON中未找到角色: {}, permissionJson: {}", role, permissionJson);
                return Collections.emptyList();
            }
            
            // 找到权限数组的起始位置
            int arrayStart = permissionJson.indexOf("[", keyIndex);
            int arrayEnd = permissionJson.indexOf("]", arrayStart);
            
            if (arrayStart == -1 || arrayEnd == -1) {
                log.warn("权限JSON格式错误，未找到数组: {}", permissionJson);
                return Collections.emptyList();
            }
            
            // 提取数组内容
            String arrayContent = permissionJson.substring(arrayStart + 1, arrayEnd);
            
            if (arrayContent.trim().isEmpty()) {
                return Collections.emptyList();
            }
            
            // 解析权限列表
            List<String> permissions = new ArrayList<>();
            String[] items = arrayContent.split(",");
            
            for (String item : items) {
                String permission = item.trim()
                        .replace("\"", "")
                        .replace("'", "");
                if (!permission.isEmpty()) {
                    permissions.add(permission);
                }
            }
            
            log.debug("解析权限成功: role={}, permissions={}", role, permissions);
            return permissions;
            
        } catch (Exception e) {
            log.error("解析权限JSON失败: permissionJson={}, role={}", permissionJson, role, e);
            return Collections.emptyList();
        }
    }

    /**
     * 构建用户信息
     */
    private Map<String, Object> buildUserInfo(User user) {
        Map<String, Object> userInfo = new HashMap<>();
        userInfo.put("userId", encryptUtils.bytesToUuid(user.getUserId()));
        userInfo.put("nickname", user.getNickname());
        userInfo.put("userType", user.getUserType());
        userInfo.put("inviteCode", user.getInviteCode());
        userInfo.put("avatar", user.getAvatar() != null ? user.getAvatar() : "");
        return userInfo;
    }
}

