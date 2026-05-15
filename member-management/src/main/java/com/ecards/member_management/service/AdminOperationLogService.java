package com.ecards.member_management.service;

import com.ecards.member_management.constants.AdminConstants;
import com.ecards.member_management.context.AdminContext;
import com.ecards.member_management.dto.request.AdminOperationLogQueryRequest;
import com.ecards.member_management.dto.response.AdminOperationLogResponse;
import com.ecards.member_management.entity.AdminOperationLog;
import com.ecards.member_management.exception.BusinessException;
import com.ecards.member_management.repository.AdminOperationLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 操作日志查询服务
 * 
 * @author Ecards Team
 * @since 2025-10-29
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminOperationLogService {

    private final AdminOperationLogRepository operationLogRepository;
    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * 分页查询操作日志
     * 权限过滤：客服无权限，审核员只看自己，超级管理员查看全部
     */
    public AdminOperationLogResponse queryLogs(AdminOperationLogQueryRequest request) {
        log.info("查询操作日志: adminId={}, adminRole={}, request={}", 
                AdminContext.getAdminId(), AdminContext.getAdminRole(), request);

        // 1. 权限检查
        int adminRole = AdminContext.getAdminRole();
        String currentAccount = AdminContext.getAccount();

        // 客服无权限
        if (adminRole == AdminConstants.Role.CUSTOMER_SERVICE) {
            throw new BusinessException(403, "客服无权查询操作日志");
        }

        // 2. 确定查询账号
        String targetAccount;
        if (adminRole == AdminConstants.Role.AUDITOR) {
            // 审核员：只能查看自己的日志
            targetAccount = currentAccount;
            log.info("审核员查询日志，自动过滤为自己的账号: {}", targetAccount);
        } else if (adminRole == AdminConstants.Role.SUPER_ADMIN) {
            // 超级管理员：可以按账号筛选，如果request中有adminAccount则按其筛选
            targetAccount = request.getAdminAccount();
            log.info("超级管理员查询日志，筛选账号: {}", targetAccount != null ? targetAccount : "全部");
        } else {
            throw new BusinessException(403, "无权限查询操作日志");
        }

        // 3. 分页查询
        Pageable pageable = PageRequest.of(
                request.getPageNum() - 1, 
                request.getPageSize(),
                Sort.by(Sort.Direction.DESC, "operationTime") // 按操作时间倒序
        );

        Page<AdminOperationLog> page;
        if (targetAccount != null && !targetAccount.isEmpty()) {
            // 按账号筛选
            page = operationLogRepository.findByAdminAccount(targetAccount, pageable);
        } else {
            // 查询全部
            page = operationLogRepository.findAll(pageable);
        }

        // 4. 转换响应
        List<AdminOperationLogResponse.LogRecord> records = page.getContent().stream()
                .map(this::convertToLogRecord)
                .collect(Collectors.toList());

        return AdminOperationLogResponse.builder()
                .records(records)
                .total(page.getTotalElements())
                .pageNum(request.getPageNum())
                .pageSize(request.getPageSize())
                .totalPages(page.getTotalPages())
                .build();
    }

    /**
     * 转换日志记录
     */
    private AdminOperationLogResponse.LogRecord convertToLogRecord(AdminOperationLog log) {
        return AdminOperationLogResponse.LogRecord.builder()
                .logId(log.getLogId())
                .adminAccount(log.getAdminAccount())
                .operationType(log.getOperationType())
                .operationDesc(log.getOperationDesc())
                .targetType(log.getTargetType())
                .operationIp(log.getOperationIp())
                .deviceId(log.getDeviceId())
                .result(log.getResult())
                .createTime(log.getOperationTime() != null ? 
                        log.getOperationTime().format(DATETIME_FORMATTER) : null)
                .build();
    }
}

