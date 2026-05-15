package com.ecards.member_management.controller;

import com.ecards.member_management.common.Result;
import com.ecards.member_management.constants.TokenConstants;
import com.ecards.member_management.context.TokenContext;
import com.ecards.member_management.dto.request.*;
import com.ecards.member_management.dto.response.*;
import com.ecards.member_management.entity.MerchantExtend;
import com.ecards.member_management.exception.BusinessException;
import com.ecards.member_management.repository.MerchantExtendRepository;
import com.ecards.member_management.service.TokenService;
import com.ecards.member_management.service.UserService;
import com.ecards.member_management.utils.CookieUtils;
import com.ecards.member_management.utils.EncryptUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

/**
 * 用户控制器
 * 提供用户注册、登录等API接口
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final TokenService tokenService;
    private final CookieUtils cookieUtils;
    private final MerchantExtendRepository merchantExtendRepository;
    private final EncryptUtils encryptUtils;
    private final com.ecards.member_management.service.StaffService staffService;
    
    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final int TEST_PERIOD_DAYS = 7;

    /**
     * 检查手机号是否已注册
     * POST /api/v1/users/checkPhone
     *
     * @param request  检查请求
     * @param deviceId 设备ID
     * @return 注册状态
     */
    @PostMapping("/checkPhone")
    public Result<CheckPhoneResponse> checkPhone(
            @Valid @RequestBody CheckPhoneRequest request,
            @RequestHeader(TokenConstants.DEVICE_ID_HEADER) String deviceId) {
        
        try {
            log.info("收到手机号注册检查请求: phone={}, deviceId={}", 
                    request.getPhone().substring(0, Math.min(10, request.getPhone().length())) + "...",
                    deviceId);
            
            // 检查手机号是否已注册
            boolean registered = userService.checkPhoneRegistered(request.getPhone());
            
            // 构建响应
            CheckPhoneResponse response = CheckPhoneResponse.builder()
                    .registered(registered)
                    .build();
            
            String message = registered ? "该手机号已注册" : "该手机号未注册";
            return Result.success(message, response);
            
        } catch (Exception e) {
            log.error("检查手机号注册状态失败", e);
            return Result.fail(e.getMessage());
        }
    }

    /**
     * 用户注册
     * POST /api/v1/users/registration
     *
     * @param request     注册请求
     * @param deviceId    设备ID
     * @param httpRequest HTTP请求
     * @return 注册结果
     */
    @PostMapping("/registration")
    public Result<UserRegistrationResponse> register(
            @Valid @RequestBody UserRegistrationRequest request,
            @RequestHeader(TokenConstants.DEVICE_ID_HEADER) String deviceId,
            HttpServletRequest httpRequest) {

        try {
            String loginIp = getClientIp(httpRequest);
            
            log.info("收到用户注册请求: phone={}, nickname={}, platform={}", 
                    request.getPhone().substring(0, Math.min(10, request.getPhone().length())) + "...",
                    request.getNickname(), request.getPlatform());

            // 注册用户
            Map<String, Object> result = userService.register(
                    request.getPhone(),
                    request.getPassword(),
                    request.getVerifyCode(),
                    request.getNickname(),
                    request.getInvitedCode(),
                    request.getRememberMe(),
                    request.getPlatform(),
                    deviceId,
                    loginIp
            );

            // 构建响应
            UserRegistrationResponse response = UserRegistrationResponse.builder()
                    .userId((String) result.get("userId"))
                    .userType((Integer) result.get("userType"))
                    .registerTime((String) result.get("registerTime"))
                    .normalToken((String) result.get("normalToken"))
                    .tokenExpireTime((String) result.get("tokenExpireTime"))
                    .userInfo(buildUserInfoResponse(result))
                    .autoLoginToken((String) result.get("autoLoginToken"))
                    .autoExpireTime((String) result.get("autoExpireTime"))
                    .build();

            return Result.success("注册成功", response);
        } catch (BusinessException e) {
            return Result.fail(e.getCode(), e.getMessage());
        } catch (Exception e) {
            log.error("用户注册失败", e);
            return Result.fail(e.getMessage());
        }
    }

    /**
     * 用户登录
     * POST /api/v1/users/login
     *
     * @param request     登录请求
     * @param deviceId    设备ID
     * @param httpRequest HTTP请求
     * @return 登录结果
     */
    @PostMapping("/login")
    public Result<UserLoginResponse> login(
            @Valid @RequestBody UserLoginRequest request,
            @RequestHeader(TokenConstants.DEVICE_ID_HEADER) String deviceId,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse) {

        try {
            String loginIp = getClientIp(httpRequest);
            
            log.info("收到用户登录请求: phone={}, platform={}, rememberMe={}", 
                    request.getPhone().substring(0, Math.min(10, request.getPhone().length())) + "...",
                    request.getPlatform(), request.getRememberMe());

            // 用户登录（现在支持rememberMe时长选择）
            Map<String, Object> result = userService.login(
                    request.getPhone(),
                    request.getPassword(),
                    request.getRememberMe(),
                    request.getPlatform(),
                    deviceId,
                    loginIp
            );

            // Web端设置Cookie
            if ("WEB".equals(request.getPlatform())) {
                String autoLoginToken = (String) result.get("autoLoginToken");
                String autoExpireTime = (String) result.get("autoExpireTime");
                
                if (autoLoginToken != null && autoExpireTime != null) {
                    int maxAge = calculateMaxAge(autoExpireTime);
                    cookieUtils.setAutoLoginCookie(httpResponse, autoLoginToken, maxAge);
                    log.info("Web端登录，已设置自动登录Cookie: maxAge={}", maxAge);
                }
            }

            // 构建响应
            UserLoginResponse response = UserLoginResponse.builder()
                    .normalToken((String) result.get("normalToken"))
                    .tokenExpireTime((String) result.get("tokenExpireTime"))
                    .userInfo(buildLoginUserInfoResponse(result))
                    .autoLoginToken((String) result.get("autoLoginToken"))
                    .autoExpireTime((String) result.get("autoExpireTime"))
                    .build();

            return Result.success("登录成功", response);
        } catch (BusinessException e) {
            return Result.fail(e.getCode(), e.getMessage());
        } catch (Exception e) {
            log.error("用户登录失败", e);
            return Result.fail(e.getMessage());
        }
    }

    /**
     * 构建用户信息响应（注册）
     */
    @SuppressWarnings("unchecked")
    private UserRegistrationResponse.UserInfo buildUserInfoResponse(Map<String, Object> result) {
        Map<String, Object> userInfo = (Map<String, Object>) result.get("userInfo");
        return UserRegistrationResponse.UserInfo.builder()
                .userId((String) userInfo.get("userId"))
                .nickname((String) userInfo.get("nickname"))
                .userType((Integer) userInfo.get("userType"))
                .inviteCode((String) userInfo.get("inviteCode"))
                .avatar((String) userInfo.get("avatar"))
                .build();
    }

    /**
     * 构建用户信息响应（登录）
     */
    @SuppressWarnings("unchecked")
    private UserLoginResponse.UserInfo buildLoginUserInfoResponse(Map<String, Object> result) {
        Map<String, Object> userInfo = (Map<String, Object>) result.get("userInfo");
        String userId = (String) userInfo.get("userId");
        Integer userType = (Integer) userInfo.get("userType");
        
        UserLoginResponse.UserInfo.UserInfoBuilder builder = UserLoginResponse.UserInfo.builder()
                .userId(userId)
                .nickname((String) userInfo.get("nickname"))
                .userType(userType)
                .avatar((String) userInfo.get("avatar"))
                .inviteCode((String) userInfo.get("inviteCode"));
        
        // 如果是商户，查询商家信息
        if (userType != null && userType == 2) {
            try {
                byte[] userIdBytes = encryptUtils.uuidToBytes(userId);
                MerchantExtend merchant = merchantExtendRepository.findByUserId(userIdBytes)
                        .orElse(null);
                
                if (merchant != null) {
                    UserLoginResponse.MerchantInfo.MerchantInfoBuilder merchantBuilder = 
                            UserLoginResponse.MerchantInfo.builder()
                                    .merchantId(encryptUtils.bytesToUuid(merchant.getMerchantId()))
                                    .merchantName(merchant.getMerchantName())
                                    .certification(merchant.getCertification())
                                    .merchantLevel(merchant.getMerchantLevel());
                    
                    // 如果是测试期商户（certification=2），返回测试期过期时间
                    if (merchant.getCertification() == 2) {
                        LocalDateTime createTime = merchant.getCreateTime();
                        LocalDateTime testExpireTime = createTime.plusDays(TEST_PERIOD_DAYS);
                        merchantBuilder.testExpireTime(testExpireTime.format(DATETIME_FORMATTER));
                    }
                    
                    builder.merchantInfo(merchantBuilder.build());
                }
            } catch (Exception e) {
                log.error("查询商家信息失败: userId={}", userId, e);
                // 不影响登录，只是不返回商家信息
            }
        }
        
        return builder.build();
    }

    /**
     * 用户信息查询
     * GET /api/v1/users/info
     * 
     * 查询当前用户的基础信息，用户ID从JWT令牌中获取
     * @return 用户信息
     */
    @GetMapping("/info")
    public Result<UserInfoResponse> getUserInfo() {
        try {
            // 从TokenContext获取当前用户ID
            String currentUserId = TokenContext.getCurrentUserId();
            if (currentUserId == null) {
                return Result.fail("令牌无效");
            }

            Map<String, Object> userInfoData = userService.getUserInfo(currentUserId);
            Integer userType = (Integer) userInfoData.get("userType");

            // 构建嵌套的用户信息响应
            UserInfoResponse.UserInfo.UserInfoBuilder userInfoBuilder = UserInfoResponse.UserInfo.builder()
                    .userId((String) userInfoData.get("userId"))
                    .nickname((String) userInfoData.get("nickname"))
                    .userType(userType)
                    .avatar((String) userInfoData.get("avatar"))
                    .inviteCode((String) userInfoData.get("inviteCode"))
                    .memberAvatar((String) userInfoData.get("memberAvatar"))
                    .phone((String) userInfoData.get("phone")) // 已加密的手机号
                    .invitedCode((String) userInfoData.get("invitedCode"))
                    .registerTime((String) userInfoData.get("registerTime"));

            // 如果是商户，查询商家信息
            if (userType != null && userType == 2) {
                try {
                    byte[] userIdBytes = encryptUtils.uuidToBytes(currentUserId);
                    MerchantExtend merchant = merchantExtendRepository.findByUserId(userIdBytes)
                            .orElse(null);

                    if (merchant != null) {
                        UserInfoResponse.MerchantInfo.MerchantInfoBuilder merchantBuilder =
                                UserInfoResponse.MerchantInfo.builder()
                                        .merchantId(encryptUtils.bytesToUuid(merchant.getMerchantId()))
                                        .merchantName(merchant.getMerchantName())
                                        .certification(merchant.getCertification())
                                        .merchantLevel(merchant.getMerchantLevel());

                        // 如果是测试期商户（certification=2），返回测试期过期时间
                        if (merchant.getCertification() == 2) {
                            LocalDateTime createTime = merchant.getCreateTime();
                            LocalDateTime testExpireTime = createTime.plusDays(TEST_PERIOD_DAYS);
                            merchantBuilder.testExpireTime(testExpireTime.format(DATETIME_FORMATTER));
                        }

                        userInfoBuilder.merchantInfo(merchantBuilder.build());
                    }
                } catch (Exception e) {
                    log.error("查询商家信息失败: userId={}", currentUserId, e);
                    // 不影响查询，只是不返回商家信息
                }
            }

            UserInfoResponse response = UserInfoResponse.builder()
                    .userInfo(userInfoBuilder.build())
                    .build();

            return Result.success("查询成功", response);
        } catch (Exception e) {
            log.error("查询用户信息失败", e);
            return Result.fail(e.getMessage());
        }
    }

    /**
     * 用户信息修改
     * PUT /api/v1/users/info
     *
     * @param request 修改请求
     * @return 修改结果
     */
    @PutMapping("/info")
    public Result<UserInfoUpdateResponse> updateUserInfo(
            @Valid @RequestBody UserInfoUpdateRequest request) {
        try {
            // 从TokenContext获取当前用户ID
            String currentUserId = TokenContext.getCurrentUserId();
            if (currentUserId == null) {
                return Result.fail("令牌无效");
            }

            String updateTime = userService.updateUserInfo(
                    currentUserId,
                    request.getUserId(),
                    request.getNickname(),
                    request.getAvatar(),
                    request.getMemberAvatar()
            );

            UserInfoUpdateResponse response = UserInfoUpdateResponse.builder()
                    .updateTime(updateTime)
                    .build();

            return Result.success("信息修改成功", response);
        } catch (Exception e) {
            log.error("修改用户信息失败", e);
            return Result.fail(e.getMessage());
        }
    }

    /**
     * 用户主动退出
     * DELETE /api/v1/users/logout
     *
     * @param request  退出请求
     * @param deviceId 设备ID
     * @return 注销结果
     */
    @DeleteMapping("/logout")
    public Result<UserLogoutResponse> logout(
            @Valid @RequestBody UserLogoutRequest request,
            @RequestHeader(TokenConstants.DEVICE_ID_HEADER) String deviceId,
            HttpServletResponse httpResponse) {
        try {
            // 从TokenContext获取当前用户ID和JTI
            String currentUserId = TokenContext.getCurrentUserId();
            String currentJti = TokenContext.get() != null ? TokenContext.get().getJti() : null;
            
            if (currentUserId == null) {
                return Result.fail("令牌无效");
            }

            List<String> revokedJtis = userService.logout(
                    currentUserId,
                    request.getUserId(),
                    request.getAutoLoginToken(),
                    currentJti,
                    deviceId,
                    request.getLogoutAllDevices()
            );

            // Web端清除Cookie
            if ("WEB".equals(request.getPlatform())) {
                cookieUtils.clearAutoLoginCookie(httpResponse);
                log.info("Web端退出登录，已清除自动登录Cookie");
            }

            UserLogoutResponse response = UserLogoutResponse.builder()
                    .revokedJtis(revokedJtis)
                    .build();

            return Result.success("退出登录成功，所有令牌已失效", response);
        } catch (Exception e) {
            log.error("用户退出失败", e);
            return Result.fail(e.getMessage());
        }
    }

    /**
     * 密码修改（通过旧密码验证）
     * PUT /api/v1/users/password
     *
     * @param request  密码修改请求
     * @param deviceId 设备ID
     * @return 修改结果
     */
    @PutMapping("/password")
    public Result<PasswordUpdateResponse> updatePassword(
            @Valid @RequestBody PasswordUpdateRequest request,
            @RequestHeader(TokenConstants.DEVICE_ID_HEADER) String deviceId) {
        try {
            log.info("收到用户密码修改请求: userId={}", request.getUserId());

            // 从TokenContext获取当前用户ID进行校验
            String tokenUserId = TokenContext.getCurrentUserId();
            if (!request.getUserId().equals(tokenUserId)) {
                return Result.fail(403, "用户ID与令牌不匹配");
            }

            // 调用Service处理密码修改
            String updateTime = userService.updatePassword(
                    request.getUserId(),
                    request.getOldPassword(),
                    request.getNewPassword(),
                    request.getConfirmPassword()
            );

            PasswordUpdateResponse response = PasswordUpdateResponse.builder()
                    .updateTime(updateTime)
                    .build();

            return Result.success("密码修改成功，所有令牌已失效，请重新登录", response);
        } catch (Exception e) {
            log.error("密码修改失败: userId={}", request.getUserId(), e);
            return Result.fail(e.getMessage());
        }
    }

    /**
     * 密码重置
     * POST /api/v1/users/password/reset
     *
     * @param request  重置请求
     * @param deviceId 设备ID
     * @return 重置结果
     */
    @PostMapping("/password/reset")
    public Result<PasswordResetResponse> resetPassword(
            @Valid @RequestBody PasswordResetRequest request,
            @RequestHeader(TokenConstants.DEVICE_ID_HEADER) String deviceId) {
        try {
            log.info("收到密码重置请求: phone={}, platform={}",
                    request.getPhone().substring(0, Math.min(10, request.getPhone().length())) + "...",
                    request.getPlatform());

            String resetTime = userService.resetPassword(
                    request.getPhone(),
                    request.getVerifyCode(),
                    request.getNewPassword(),
                    deviceId
            );

            PasswordResetResponse response = PasswordResetResponse.builder()
                    .resetTime(resetTime)
                    .build();

            return Result.success("密码重置成功，请重新登录", response);
        } catch (Exception e) {
            log.error("密码重置失败", e);
            return Result.fail(e.getMessage());
        }
    }

    /**
     * 查询用户的所有工作关系
     * GET /api/v1/users/work-relations
     * 
     * 权限：员工用户（需要普通令牌）
     * 说明：只返回在职的工作关系（status=1）
     * 
     * @param userId 用户ID（必须与令牌中的userId一致）
     * @return 工作关系列表
     */
    @GetMapping("/work-relations")
    public Result<UserWorkRelationsResponse> getWorkRelations(
            @RequestParam String userId) {
        
        try {
            // 1. 获取当前用户ID（从令牌上下文）
            String currentUserId = TokenContext.getCurrentUserId();

            // 2. 校验只能查询自己的工作关系
            if (!userId.equals(currentUserId)) {
                return Result.fail("只能查询自己的工作关系");
            }

            // 3. 调用Service
            UserWorkRelationsResponse response = staffService.getUserWorkRelations(userId);

            return Result.success("工作关系查询成功", response);
        } catch (Exception e) {
            log.error("工作关系查询失败", e);
            return Result.fail("工作关系查询失败：" + e.getMessage());
        }
    }

    /**
     * 小程序自动登录
     * POST /api/v1/users/autologin-wx
     *
     * @param authorization 自动登录令牌
     * @param deviceId      设备ID
     * @param httpRequest   HTTP请求
     * @return 自动登录结果
     */
    @PostMapping("/autologin-wx")
    public Result<AutoLoginResponse> autoLoginWx(
            @RequestHeader(TokenConstants.AUTHORIZATION_HEADER) String authorization,
            @RequestHeader(TokenConstants.DEVICE_ID_HEADER) String deviceId,
            HttpServletRequest httpRequest) {

        try {
            String loginIp = getClientIp(httpRequest);
            
            // 提取Bearer令牌
            String autoLoginToken = extractBearerToken(authorization);
            if (autoLoginToken == null) {
                return Result.fail("Authorization格式错误");
            }

            log.info("收到小程序自动登录请求: deviceId={}, autoLoginToken={}", deviceId, autoLoginToken);

            // 调用TokenService进行自动登录
            Map<String, Object> result = tokenService.generateNormalTokenByAutoLogin(
                    autoLoginToken, "MINI_PROGRAM", deviceId, loginIp);

            // 构建响应
            AutoLoginResponse response = AutoLoginResponse.builder()
                    .normalToken((String) result.get("token"))
                    .normalExpireTime((String) result.get("expireTime"))
                    .tokenRotated((Boolean) result.getOrDefault("tokenRotated", false))
                    .newAutoLoginToken((String) result.get("newAutoLoginToken"))
                    .newAutoExpireTime((String) result.get("newAutoExpireTime"))
                    .userInfo(buildSimpleUserInfoResponse(result))
                    .build();

            return Result.success("自动登录成功", response);
        } catch (Exception e) {
            log.error("小程序自动登录失败", e);
            return Result.fail(e.getMessage());
        }
    }

    /**
     * Web自动登录
     * POST /api/v1/users/autologin-web
     *
     * @param deviceId      设备ID
     * @param httpRequest   HTTP请求
     * @param httpResponse  HTTP响应
     * @return 自动登录结果
     */
    @PostMapping("/autologin-web")
    public Result<AutoLoginResponse> autoLoginWeb(
            @RequestHeader(TokenConstants.DEVICE_ID_HEADER) String deviceId,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse) {

        try {
            String loginIp = getClientIp(httpRequest);
            
            // 从Cookie获取自动登录令牌
            String autoLoginToken = cookieUtils.getAutoLoginTokenFromCookie(httpRequest);
            if (autoLoginToken == null || autoLoginToken.trim().isEmpty()) {
                return Result.fail("未找到自动登录令牌");
            }

            // 移除Bearer前缀（如果存在）
            if (autoLoginToken.startsWith(TokenConstants.BEARER_PREFIX)) {
                autoLoginToken = autoLoginToken.substring(TokenConstants.BEARER_PREFIX.length());
            }

            log.info("收到Web自动登录请求: deviceId={}", deviceId);

            // 调用TokenService进行自动登录
            Map<String, Object> result = tokenService.generateNormalTokenByAutoLogin(
                    autoLoginToken, "WEB", deviceId, loginIp);

            // 处理令牌轮换（Web端通过Cookie更新）
            Boolean tokenRotated = (Boolean) result.getOrDefault("tokenRotated", false);
            if (tokenRotated) {
                String newAutoLoginToken = (String) result.get("newAutoLoginToken");
                if (newAutoLoginToken != null) {
                    // 计算新令牌的过期时间（秒）
                    String newAutoExpireTime = (String) result.get("newAutoExpireTime");
                    int maxAge = calculateMaxAge(newAutoExpireTime);
                    
                    // 设置新的Cookie
                    cookieUtils.setAutoLoginCookie(httpResponse, newAutoLoginToken, maxAge);
                    log.info("Web端令牌轮换，已更新Cookie");
                }
            }

            // 构建响应（Web端不返回新令牌，通过Cookie处理）
            AutoLoginResponse response = AutoLoginResponse.builder()
                    .normalToken((String) result.get("token"))
                    .normalExpireTime((String) result.get("expireTime"))
                    .tokenRotated(tokenRotated)
                    .newAutoLoginToken(null) // Web端不返回，通过Cookie处理
                    .newAutoExpireTime(null)
                    .userInfo(buildSimpleUserInfoResponse(result))
                    .build();

            return Result.success("自动登录成功", response);
        } catch (Exception e) {
            log.error("Web自动登录失败", e);
            return Result.fail(e.getMessage());
        }
    }

    /**
     * 获取客户端IP地址
     */
    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        // 处理多个IP的情况（取第一个）
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }

    /**
     * 提取Bearer令牌
     */
    private String extractBearerToken(String authorization) {
        if (authorization != null && authorization.startsWith(TokenConstants.BEARER_PREFIX)) {
            return authorization.substring(TokenConstants.BEARER_PREFIX.length());
        }
        return null;
    }

    /**
     * 计算Cookie的MaxAge（秒）
     */
    private int calculateMaxAge(String expireTimeStr) {
        try {
            LocalDateTime expireTime = LocalDateTime.parse(expireTimeStr, DATETIME_FORMATTER);
            LocalDateTime now = LocalDateTime.now();
            long seconds = java.time.Duration.between(now, expireTime).getSeconds();
            return (int) Math.max(0, seconds); // 确保不为负数
        } catch (Exception e) {
            log.warn("解析过期时间失败，使用默认7天: {}", expireTimeStr, e);
            return 7 * 24 * 60 * 60; // 默认7天
        }
    }

    /**
     * 构建简化用户信息响应（自动登录接口使用）
     */
    @SuppressWarnings("unchecked")
    private AutoLoginResponse.SimpleUserInfo buildSimpleUserInfoResponse(Map<String, Object> result) {
        Map<String, Object> userInfo = (Map<String, Object>) result.get("userInfo");
        if (userInfo == null) {
            return null;
        }

        String userId = (String) userInfo.get("userId");
        Integer userType = (Integer) userInfo.get("userType");

        AutoLoginResponse.SimpleUserInfo.SimpleUserInfoBuilder builder = AutoLoginResponse.SimpleUserInfo.builder()
                .userId(userId)
                .nickname((String) userInfo.get("nickname"))
                .userType(userType)
                .avatar((String) userInfo.get("avatar"))
                .inviteCode((String) userInfo.get("inviteCode"));

        // 如果是商户，查询商家信息
        if (userType != null && userType == 2) {
            try {
                byte[] userIdBytes = encryptUtils.uuidToBytes(userId);
                MerchantExtend merchant = merchantExtendRepository.findByUserId(userIdBytes)
                        .orElse(null);

                if (merchant != null) {
                    AutoLoginResponse.MerchantInfo.MerchantInfoBuilder merchantBuilder =
                            AutoLoginResponse.MerchantInfo.builder()
                                    .merchantId(encryptUtils.bytesToUuid(merchant.getMerchantId()))
                                    .merchantName(merchant.getMerchantName())
                                    .certification(merchant.getCertification())
                                    .merchantLevel(merchant.getMerchantLevel());

                    // 如果是测试期商户（certification=2），返回测试期过期时间
                    if (merchant.getCertification() == 2) {
                        LocalDateTime createTime = merchant.getCreateTime();
                        LocalDateTime testExpireTime = createTime.plusDays(TEST_PERIOD_DAYS);
                        merchantBuilder.testExpireTime(testExpireTime.format(DATETIME_FORMATTER));
                    }

                    builder.merchantInfo(merchantBuilder.build());
                }
            } catch (Exception e) {
                log.error("自动登录查询商家信息失败: userId={}", userId, e);
                // 不影响登录，只是不返回商家信息
            }
        }

        return builder.build();
    }
}

