package com.ecards.member_management.controller;

import com.ecards.member_management.annotation.RequireAdminAuth;
import com.ecards.member_management.annotation.RequireRole;
import com.ecards.member_management.common.Result;
import com.ecards.member_management.constants.AdminConstants;
import com.ecards.member_management.dto.request.*;
import com.ecards.member_management.dto.response.*;
import com.ecards.member_management.service.AdminMerchantAuditService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * 管理员商户审核控制器
 * 
 * 功能：
 * 1. 商户审核列表查询（分页、筛选、排序）
 * 2. 商户审核详情查询
 * 3. 商户审核通过
 * 4. 商户审核拒绝
 * 
 * 权限：超级管理员和审核员可访问
 * 
 * @author Ecards Team
 * @since 2025-10-28
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/admin/merchant-audit")
@RequiredArgsConstructor
public class AdminMerchantAuditController {

    private final AdminMerchantAuditService auditService;

    /**
     * 查询商户审核列表
     * 
     * 权限：超级管理员、审核员
     * 
     * @param request 查询请求
     * @return Result<MerchantAuditListResponse>
     */
    @GetMapping("/list")
    @RequireAdminAuth("查询商户审核列表")
    public Result<MerchantAuditListResponse> getAuditList(@Valid @ModelAttribute MerchantAuditListRequest request) {
        log.info("查询商户审核列表: status={}, sortOrder={}, page={}/{}", 
                request.getAuditStatus(), request.getSortOrder(), request.getPageNum(), request.getPageSize());
        
        MerchantAuditListResponse response = auditService.getAuditList(request);
        return Result.success("查询成功", response);
    }

    /**
     * 查询商户审核详情
     * 
     * 权限：超级管理员、审核员
     * 
     * @param auditId 审核记录ID
     * @return Result<MerchantAuditDetailResponse>
     */
    @GetMapping("/detail")
    @RequireAdminAuth("查询商户审核详情")
    public Result<MerchantAuditDetailResponse> getAuditDetail(@RequestParam Long auditId) {
        log.info("查询商户审核详情: auditId={}", auditId);
        
        MerchantAuditDetailRequest request = MerchantAuditDetailRequest.builder()
                .auditId(auditId)
                .build();
        
        MerchantAuditDetailResponse response = auditService.getAuditDetail(request);
        return Result.success("查询成功", response);
    }

    /**
     * 通过商户审核
     * 
     * 权限：超级管理员、审核员
     * 
     * @param request 审核请求
     * @return Result<Void>
     */
    @PostMapping("/approve")
    @RequireAdminAuth("通过商户审核")
    public Result<Void> approveMerchant(@Valid @RequestBody MerchantAuditApproveRequest request) {
        log.info("通过商户审核: auditId={}", request.getAuditId());
        
        auditService.approveMerchant(request);
        return Result.success("审核通过成功");
    }

    /**
     * 拒绝商户审核
     * 
     * 权限：超级管理员、审核员
     * 
     * @param request 审核请求
     * @return Result<Void>
     */
    @PostMapping("/reject")
    @RequireAdminAuth("拒绝商户审核")
    public Result<Void> rejectMerchant(@Valid @RequestBody MerchantAuditRejectRequest request) {
        log.info("拒绝商户审核: auditId={}, reason={}", request.getAuditId(), request.getRejectReason());
        
        auditService.rejectMerchant(request);
        return Result.success("审核拒绝成功");
    }
}

