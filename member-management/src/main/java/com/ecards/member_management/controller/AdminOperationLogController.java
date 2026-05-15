package com.ecards.member_management.controller;

import com.ecards.member_management.annotation.RequireAdminAuth;
import com.ecards.member_management.common.Result;
import com.ecards.member_management.dto.request.AdminOperationLogQueryRequest;
import com.ecards.member_management.dto.response.AdminOperationLogResponse;
import com.ecards.member_management.service.AdminOperationLogService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * 操作日志查询控制器
 * 
 * @author Ecards Team
 * @since 2025-10-29
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/admin/operation-logs")
@RequiredArgsConstructor
public class AdminOperationLogController {

    private final AdminOperationLogService operationLogService;

    /**
     * 分页查询操作日志
     * 权限：客服无权限，审核员只看自己，超级管理员查看全部
     */
    @GetMapping("/query")
    @RequireAdminAuth
    public Result<AdminOperationLogResponse> queryLogs(@Valid @ModelAttribute AdminOperationLogQueryRequest request) {
        log.info("收到操作日志查询请求: {}", request);
        AdminOperationLogResponse response = operationLogService.queryLogs(request);
        return Result.success("查询成功", response);
    }
}

