package com.ecards.member_management.controller;

import com.ecards.member_management.annotation.RequireAdminAuth;
import com.ecards.member_management.annotation.RequireRole;
import com.ecards.member_management.common.Result;
import com.ecards.member_management.constants.AdminConstants;
import com.ecards.member_management.dto.request.MerchantBanRequest;
import com.ecards.member_management.dto.request.MerchantUnbanRequest;
import com.ecards.member_management.dto.request.MerchantWarnRequest;
import com.ecards.member_management.service.AdminMerchantManagementService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * 管理员商户管理控制器
 * 
 * @author Ecards Team
 * @since 2025-10-29
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/admin/merchant-management")
@RequiredArgsConstructor
public class AdminMerchantManagementController {

    private final AdminMerchantManagementService merchantManagementService;

    /**
     * 商户警告
     * 权限：超级管理员、审核员
     */
    @PostMapping("/warn")
    @RequireAdminAuth
    @RequireRole({AdminConstants.Role.SUPER_ADMIN, AdminConstants.Role.AUDITOR})
    public Result<?> warnMerchant(@Valid @RequestBody MerchantWarnRequest request) {
        log.info("收到商户警告请求: phone={}", request.getPhone());
        merchantManagementService.warnMerchant(request);
        return Result.success("商户警告成功");
    }

    /**
     * 商户封禁
     * 权限：仅超级管理员
     */
    @PostMapping("/ban")
    @RequireAdminAuth
    @RequireRole({AdminConstants.Role.SUPER_ADMIN})
    public Result<?> banMerchant(@Valid @RequestBody MerchantBanRequest request) {
        log.info("收到商户封禁请求: phone={}", request.getPhone());
        merchantManagementService.banMerchant(request);
        return Result.success("商户封禁成功");
    }

    /**
     * 解除商户封禁
     * 权限：仅超级管理员
     */
    @PostMapping("/unban")
    @RequireAdminAuth
    @RequireRole({AdminConstants.Role.SUPER_ADMIN})
    public Result<?> unbanMerchant(@Valid @RequestBody MerchantUnbanRequest request) {
        log.info("收到解除商户封禁请求: phone={}", request.getPhone());
        merchantManagementService.unbanMerchant(request);
        return Result.success("商户解封成功");
    }
}

