package com.ecards.member_management.controller;

import com.ecards.member_management.annotation.RequireAdminAuth;
import com.ecards.member_management.common.Result;
import com.ecards.member_management.dto.request.AdminPasswordUpdateRequest;
import com.ecards.member_management.dto.request.AdminSndPasswordUpdateRequest;
import com.ecards.member_management.service.AdminPasswordService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * 管理员密码管理控制器
 * 
 * @author Ecards Team
 * @since 2025-10-29
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/admin/password")
@RequiredArgsConstructor
public class AdminPasswordController {

    private final AdminPasswordService passwordService;

    /**
     * 修改登录密码
     */
    @PostMapping("/update")
    @RequireAdminAuth
    public Result<?> updatePassword(@Valid @RequestBody AdminPasswordUpdateRequest request) {
        log.info("收到修改登录密码请求");
        passwordService.updatePassword(request);
        return Result.success("密码修改成功，请重新登录");
    }

    /**
     * 修改二级密码
     */
    @PostMapping("/snd-password/update")
    @RequireAdminAuth
    public Result<?> updateSndPassword(@Valid @RequestBody AdminSndPasswordUpdateRequest request) {
        log.info("收到修改二级密码请求");
        passwordService.updateSndPassword(request);
        return Result.success("二级密码修改成功");
    }
}

