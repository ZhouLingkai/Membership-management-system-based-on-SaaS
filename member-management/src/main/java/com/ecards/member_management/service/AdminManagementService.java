package com.ecards.member_management.service;

import com.ecards.member_management.constants.AdminConstants;
import com.ecards.member_management.context.AdminContext;
import com.ecards.member_management.dto.request.AdminCreateRequest;
import com.ecards.member_management.dto.response.AdminCreateResponse;
import com.ecards.member_management.entity.Admin;
import com.ecards.member_management.entity.AdminOperationLog;
import com.ecards.member_management.exception.BusinessException;
import com.ecards.member_management.repository.AdminOperationLogRepository;
import com.ecards.member_management.repository.AdminRepository;
import com.ecards.member_management.utils.EncryptUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * 管理员管理服务
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
@Service
@RequiredArgsConstructor
public class AdminManagementService {

    private final AdminRepository adminRepository;
    private final AdminOperationLogRepository adminOperationLogRepository;
    private final EncryptUtils encryptUtils;
    private final ObjectMapper objectMapper;

    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * 创建管理员（仅超级管理员）
     * 
     * @param request 创建请求
     * @return AdminCreateResponse
     */
    @Transactional
    public AdminCreateResponse createAdmin(AdminCreateRequest request) {
        log.info("==================== 开始创建管理员 ====================");
        log.info("📝 请求参数: account={}, phone={}, role={}, remark={}", 
                request.getAccount(), request.getPhone(), request.getAdminRole(), request.getRemark());

        // 1. 禁止创建超级管理员
        if (request.getAdminRole() == AdminConstants.Role.SUPER_ADMIN) {
            log.error("❌ 禁止创建超级管理员: account={}", request.getAccount());
            throw new BusinessException(1006, "禁止创建超级管理员，只能创建审核员或客服");
        }

        // 2. 验证账号唯一性
        log.info("🔍 检查账号是否存在: {}", request.getAccount());
        if (adminRepository.existsByAccount(request.getAccount())) {
            log.error("❌ 账号已存在: account={}", request.getAccount());
            throw new BusinessException(1004, "账号已存在");
        }

        // 3. 验证手机号唯一性
        log.info("🔍 检查手机号是否存在: {}", request.getPhone());
        if (adminRepository.existsByPhone(request.getPhone())) {
            log.error("❌ 手机号已存在: phone={}", request.getPhone());
            throw new BusinessException(1005, "手机号已存在");
        }

        // 4. 获取当前操作者（创建者）
        String creatorId = AdminContext.getAdminId();
        log.info("👤 当前操作者ID: {}", creatorId);
        log.info("👤 当前操作者账号: {}", AdminContext.getAccount());
        log.info("👤 当前操作者角色: {}", AdminContext.getRoleCode());
        
        if (creatorId == null) {
            log.error("❌ 无法获取创建者ID，AdminContext为空");
            throw new BusinessException(500, "系统错误：无法获取创建者信息");
        }

        // 5. 创建管理员实体
        log.info("🏗️ 开始创建管理员实体...");
        Admin newAdmin = new Admin();
        newAdmin.setAdminId(encryptUtils.uuidToBytes(UUID.randomUUID().toString()));
        newAdmin.setPhone(request.getPhone());
        newAdmin.setAccount(request.getAccount());
        
        log.info("🔐 加密密码...");
        newAdmin.setPassword(encryptUtils.encryptPassword(request.getPassword()));
        newAdmin.setSndPswd(encryptUtils.encryptPassword(request.getSndPassword()));
        
        newAdmin.setAdminRole(request.getAdminRole());
        newAdmin.setTokenVersion(1);
        newAdmin.setStatus(AdminConstants.Status.ENABLED);
        newAdmin.setCreatorId(encryptUtils.uuidToBytes(creatorId));
        newAdmin.setRemark(request.getRemark());
        newAdmin.setCreateTime(LocalDateTime.now());
        newAdmin.setUpdateTime(LocalDateTime.now());

        // 6. 保存到数据库
        log.info("💾 准备保存到数据库...");
        try {
            adminRepository.save(newAdmin);
            log.info("✅ 数据库保存成功");
        } catch (Exception e) {
            log.error("❌ 数据库保存失败", e);
            throw new BusinessException(99999, "数据库保存失败: " + e.getMessage());
        }

        String newAdminId = encryptUtils.bytesToUuid(newAdmin.getAdminId());
        log.info("✅ 管理员创建成功: adminId={}, account={}, role={}, creatorId={}", 
                newAdminId, request.getAccount(), request.getAdminRole(), creatorId);

        // 7. 记录操作日志
        log.info("📝 记录操作日志...");
        logOperation(
                AdminConstants.OperationType.CREATE_ADMIN,
                newAdmin.getAdminId(),
                String.format("创建管理员: account=%s, role=%s", 
                        request.getAccount(), 
                        AdminConstants.Role.getRoleCode(request.getAdminRole())),
                request
        );

        // 8. 构建响应
        log.info("🎉 构建响应数据...");
        AdminCreateResponse response = AdminCreateResponse.builder()
                .adminId(newAdminId)
                .account(newAdmin.getAccount())
                .phone(maskPhone(newAdmin.getPhone()))
                .adminRole(newAdmin.getAdminRole())
                .roleCode(AdminConstants.Role.getRoleCode(newAdmin.getAdminRole()))
                .createTime(newAdmin.getCreateTime().format(DATETIME_FORMATTER))
                .creatorId(creatorId)
                .remark(newAdmin.getRemark())
                .build();
        
        log.info("==================== 创建管理员完成 ====================");
        return response;
    }

    /**
     * 记录操作日志
     * 
     * @param operationType 操作类型
     * @param targetId      操作对象ID
     * @param description   操作描述
     * @param requestParams 请求参数
     */
    private void logOperation(String operationType, byte[] targetId, String description, Object requestParams) {
        try {
            AdminOperationLog log = new AdminOperationLog();
            log.setAdminId(encryptUtils.uuidToBytes(AdminContext.getAdminId()));
            log.setAdminAccount(AdminContext.getAccount());
            log.setOperationType(operationType);
            log.setTargetType(AdminConstants.TargetType.ADMIN);
            log.setTargetId(targetId);
            log.setOperationDesc(description);
            log.setOperationIp(AdminContext.getLoginIp());
            log.setDeviceId(AdminContext.getDeviceId());
            log.setOperationTime(LocalDateTime.now());
            log.setResult(1); // 1-成功

            // 序列化请求参数（脱敏密码）
            if (requestParams != null) {
                try {
                    String paramsJson = objectMapper.writeValueAsString(requestParams);
                    // 简单的密码脱敏（将password和sndPassword字段值替换为***）
                    paramsJson = paramsJson.replaceAll("\"password\":\"[^\"]+\"", "\"password\":\"***\"");
                    paramsJson = paramsJson.replaceAll("\"sndPassword\":\"[^\"]+\"", "\"sndPassword\":\"***\"");
                    log.setRequestParams(paramsJson);
                } catch (JsonProcessingException e) {
                    this.log.warn("操作日志参数序列化失败", e);
                }
            }

            adminOperationLogRepository.save(log);

        } catch (Exception e) {
            // 日志记录失败不应影响主业务流程
            this.log.error("操作日志记录失败", e);
        }
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


