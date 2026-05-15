package com.ecards.member_management.controller;

import com.ecards.member_management.common.Result;
import com.ecards.member_management.dto.request.AdminPasswordResetRequest;
import com.ecards.member_management.dto.request.AdminSndPasswordResetRequest;
import com.ecards.member_management.service.AdminPasswordRecoveryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * 管理员密码找回控制器
 * 
 * @author Ecards Team
 * @since 2025-10-29
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/admin/password-recovery")
@RequiredArgsConstructor
public class AdminPasswordRecoveryController {

    private final AdminPasswordRecoveryService passwordRecoveryService;

    /**
     * 找回登录密码
     */
    @PostMapping("/reset-password")
    public Result<?> resetPassword(@Valid @RequestBody AdminPasswordResetRequest request) {
        log.info("收到管理员找回登录密码请求: phone={}", request.getPhone());
        passwordRecoveryService.resetPassword(request);
        return Result.success("登录密码重置成功，请使用新密码登录");
    }

    /**
     * 找回二级密码
     */
    @PostMapping("/reset-snd-password")
    public Result<?> resetSndPassword(@Valid @RequestBody AdminSndPasswordResetRequest request) {
        log.info("收到管理员找回二级密码请求: phone={}", request.getPhone());
        passwordRecoveryService.resetSndPassword(request);
        return Result.success("二级密码重置成功");
    }
}

