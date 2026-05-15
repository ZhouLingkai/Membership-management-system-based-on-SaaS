package com.ecards.member_management.service;

import com.ecards.member_management.constants.AdminConstants;
import com.ecards.member_management.context.AdminContext;
import com.ecards.member_management.dto.request.*;
import com.ecards.member_management.dto.response.*;
import com.ecards.member_management.entity.*;
import com.ecards.member_management.exception.BusinessException;
import com.ecards.member_management.repository.*;
import com.ecards.member_management.utils.EncryptUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 管理员商户审核服务
 * 
 * 功能：
 * 1. 商户审核列表查询（分页、筛选、排序）
 * 2. 商户审核详情查询
 * 3. 商户审核通过（创建商户信息、创建店铺）
 * 4. 商户审核拒绝
 * 
 * @author Ecards Team
 * @since 2025-10-28
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminMerchantAuditService {

    private final MerchantAuditRecordRepository auditRecordRepository;
    private final MerchantExtendRepository merchantExtendRepository;
    private final StoreRepository storeRepository;
    private final UserRepository userRepository;
    private final AdminRepository adminRepository;
    private final AdminOperationLogRepository operationLogRepository;
    private final EncryptUtils encryptUtils;
    private final ObjectMapper objectMapper;

    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * 查询商户审核列表（分页）
     * 
     * @param request 查询请求
     * @return MerchantAuditListResponse
     */
    public MerchantAuditListResponse getAuditList(MerchantAuditListRequest request) {
        log.info("查询商户审核列表: status={}, sortOrder={}, page={}, size={}", 
                request.getAuditStatus(), request.getSortOrder(), request.getPageNum(), request.getPageSize());

        // 构建分页参数
        Sort sort = request.getSortOrder() == 0 
                ? Sort.by(Sort.Direction.ASC, "createTime") 
                : Sort.by(Sort.Direction.DESC, "createTime");
        Pageable pageable = PageRequest.of(request.getPageNum() - 1, request.getPageSize(), sort);

        // 根据状态查询（0-待审核, 1-已通过, 2-已拒绝）
        Page<MerchantAuditRecord> page;
        if (request.getAuditStatus() == null) {
            // 查询全部
            page = auditRecordRepository.findAll(pageable);
        } else {
            // 查询指定状态
            page = auditRecordRepository.findByAuditStatus(request.getAuditStatus(), pageable);
        }

        // 转换为DTO
        List<MerchantAuditListResponse.AuditRecordItem> items = page.getContent().stream()
                .map(this::convertToListItem)
                .collect(Collectors.toList());

        // 计算总页数
        int totalPages = (int) Math.ceil((double) page.getTotalElements() / request.getPageSize());

        return MerchantAuditListResponse.builder()
                .records(items)
                .total(page.getTotalElements())
                .pageNum(request.getPageNum())
                .pageSize(request.getPageSize())
                .totalPages(totalPages)
                .build();
    }

    /**
     * 查询商户审核详情
     * 
     * @param request 查询请求
     * @return MerchantAuditDetailResponse
     */
    public MerchantAuditDetailResponse getAuditDetail(MerchantAuditDetailRequest request) {
        log.info("查询商户审核详情: auditId={}", request.getAuditId());

        // 查询审核记录
        MerchantAuditRecord record = auditRecordRepository.findById(request.getAuditId())
                .orElseThrow(() -> new BusinessException(404, "审核记录不存在"));

        // 查询用户信息（获取手机号）
        User user = userRepository.findById(record.getUserId())
                .orElseThrow(() -> new BusinessException(404, "用户不存在"));

        // 查询审核人信息（如果有）
        String auditorAccount = null;
        if (record.getAuditorId() != null) {
            auditorAccount = adminRepository.findById(record.getAuditorId())
                    .map(Admin::getAccount)
                    .orElse(null);
        }

        return MerchantAuditDetailResponse.builder()
                .auditId(record.getAuditId())
                .userId(encryptUtils.bytesToUuid(record.getUserId()))
                .userPhone(user.getPhone()) // 返回完整手机号，不脱敏
                .numStores(record.getNumStores())
                .numMembers(record.getNumMembers())
                .storeName(record.getStoreName())
                .storePhotos(record.getStorePhotos())
                .businessLicense(record.getBusinessLicense())
                .applicationMethod(record.getApplicationMethod())
                .applicationMethodDesc(getApplicationMethodDesc(record.getApplicationMethod()))
                .auditStatus(record.getAuditStatus())
                .auditStatusDesc(getAuditStatusDesc(record.getAuditStatus()))
                .rejectReason(record.getRejectReason())
                .createTime(record.getCreateTime().format(DATETIME_FORMATTER))
                .auditTime(record.getAuditTime() != null ? record.getAuditTime().format(DATETIME_FORMATTER) : null)
                .auditorAccount(auditorAccount)
                .build();
    }

    /**
     * 通过审核
     * 
     * @param request 审核请求
     */
    @Transactional
    public void approveMerchant(MerchantAuditApproveRequest request) {
        log.info("通过商户审核: auditId={}", request.getAuditId());

        // 1. 获取当前管理员ID
        byte[] adminId = encryptUtils.uuidToBytes(AdminContext.getAdminId());

        // 2. 查询审核记录
        MerchantAuditRecord record = auditRecordRepository.findById(request.getAuditId())
                .orElseThrow(() -> new BusinessException(404, "审核记录不存在"));

        // 3. 检查审核状态（0-待审核）
        if (record.getAuditStatus() != 0) {
            throw new BusinessException(400, "该记录不是待审核状态");
        }

        // 4. 查询用户信息
        User user = userRepository.findById(record.getUserId())
                .orElseThrow(() -> new BusinessException(404, "用户不存在"));

        // 5. 更新审核记录
        record.setAuditStatus(1); // 已通过
        record.setAuditorId(adminId);
        record.setAuditTime(LocalDateTime.now());
        auditRecordRepository.save(record);

        // 6. 创建或更新商户信息
        MerchantExtend merchantExtend = merchantExtendRepository.findByUserId(record.getUserId())
                .orElse(new MerchantExtend());
        
        if (merchantExtend.getMerchantId() == null) {
            // 新建商户
            merchantExtend.setMerchantId(encryptUtils.uuidToBytes(UUID.randomUUID().toString()));
            merchantExtend.setUserId(record.getUserId());
        }
        
        merchantExtend.setMerchantName(record.getStoreName());
        merchantExtend.setCertification(1); // 已认证
        merchantExtend.setMerchantLevel(1); // 普通等级
        merchantExtend.setPrivilegeExpireTime(LocalDateTime.now().plusYears(100)); // 默认永久
        merchantExtend.setRemainingNoticeCount(0);
        
        // 如果是新建，设置二级密码（默认等于用户密码）
        if (merchantExtend.getSndPswd() == null) {
            merchantExtend.setSndPswd(user.getPassword());
        }
        
        merchantExtendRepository.save(merchantExtend);

        // 7. 更新用户类型为商户
        user.setUserType(2);
        userRepository.save(user);

        // 8. 根据申请方式决定是否创建店铺
        if (record.getApplicationMethod() == 1) {
            // 直接认证通道：直接创建第一家店铺
            createFirstStore(merchantExtend.getMerchantId(), record, user.getPhone());
        } else if (record.getApplicationMethod() == 2) {
            // 免认证后补充资质：检查是否已有店铺（测试阶段可能已创建）
            List<Store> existingStores = storeRepository.findByMerchantId(merchantExtend.getMerchantId());
            if (existingStores.isEmpty()) {
                // 没有店铺，创建第一家店铺
                createFirstStore(merchantExtend.getMerchantId(), record, user.getPhone());
                log.info("免认证商户未创建店铺，自动创建第一家店铺");
            } else {
                // 已有店铺（测试阶段创建的），跳过创建
                log.info("免认证商户已有店铺，跳过自动创建: storeCount={}", existingStores.size());
            }
        }

        // 9. 记录操作日志
        logOperation(AdminConstants.OperationType.APPROVE_MERCHANT, 
                "通过商户审核", 
                String.format("审核ID: %d, 商户名称: %s, 备注: %s", 
                        request.getAuditId(), record.getStoreName(), request.getRemark()));

        log.info("商户审核通过成功: auditId={}, merchantId={}", 
                request.getAuditId(), encryptUtils.bytesToUuid(merchantExtend.getMerchantId()));
    }

    /**
     * 拒绝审核
     * 
     * @param request 审核请求
     */
    @Transactional
    public void rejectMerchant(MerchantAuditRejectRequest request) {
        log.info("拒绝商户审核: auditId={}, reason={}", request.getAuditId(), request.getRejectReason());

        // 1. 获取当前管理员ID
        byte[] adminId = encryptUtils.uuidToBytes(AdminContext.getAdminId());

        // 2. 查询审核记录
        MerchantAuditRecord record = auditRecordRepository.findById(request.getAuditId())
                .orElseThrow(() -> new BusinessException(404, "审核记录不存在"));

        // 3. 检查审核状态（0-待审核）
        if (record.getAuditStatus() != 0) {
            throw new BusinessException(400, "该记录不是待审核状态");
        }

        // 4. 更新审核记录
        record.setAuditStatus(2); // 已拒绝
        record.setAuditorId(adminId);
        record.setAuditTime(LocalDateTime.now());
        record.setRejectReason(request.getRejectReason());
        auditRecordRepository.save(record);

        // 5. 更新商户认证状态为审核拒绝（如果已存在商户信息）
        merchantExtendRepository.findByUserId(record.getUserId()).ifPresent(merchant -> {
            merchant.setCertification(4); // 审核拒绝
            merchantExtendRepository.save(merchant);
        });

        // 6. 记录操作日志
        logOperation(AdminConstants.OperationType.REJECT_MERCHANT, 
                "拒绝商户审核", 
                String.format("审核ID: %d, 拒绝原因: %s", request.getAuditId(), request.getRejectReason()));

        log.info("商户审核拒绝成功: auditId={}", request.getAuditId());
    }

    /**
     * 创建第一家店铺
     */
    private void createFirstStore(byte[] merchantId, MerchantAuditRecord record, String userPhone) {
        log.info("为商户创建第一家店铺: merchantId={}", encryptUtils.bytesToUuid(merchantId));
    
        Store store = new Store();
        store.setStoreId(encryptUtils.uuidToBytes(UUID.randomUUID().toString()));
        store.setMerchantId(merchantId);
        store.setStoreName(record.getStoreName());
        store.setAddress(null); // NULL
        store.setStorePhotos(record.getStorePhotos());
        store.setBusinessLicense(record.getBusinessLicense());
        store.setContactPhone(userPhone); // 用户手机号
        store.setContactWx("");          // ← 添加这一行：设置默认值为空字符串
        store.setBusinessTime("");       // ← 添加这一行：设置营业时间为空字符串
        store.setStoreType("其他");      // ← 添加这一行：设置默认店铺类型
        store.setAppointment(0);         // ← 添加这一行：默认不支持预约
        store.setStatus(1);              // 正常营业
        store.setOpenStoreTime(null);    // NULL
        
        storeRepository.save(store);
        log.info("第一家店铺创建成功: storeId={}", encryptUtils.bytesToUuid(store.getStoreId()));
    }

    /**
     * 记录操作日志
     */
    private void logOperation(String operationType, String operationDesc, String operationDetail) {
        try {
            AdminOperationLog log = new AdminOperationLog();
            log.setAdminId(encryptUtils.uuidToBytes(AdminContext.getAdminId()));
            log.setAdminAccount(AdminContext.getAccount());
            log.setOperationType(operationType);
            log.setOperationDesc(operationDesc);
            log.setRequestParams(operationDetail);
            log.setOperationIp(AdminContext.getLoginIp());
            log.setDeviceId(AdminContext.getDeviceId());
            log.setResult(1); // 1 - 成功
            log.setTargetType("MERCHANT");
            
            operationLogRepository.save(log);
        } catch (Exception e) {
            this.log.error("记录操作日志失败", e);
        }
    }

    /**
     * 转换为列表项DTO
     */
    private MerchantAuditListResponse.AuditRecordItem convertToListItem(MerchantAuditRecord record) {
        // 查询用户手机号
        String userPhone = userRepository.findById(record.getUserId())
                .map(user -> maskPhone(user.getPhone()))
                .orElse("未知");

        // 查询审核人账号
        String auditorAccount = null;
        if (record.getAuditorId() != null) {
            auditorAccount = adminRepository.findById(record.getAuditorId())
                    .map(Admin::getAccount)
                    .orElse(null);
        }

        return MerchantAuditListResponse.AuditRecordItem.builder()
                .auditId(record.getAuditId())
                .userId(encryptUtils.bytesToUuid(record.getUserId()))
                .userPhone(userPhone)
                .storeName(record.getStoreName())
                .applicationMethod(record.getApplicationMethod())
                .applicationMethodDesc(getApplicationMethodDesc(record.getApplicationMethod()))
                .auditStatus(record.getAuditStatus())
                .auditStatusDesc(getAuditStatusDesc(record.getAuditStatus()))
                .createTime(record.getCreateTime().format(DATETIME_FORMATTER))
                .auditTime(record.getAuditTime() != null ? record.getAuditTime().format(DATETIME_FORMATTER) : null)
                .auditorAccount(auditorAccount)
                .build();
    }

    /**
     * 手机号脱敏
     */
    private String maskPhone(String phone) {
        if (phone == null || phone.length() < 11) {
            return phone;
        }
        return phone.substring(0, 3) + "****" + phone.substring(7);
    }

    /**
     * 获取申请方式描述
     */
    private String getApplicationMethodDesc(Integer method) {
        if (method == null) return "未知";
        return switch (method) {
            case 1 -> "直接认证";
            case 2 -> "免认证后续补充";
            default -> "未知";
        };
    }

    /**
     * 获取审核状态描述
     */
    private String getAuditStatusDesc(Integer status) {
        if (status == null) return "未知";
        return switch (status) {
            case 0 -> "待审核";
            case 1 -> "已通过";
            case 2 -> "已拒绝";
            default -> "未知";
        };
    }
}

