package com.ecards.member_management.service;

import com.ecards.member_management.common.ErrorCode;
import com.ecards.member_management.dto.response.ActivateBatchResponse;
import com.ecards.member_management.entity.MerchantExtend;
import com.ecards.member_management.entity.User;
import com.ecards.member_management.exception.BusinessException;
import com.ecards.member_management.repository.MerchantExtendRepository;
import com.ecards.member_management.repository.UserRepository;
import com.ecards.member_management.utils.EncryptUtils;
import com.ecards.member_management.utils.JwtUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * 用户服务
 * 提供用户注册、登录等核心功能
 */
@Slf4j
@Service
public class UserService {

    private final UserRepository userRepository;
    private final VerifyCodeService verifyCodeService;
    private final TokenRedisService tokenRedisService;
    private final JwtUtils jwtUtils;
    private final EncryptUtils encryptUtils;
    private final MerchantExtendRepository merchantExtendRepository;
    private final MemberCardService memberCardService;

    public UserService(UserRepository userRepository,
                       VerifyCodeService verifyCodeService,
                       TokenRedisService tokenRedisService,
                       JwtUtils jwtUtils,
                       EncryptUtils encryptUtils,
                       MerchantExtendRepository merchantExtendRepository,
                       @Lazy MemberCardService memberCardService) {
        this.userRepository = userRepository;
        this.verifyCodeService = verifyCodeService;
        this.tokenRedisService = tokenRedisService;
        this.jwtUtils = jwtUtils;
        this.encryptUtils = encryptUtils;
        this.merchantExtendRepository = merchantExtendRepository;
        this.memberCardService = memberCardService;
    }

    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final int INVITE_CODE_LENGTH = 6;

    /**
     * 用户注册
     *
     * @param encryptedPhone 加密后的手机号
     * @param password       明文密码
     * @param verifyCode     验证码
     * @param nickname       昵称
     * @param invitedCode    邀请码（可选）
     * @param rememberMe     是否记住登录
     * @param platform       平台类型
     * @param deviceId       设备ID
     * @param loginIp        登录IP
     * @return 注册结果（包含令牌）
     */
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> register(String encryptedPhone, String password, String verifyCode,
                                        String nickname, String invitedCode, Boolean rememberMe,
                                        String platform, String deviceId, String loginIp) {
        try {
            // 1. 验证验证码（不立即删除，防止后续校验失败导致验证码失效）
            if (!verifyCodeService.verifyCode(encryptedPhone, verifyCode, deviceId, false)) {
                throw new BusinessException(ErrorCode.VERIFICATION_CODE_INVALID);
            }

            // 2. 解密手机号（用于数据库查询和存储）
            String plainPhone = encryptUtils.decryptAES(encryptedPhone);
            if (plainPhone == null || plainPhone.isEmpty()) {
                log.error("手机号解密失败: encryptedPhone={}", encryptedPhone.substring(0, Math.min(10, encryptedPhone.length())));
                throw new BusinessException(ErrorCode.PARAM_ERROR, "手机号格式错误");
            }

            // 3. 检查手机号是否已注册（使用明文查询）
            if (userRepository.findByPhone(plainPhone).isPresent()) {
                throw new BusinessException(ErrorCode.PHONE_ALREADY_REGISTERED);
            }

            // 4. 验证邀请码（如果填写）
            if (invitedCode != null && !invitedCode.trim().isEmpty()) {
                Optional<User> inviter = userRepository.findByInviteCode(invitedCode);
                if (inviter.isEmpty()) {
                    throw new BusinessException(ErrorCode.PARAM_ERROR, "邀请码无效");
                }
            }

            // 5. 加密密码
            String hashedPassword = encryptUtils.encryptPassword(password);

            // 6. 生成用户ID和邀请码
            UUID userId = UUID.randomUUID();
            byte[] userIdBytes = encryptUtils.uuidToBytes(userId.toString());
            String userInviteCode = generateUniqueInviteCode();

            // 7. 创建用户实体
            User user = new User();
            user.setUserId(userIdBytes);
            user.setPhone(plainPhone); // ✅ 存储明文手机号
            user.setPassword(hashedPassword);
            user.setNickname(nickname);
            user.setUserType(1); // 普通用户
            user.setInviteCode(userInviteCode);
            user.setInvitedCode(invitedCode != null && !invitedCode.trim().isEmpty() ? invitedCode : null);
            
            // 设置时间字段（统一使用同一个时间点）
            LocalDateTime now = LocalDateTime.now(ZoneId.of("Asia/Shanghai"));
            user.setLastLoginTime(now);
            user.setRegisterTime(now);
            user.setUpdateTime(now);

            // 8. 保存用户
            userRepository.save(user);
            log.info("用户注册成功: userId={}, phone={}", userId, plainPhone);

            // 9. 激活通过该手机号办理的未激活会员卡
            ActivateBatchResponse activateResult = null;
            try {
                activateResult = memberCardService.activateBatch(userId.toString(), plainPhone);
                if (activateResult.getActivatedCount() > 0) {
                    log.info("注册时自动激活会员卡: userId={}, activatedCount={}", 
                            userId, activateResult.getActivatedCount());
                }
            } catch (Exception e) {
                log.warn("注册时激活会员卡失败，不影响注册流程: userId={}, error={}", userId, e.getMessage());
            }

            // 10. 生成普通令牌（自动登录）
            Map<String, Object> tokenResult = generateTokenForUser(
                    user,
                    platform,
                    deviceId,
                    loginIp,
                    true, // 始终生成自动登录令牌
                    rememberMe
            );

            // 10. 构建返回结果
            Map<String, Object> result = new HashMap<>();
            result.put("userId", userId.toString());
            result.put("userType", 1);
            result.put("registerTime", user.getRegisterTime().format(DATETIME_FORMATTER));
            result.put("normalToken", tokenResult.get("token"));
            result.put("tokenExpireTime", tokenResult.get("expireTime"));
            result.put("userInfo", buildUserInfo(user));
            
            // 添加自动登录令牌（如果有）
            if (tokenResult.containsKey("autoLoginToken")) {
                result.put("autoLoginToken", tokenResult.get("autoLoginToken"));
                result.put("autoExpireTime", tokenResult.get("autoExpireTime"));
            }

            // 添加会员卡激活结果
            if (activateResult != null) {
                result.put("activatedCount", activateResult.getActivatedCount());
                result.put("activatedCards", activateResult.getActivatedCards());
            } else {
                result.put("activatedCount", 0);
                result.put("activatedCards", List.of());
            }

            // 11. 注册成功，手动删除验证码
            verifyCodeService.deleteCode(encryptedPhone);

            return result;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("用户注册失败", e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "注册失败");
        }
    }

    /**
     * 用户登录（密码登录）
     *
     * @param encryptedPhone 加密后的手机号
     * @param password       明文密码
     * @param rememberMe     是否记住登录
     * @param platform       平台类型
     * @param deviceId       设备ID
     * @param loginIp        登录IP
     * @return 登录结果（包含令牌）
     */
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> login(String encryptedPhone, String password, Boolean rememberMe,
                                     String platform, String deviceId, String loginIp) {
        try {
            // 1. 解密手机号（用于数据库查询）
            String plainPhone = encryptUtils.decryptAES(encryptedPhone);
            if (plainPhone == null || plainPhone.isEmpty()) {
                log.error("登录失败: 手机号解密失败");
                throw new BusinessException(ErrorCode.PARAM_ERROR, "手机号格式错误");
            }

            // 1.1 检查是否被锁定
            tokenRedisService.checkLoginLock(plainPhone);

            // 2. 查询用户（使用明文查询）
            User user = userRepository.findByPhone(plainPhone)
                    .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_EXIST));

            // 3. 验证密码
            if (!encryptUtils.verifyPassword(password, user.getPassword())) {
                // 记录失败并获取剩余次数
                int remaining = tokenRedisService.recordLoginFailure(plainPhone);
                throw new BusinessException(ErrorCode.PASSWORD_ERROR, "密码错误，还有" + remaining + "次机会");
            }

            // 登录成功，清除失败记录
            tokenRedisService.clearLoginFailure(plainPhone);

            // 4. 更新最后登录时间
            LocalDateTime now = LocalDateTime.now(ZoneId.of("Asia/Shanghai"));
            user.setLastLoginTime(now);
            
            // 异步保存到数据库
            CompletableFuture.runAsync(() -> {
                try {
                    userRepository.save(user);
                } catch (Exception e) {
                    log.error("异步更新最后登录时间失败: userId={}", encryptUtils.bytesToUuid(user.getUserId()), e);
                }
            });

            // 5. 生成普通令牌（始终生成自动登录令牌，时长根据rememberMe决定）
            Map<String, Object> tokenResult = generateTokenForUser(
                    user,
                    platform,
                    deviceId,
                    loginIp,
                    true, // 始终生成自动登录令牌
                    rememberMe
            );

            // 6. 构建返回结果
            Map<String, Object> result = new HashMap<>();
            result.put("normalToken", tokenResult.get("token"));
            result.put("tokenExpireTime", tokenResult.get("expireTime"));
            result.put("userInfo", buildUserInfo(user));

            // 添加自动登录令牌（如果有）
            if (tokenResult.containsKey("autoLoginToken")) {
                result.put("autoLoginToken", tokenResult.get("autoLoginToken"));
                result.put("autoExpireTime", tokenResult.get("autoExpireTime"));
            }

            // log.info("用户登录成功: userId={}", encryptUtils.bytesToUuid(user.getUserId()));
            return result;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("用户登录失败", e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "登录失败");
        }
    }

    /**
     * 为用户生成令牌
     *
     * @param user             用户实体
     * @param platform         平台类型
     * @param deviceId         设备ID
     * @param loginIp          登录IP
     * @param generateAutoLogin 是否生成自动登录令牌
     * @param rememberMe       是否记住登录（true=7天，false=8小时）
     * @return 令牌信息
     */
    private Map<String, Object> generateTokenForUser(User user, String platform, String deviceId,
                                                     String loginIp, boolean generateAutoLogin, Boolean rememberMe) {
        String userId = encryptUtils.bytesToUuid(user.getUserId());
        
        // 根据userType动态设置role
        String role;
        switch (user.getUserType()) {
            case 2:
                role = "MERCHANT";
                break;
            case 3:
                role = "STAFF";
                break;
            default:
                role = "NORMAL_USER";
        }
        
        Integer tokenVersion = user.getTokenVersion();

        // 获取商家ID（如果是商家）
        String merchantId = null;
        if (user.getUserType() == 2) {
            byte[] userIdBytes = encryptUtils.uuidToBytes(userId);
            Optional<MerchantExtend> merchantExtend = merchantExtendRepository.findByUserId(userIdBytes);
            if (merchantExtend.isPresent()) {
                merchantId = encryptUtils.bytesToUuid(merchantExtend.get().getMerchantId());
                // log.info("商家登录，已设置merchantId: userId={}, merchantId={}", userId, merchantId);
            } else {
                log.warn("商家用户未找到商家信息: userId={}", userId);
            }
        }

        // 生成普通令牌
        String jti = UUID.randomUUID().toString();
        String token = jwtUtils.generateNormalToken(
                userId, role, merchantId, deviceId, loginIp, tokenVersion, jti, 7200000L);
        Date expireTime = new Date(System.currentTimeMillis() + 7200000L);

        Map<String, Object> result = new HashMap<>();
        result.put("token", "Bearer " + token);
        result.put("expireTime", formatDateTime(expireTime));
        result.put("jti", jti);
        result.put("userRole", role);

        // 生成自动登录令牌
        if (generateAutoLogin) {
            // 根据rememberMe选择令牌时长
            long autoLoginDuration;
            if (Boolean.TRUE.equals(rememberMe)) {
                // 7天
                autoLoginDuration = 604800000L; // 7 * 24 * 60 * 60 * 1000
                // log.info("生成7天自动登录令牌: userId={}", userId);
            } else {
                // 8小时
                autoLoginDuration = 28800000L; // 8 * 60 * 60 * 1000
                // log.info("生成8小时自动登录令牌: userId={}", userId);
            }

            String autoJti = UUID.randomUUID().toString();
            String autoLoginToken = jwtUtils.generateAutoLoginToken(
                    userId, platform, deviceId, loginIp, tokenVersion, autoJti, autoLoginDuration);
            Date autoExpireTime = new Date(System.currentTimeMillis() + autoLoginDuration);

            // 保存到Redis（时长转换为秒）
            tokenRedisService.saveAutoLoginToken(
                    userId, platform, deviceId, autoJti, autoLoginDuration / 1000);

            result.put("autoLoginToken", "Bearer " + autoLoginToken);
            result.put("autoExpireTime", formatDateTime(autoExpireTime));
        }

        return result;
    }

    /**
     * 格式化时间
     */
    private String formatDateTime(Date date) {
        LocalDateTime localDateTime = date.toInstant().atZone(ZoneId.of("Asia/Shanghai")).toLocalDateTime();
        return localDateTime.format(DATETIME_FORMATTER);
    }

    /**
     * 生成唯一邀请码
     *
     * @return 6位随机字符串（字母+数字）
     */
    private String generateUniqueInviteCode() {
        String characters = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"; // 去除易混淆字符
        Random random = new Random();
        int maxAttempts = 10;

        for (int attempt = 0; attempt < maxAttempts; attempt++) {
            StringBuilder code = new StringBuilder();
            for (int i = 0; i < INVITE_CODE_LENGTH; i++) {
                code.append(characters.charAt(random.nextInt(characters.length())));
            }

            String inviteCode = code.toString();
            
            // 检查唯一性
            if (userRepository.findByInviteCode(inviteCode).isEmpty()) {
                return inviteCode;
            }
        }

        // 如果10次都重复，使用UUID的一部分
        return UUID.randomUUID().toString().substring(0, INVITE_CODE_LENGTH).toUpperCase();
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

    /**
     * 查询用户信息
     *
     * @param userId 用户ID（来自令牌）
     * @return 用户信息
     */
    public Map<String, Object> getUserInfo(String userId) {
        log.info("查询用户信息: userId={}", userId);

        // 查询用户
        User user = userRepository.findById(encryptUtils.uuidToBytes(userId))
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_EXIST));

        // 构建响应
        Map<String, Object> result = new HashMap<>();
        result.put("userId", userId);
        result.put("phone", encryptUtils.encryptAES(user.getPhone())); // 返回AES-256-CBC加密后的手机号
        result.put("nickname", user.getNickname());
        result.put("avatar", user.getAvatar());
        result.put("memberAvatar", user.getMemberAvatar());
        result.put("userType", user.getUserType());
        result.put("inviteCode", user.getInviteCode());
        result.put("invitedCode", user.getInvitedCode());
        result.put("registerTime", user.getRegisterTime().format(DATETIME_FORMATTER));

        return result;
    }

    /**
     * 修改用户信息
     *
     * @param userId        用户ID（来自令牌）
     * @param requestUserId 请求中的用户ID
     * @param nickname      昵称（可选）
     * @param avatar        头像URL（可选）
     * @param memberAvatar  会员头像URL（可选）
     * @return 更新时间
     */
    @Transactional(rollbackFor = Exception.class)
    public String updateUserInfo(String userId, String requestUserId,
                                 String nickname, String avatar, String memberAvatar) {
        log.info("修改用户信息: userId={}, nickname={}, avatar={}, memberAvatar={}",
                userId, nickname, avatar, memberAvatar);

        // 验证权限：令牌中的userId必须与请求中的userId一致
        if (!userId.equals(requestUserId)) {
            throw new BusinessException(ErrorCode.PERMISSION_DENIED, "无权修改其他用户的信息");
        }

        // 查询用户
        User user = userRepository.findById(encryptUtils.uuidToBytes(userId))
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_EXIST));

        // 修改非空字段
        boolean hasUpdate = false;
        if (nickname != null && !nickname.isEmpty()) {
            user.setNickname(nickname);
            hasUpdate = true;
        }
        if (avatar != null) {
            user.setAvatar(avatar.isEmpty() ? null : avatar);
            hasUpdate = true;
        }
        if (memberAvatar != null) {
            user.setMemberAvatar(memberAvatar.isEmpty() ? null : memberAvatar);
            hasUpdate = true;
        }

        if (!hasUpdate) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "未提供任何需要修改的字段");
        }

        // 保存（触发@PreUpdate自动更新updateTime）
        user = userRepository.save(user);

        return user.getUpdateTime().format(DATETIME_FORMATTER);
    }

    /**
     * 用户主动退出
     *
     * @param userId           用户ID（来自令牌）
     * @param requestUserId    请求中的用户ID
     * @param autoLoginToken   自动登录令牌（可选）
     * @param currentJti       当前令牌的JTI
     * @param deviceId         设备ID
     * @param logoutAllDevices 是否全设备退出
     * @return 已注销令牌的JTI列表
     */
    @Transactional(rollbackFor = Exception.class)
    public List<String> logout(String userId, String requestUserId,
                               String autoLoginToken, String currentJti, String deviceId,
                               Boolean logoutAllDevices) {
        boolean isLogoutAll = Boolean.TRUE.equals(logoutAllDevices);
        log.info("用户主动退出: userId={}, deviceId={}, logoutAllDevices={}", userId, deviceId, isLogoutAll);

        // 验证权限
        if (!userId.equals(requestUserId)) {
            throw new BusinessException(ErrorCode.PERMISSION_DENIED, "无权注销其他用户的令牌");
        }

        // 查询用户（验证用户存在）
        User user = userRepository.findById(encryptUtils.uuidToBytes(userId))
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_EXIST));

        List<String> revokedJtis = new ArrayList<>();

        if (isLogoutAll) {
            // 全设备退出：递增令牌版本号
            user.setTokenVersion(user.getTokenVersion() + 1);
            userRepository.save(user);

            Integer newVersion = user.getTokenVersion();
            log.info("全设备退出: userId={}, newTokenVersion={}", userId, newVersion);

            // 更新Redis缓存
            try {
                tokenRedisService.updateTokenVersionCache(userId, newVersion);
            } catch (Exception e) {
                log.error("更新令牌版本号缓存失败", e);
            }

            // 清理所有自动登录令牌
            try {
                String autoLoginPattern = "auto:login:token:" + userId + ":*";
                tokenRedisService.deleteAutoLoginTokensByPattern(autoLoginPattern);
                log.info("已清理用户所有设备的自动登录令牌: userId={}", userId);
            } catch (Exception e) {
                log.error("清理自动登录令牌失败", e);
            }

            revokedJtis.add("all_tokens_via_version_" + newVersion);

        } else {
            // 单设备退出：只注销当前令牌
            // 1. 注销当前普通令牌
            if (currentJti != null && !currentJti.isEmpty()) {
                tokenRedisService.addToBlacklist(currentJti, 7200L);
                revokedJtis.add(currentJti);
                log.info("已注销当前普通令牌: jti={}", currentJti);
            }

            // 2. 注销自动登录令牌（如果提供）
            if (autoLoginToken != null && !autoLoginToken.isEmpty()) {
                try {
                    String token = autoLoginToken.replace("Bearer ", "").trim();
                    io.jsonwebtoken.Claims claims = jwtUtils.parseToken(token);
                    String autoJti = claims.getId();

                    String tokenUserId = claims.get("user_id", String.class);
                    if (userId.equals(tokenUserId)) {
                        tokenRedisService.addToBlacklist(autoJti, 604800L);
                        revokedJtis.add(autoJti);
                        log.info("已注销自动登录令牌: jti={}", autoJti);
                    }
                } catch (Exception e) {
                    log.error("注销自动登录令牌失败", e);
                }
            }

            // 3. 清理当前设备的自动登录令牌
            try {
                String autoLoginKey = "auto:login:token:" + userId + ":" + deviceId;
                tokenRedisService.deleteAutoLoginToken(autoLoginKey);
                log.info("已清理当前设备的自动登录令牌: key={}", autoLoginKey);
            } catch (Exception e) {
                log.error("清理自动登录令牌失败", e);
            }
        }

        return revokedJtis;
    }

    /**
     * 重置密码
     *
     * @param encryptedPhone 加密的手机号
     * @param verifyCode     验证码
     * @param newPassword    新密码（明文）
     * @param deviceId       设备ID
     * @return 重置时间
     */
    @Transactional(rollbackFor = Exception.class)
    public String resetPassword(String encryptedPhone, String verifyCode,
                                String newPassword, String deviceId) {
        log.info("密码重置请求: phone=加密, deviceId={}", deviceId);

            // 1. 解密手机号
            String plainPhone = encryptUtils.decryptAES(encryptedPhone);
        if (plainPhone == null || plainPhone.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "手机号格式错误");
        }

        // 2. 验证验证码
        if (!verifyCodeService.verifyCode(encryptedPhone, verifyCode, deviceId)) {
            throw new BusinessException(ErrorCode.VERIFY_CODE_INVALID);
        }

        // 3. 查询用户
        User user = userRepository.findByPhone(plainPhone)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_EXIST));

        // 4. 加密新密码
        String hashedPassword = encryptUtils.encryptPassword(newPassword);

        // 5. 更新密码并递增令牌版本号
        user.setPassword(hashedPassword);
        user.setTokenVersion(user.getTokenVersion() + 1);
        user = userRepository.save(user);

        String userId = encryptUtils.bytesToUuid(user.getUserId());
        Integer newVersion = user.getTokenVersion();
        log.info("密码重置成功: userId={}, phone={}, newTokenVersion={}", userId, plainPhone, newVersion);

        // 6. 更新Redis中的版本号缓存
        try {
            tokenRedisService.updateTokenVersionCache(userId, newVersion);
        } catch (Exception e) {
            log.error("更新令牌版本号缓存失败", e);
        }

        // 7. 清理Redis中的自动登录令牌（双保险）
        try {
            String autoLoginPattern = "auto:login:token:" + userId + ":*";
            tokenRedisService.deleteAutoLoginTokensByPattern(autoLoginPattern);
            log.info("已清理用户所有自动登录令牌: userId={}", userId);
        } catch (Exception e) {
            log.error("清理自动登录令牌失败", e);
        }

        return LocalDateTime.now().format(DATETIME_FORMATTER);
    }

    /**
     * 修改用户密码（通过旧密码验证）
     *
     * @param userId          用户ID（UUID字符串）
     * @param oldPassword     原密码（明文）
     * @param newPassword     新密码（明文）
     * @param confirmPassword 确认新密码
     * @return 更新时间
     */
    @Transactional(rollbackFor = Exception.class)
    public String updatePassword(String userId, String oldPassword, 
                                 String newPassword, String confirmPassword) {
        log.info("用户密码修改请求: userId={}", userId);

        // 1. 校验两次新密码是否一致
        if (!newPassword.equals(confirmPassword)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "两次输入的新密码不一致");
        }

        // 2. 查询用户信息
        byte[] userIdBytes = encryptUtils.uuidToBytes(userId);
        User user = userRepository.findById(userIdBytes)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_EXIST));

        // 3. 校验原密码是否正确
        boolean oldPasswordMatch = encryptUtils.verifyPassword(oldPassword, user.getPassword());
        if (!oldPasswordMatch) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "原密码错误");
        }

        // 4. 加密新密码
        String hashedPassword = encryptUtils.encryptPassword(newPassword);

        // 5. 更新密码并递增令牌版本号
        user.setPassword(hashedPassword);
        user.setTokenVersion(user.getTokenVersion() + 1);
        user.setUpdateTime(LocalDateTime.now());
        user = userRepository.save(user);

        Integer newVersion = user.getTokenVersion();
        log.info("密码修改成功: userId={}, newTokenVersion={}", userId, newVersion);

        // 6. 更新Redis中的版本号缓存
        try {
            tokenRedisService.updateTokenVersionCache(userId, newVersion);
        } catch (Exception e) {
            log.error("更新令牌版本号缓存失败", e);
        }

        // 7. 清理Redis中的自动登录令牌（双保险）
        try {
            String autoLoginPattern = "auto:login:token:" + userId + ":*";
            tokenRedisService.deleteAutoLoginTokensByPattern(autoLoginPattern);
            log.info("已清理用户所有自动登录令牌: userId={}", userId);
        } catch (Exception e) {
            log.error("清理自动登录令牌失败", e);
        }

        return LocalDateTime.now().format(DATETIME_FORMATTER);
    }

    /**
     * 检查手机号是否已注册
     *
     * @param phone 手机号（可能是明文11位数字，也可能是加密后的Base64字符串）
     * @return true-已注册，false-未注册
     */
    public boolean checkPhoneRegistered(String phone) {
        try {
            String plainPhone;
            
            // 1. 判断是否为11位数字（明文手机号）
            if (phone.matches("^\\d{11}$")) {
                plainPhone = phone;
                log.info("手机号为明文格式: {}", phone.substring(0, 3) + "****" + phone.substring(7));
            } else {
                // 2. 尝试解密
                try {
                    plainPhone = encryptUtils.decryptAES(phone);
                    if (plainPhone == null || plainPhone.isEmpty()) {
                        log.warn("手机号解密失败，返回未注册");
                        return false;
                    }
                    log.info("手机号解密成功: {}", plainPhone.substring(0, 3) + "****" + plainPhone.substring(7));
                } catch (Exception e) {
                    log.error("手机号解密异常: {}", e.getMessage());
                    return false;
                }
            }
            
            // 3. 验证手机号格式：必须是1开头的11位数字
            if (!plainPhone.matches("^1\\d{10}$")) {
                log.info("手机号格式不正确（非1开头的11位数字），返回未注册: {}", plainPhone);
                return false;
            }
            
            // 4. 查询数据库检查是否已注册
            boolean exists = userRepository.existsByPhone(plainPhone);
            log.info("手机号注册状态查询: phone={}, registered={}", 
                    plainPhone.substring(0, 3) + "****" + plainPhone.substring(7), exists);
            
            return exists;
            
        } catch (Exception e) {
            log.error("检查手机号注册状态异常", e);
            return false;
        }
    }
}

