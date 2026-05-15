package com.ecards.member_management.controller;

import com.ecards.member_management.annotation.RequireAdminAuth;
import com.ecards.member_management.annotation.RequireRole;
import com.ecards.member_management.common.Result;
import com.ecards.member_management.constants.AdminConstants;
import com.ecards.member_management.dto.request.AdminCreateRequest;
import com.ecards.member_management.dto.response.AdminCreateResponse;
import com.ecards.member_management.service.AdminManagementService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * 管理员管理控制器
 * 
 * 功能：
 * 1. 创建管理员（仅超管）
 * 2. 查询管理员列表（后续扩展）
 * 3. 修改管理员状态（后续扩展）
 * 4. 重置管理员密码（后续扩展）
 * 
 * @author Ecards Team
 * @since 2025-10-28
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
public class AdminManagementController {

    private final AdminManagementService adminManagementService;

    /**
     * 创建管理员
     * 
     * 权限：仅超级管理员
     * 
     * @param request 创建请求
     * @return Result<AdminCreateResponse>
     */
    @PostMapping("/create")
    @RequireAdminAuth("创建管理员")
    @RequireRole(value = AdminConstants.Role.SUPER_ADMIN, description = "仅超级管理员可创建管理员")
    public Result<AdminCreateResponse> createAdmin(@Valid @RequestBody AdminCreateRequest request) {
        log.info("==================== Controller层：收到创建管理员请求 ====================");
        log.info("📥 请求参数: account={}, phone={}, role={}", 
                request.getAccount(), request.getPhone(), request.getAdminRole());

        try {
            log.info("🔄 调用Service层创建管理员...");
            AdminCreateResponse response = adminManagementService.createAdmin(request);
            log.info("✅ Service层返回成功: adminId={}, account={}", response.getAdminId(), response.getAccount());
            
            Result<AdminCreateResponse> result = Result.success("管理员创建成功", response);
            log.info("📤 Controller返回响应: code={}, message={}", result.getCode(), result.getMessage());
            return result;

        } catch (Exception e) {
            log.error("❌ Controller捕获异常: account={}, error={}", request.getAccount(), e.getMessage(), e);
            throw e;
        }
    }

    /**
     * 测试接口 - 验证管理员Token（所有管理员可访问）
     * 用于测试权限验证机制
     * 
     * @return Result<String>
     */
    @GetMapping("/test/auth")
    @RequireAdminAuth("测试管理员认证")
    public Result<String> testAdminAuth() {
        return Result.success("管理员认证成功", 
                String.format("当前管理员ID: %s, 角色: %s", 
                        com.ecards.member_management.context.AdminContext.getAdminId(),
                        com.ecards.member_management.context.AdminContext.getRoleCode()));
    }

    /**
     * 测试接口 - 验证超管权限（仅超管可访问）
     * 用于测试角色权限验证
     * 
     * @return Result<String>
     */
    @GetMapping("/test/super")
    @RequireAdminAuth("测试超管权限")
    @RequireRole(value = AdminConstants.Role.SUPER_ADMIN, description = "仅超级管理员可访问")
    public Result<String> testSuperAdminRole() {
        return Result.success("超管权限验证成功", 
                String.format("欢迎超级管理员: %s", 
                        com.ecards.member_management.context.AdminContext.getAccount()));
    }

    /**
     * 测试接口 - 验证审核员权限（仅审核员可访问）
     * 用于测试角色权限验证
     * 
     * @return Result<String>
     */
    @GetMapping("/test/auditor")
    @RequireAdminAuth("测试审核员权限")
    @RequireRole(value = AdminConstants.Role.AUDITOR, description = "仅审核员可访问")
    public Result<String> testAuditorRole() {
        return Result.success("审核员权限验证成功", 
                String.format("欢迎审核员: %s", 
                        com.ecards.member_management.context.AdminContext.getAccount()));
    }
}


