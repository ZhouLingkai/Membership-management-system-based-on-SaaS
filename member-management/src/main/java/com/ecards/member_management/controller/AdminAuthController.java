package com.ecards.member_management.controller;

import com.ecards.member_management.common.Result;
import com.ecards.member_management.config.AdminProperties;
import com.ecards.member_management.constants.AdminConstants;
import com.ecards.member_management.dto.request.AdminLoginRequest;
import com.ecards.member_management.dto.response.AdminLoginResponse;
import com.ecards.member_management.entity.Admin;
import com.ecards.member_management.service.AdminAuthService;
import com.ecards.member_management.utils.EncryptUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 管理员认证控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
public class AdminAuthController {

    private final AdminAuthService adminAuthService;
    private final AdminProperties adminProperties;
    private final EncryptUtils encryptUtils;

    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * 管理员登录
     *
     * @param request        登录请求
     * @param servletRequest HTTP请求
     * @return Result<AdminLoginResponse>
     */
    @PostMapping("/login")
    public Result<AdminLoginResponse> login(
            @Valid @RequestBody AdminLoginRequest request,
            HttpServletRequest servletRequest) {

        try {
            log.info("收到管理员登录请求: account={}, deviceId={}", 
                    request.getAccount(), request.getDeviceId());

            // 获取登录IP
            String loginIp = getClientIp(servletRequest);

            // 执行登录
            String adminToken = adminAuthService.login(
                    request.getAccount(),
                    request.getPassword(),
                    request.getDeviceId(),
                    loginIp
            );

            // 查询管理员信息
            Admin admin = adminAuthService.getAdminByAccount(request.getAccount());

            // 构建响应
            LocalDateTime expireTime = LocalDateTime.now()
                    .plusSeconds(adminProperties.getJwt().getExpiration() / 1000);

            AdminLoginResponse.AdminInfo adminInfo = AdminLoginResponse.AdminInfo.builder()
                    .adminId(encryptUtils.bytesToUuid(admin.getAdminId()))
                    .account(admin.getAccount())
                    .adminRole(admin.getAdminRole())
                    .roleCode(AdminConstants.Role.getRoleCode(admin.getAdminRole()))
                    .phone(maskPhone(admin.getPhone()))
                    .build();

            AdminLoginResponse response = AdminLoginResponse.builder()
                    .adminToken(adminToken)
                    .expireTime(expireTime.format(DATETIME_FORMATTER))
                    .adminInfo(adminInfo)
                    .build();

            log.info("管理员登录成功: account={}, role={}", 
                    request.getAccount(), admin.getAdminRole());

            return Result.success("登录成功", response);

        } catch (Exception e) {
            log.error("管理员登录失败: account={}, error={}", 
                    request.getAccount(), e.getMessage(), e);
            throw e;
        }
    }

    /**
     * 获取客户端真实IP
     */
    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        // 如果是多级代理，取第一个IP
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }

    /**
     * 手机号脱敏（中间4位）
     */
    private String maskPhone(String phone) {
        if (phone == null || phone.length() < 7) {
            return phone;
        }
        return phone.substring(0, 3) + "****" + phone.substring(7);
    }
}

