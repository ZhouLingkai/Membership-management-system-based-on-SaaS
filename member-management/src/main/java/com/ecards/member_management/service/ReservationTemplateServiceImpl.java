package com.ecards.member_management.service;

import com.ecards.member_management.common.ErrorCode;
import com.ecards.member_management.dto.reservation.*;
import com.ecards.member_management.entity.ReservationTemplate;
import com.ecards.member_management.entity.Store;
import com.ecards.member_management.exception.BusinessException;
import com.ecards.member_management.repository.ReservationRecordRepository;
import com.ecards.member_management.repository.ReservationResourceRepository;
import com.ecards.member_management.repository.ReservationTemplateRepository;
import com.ecards.member_management.repository.StoreRepository;
import com.ecards.member_management.utils.*;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * 预约模板Service实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReservationTemplateServiceImpl implements ReservationTemplateService {

    private final ReservationTemplateRepository templateRepository;
    private final StoreRepository storeRepository;
    private final ReservationRecordRepository reservationRecordRepository;
    private final ReservationResourceRepository resourceRepository;
    private final JwtUtils jwtUtils;
    private final EncryptUtils encryptUtils;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    public TemplateQueryResponse queryTemplate(String storeId, String token) {
        log.info("查询预约模板：storeId={}", storeId);

        // 1. 解析令牌
        Claims claims = jwtUtils.parseToken(token);
        Integer tokenType = claims.get("token_type", Integer.class);
        
        // 2. 验证工作令牌
        if (tokenType != 3) {
            throw new BusinessException(ErrorCode.TOKEN_PERMISSION_DENIED, "需要工作令牌");
        }

        String tokenStoreId = claims.get("store_id", String.class);
        if (!storeId.equals(tokenStoreId)) {
            throw new BusinessException(ErrorCode.NOT_MERCHANT_USER, "无权操作该店铺");
        }

        // 3. 验证店铺存在
        byte[] storeIdBytes = encryptUtils.uuidToBytes(storeId);
        Store store = storeRepository.findById(storeIdBytes)
                .orElseThrow(() -> new BusinessException(ErrorCode.STORE_NOT_EXIST));

        // 4. 查询有效模板
        LocalDate currentDate = LocalDate.now();
        Optional<ReservationTemplate> templateOpt = templateRepository.findEffectiveTemplate(storeIdBytes, currentDate);

        if (templateOpt.isEmpty()) {
            return null;
        }

        // 5. 构建响应
        ReservationTemplate template = templateOpt.get();
        TemplateQueryResponse response = new TemplateQueryResponse();
        response.setReserveId(template.getReserveId());
        response.setStoreId(storeId);
        response.setReservationTimeList(template.getReservationTimeList());
        response.setCancelRule(template.getCancelRule());
        response.setAdvanceDays(template.getAdvanceDays());
        response.setForbiddenDays(template.getForbiddenDays());
        response.setCustomizeForbidden(template.getCustomizeForbidden());
        response.setEffectiveStartTime(template.getEffectiveStartTime().format(DATE_FORMATTER));
        response.setEffectiveEndTime(template.getEffectiveEndTime() != null ? 
                template.getEffectiveEndTime().format(DATE_FORMATTER) : null);
        response.setCreateTime(template.getCreateTime().format(DATETIME_FORMATTER));
        response.setUpdateTime(template.getUpdateTime().format(DATETIME_FORMATTER));

        return response;
    }

    @Override
    @Transactional
    public TemplateCreateResponse createTemplate(TemplateCreateRequest request, String token) {
        log.info("创建预约模板：storeId={}", request.getStoreId());

        // 1. 解析令牌并验证权限
        Claims claims = jwtUtils.parseToken(token);
        Integer tokenType = claims.get("token_type", Integer.class);
        String role = claims.get("role", String.class);

        if (tokenType != 3) {
            throw new BusinessException(ErrorCode.TOKEN_PERMISSION_DENIED, "需要工作令牌");
        }

        String tokenStoreId = claims.get("store_id", String.class);
        if (!request.getStoreId().equals(tokenStoreId)) {
            throw new BusinessException(ErrorCode.NOT_MERCHANT_USER, "无权操作该店铺");
        }

        // 仅商家和店长可创建
        if ("STAFF".equals(role)) {
            throw new BusinessException(ErrorCode.TOKEN_PERMISSION_DENIED, "店员无权创建模板");
        }

        // 2. 验证店铺存在
        byte[] storeIdBytes = encryptUtils.uuidToBytes(request.getStoreId());
        storeRepository.findById(storeIdBytes)
                .orElseThrow(() -> new BusinessException(ErrorCode.STORE_NOT_EXIST));

        // 3. 校验店铺是否已存在模板（接口2只能创建第一个模板）
        List<ReservationTemplate> existingTemplates = templateRepository.findByStoreId(storeIdBytes);
        if (!existingTemplates.isEmpty()) {
            throw new BusinessException(ErrorCode.RESERVATION_TEMPLATE_ALREADY_EXISTS);
        }

        // 4. 校验时间段
        TimeSlotValidator.validateTimeSlots(request.getReservationTimeList());

        // 5. 校验取消规则
        CancelRuleValidator.validateCancelRule(request.getCancelRule());

        // 6. 生成生效日期（后端自动生成）
        LocalDate startDate = LocalDate.now(); // 创建时的日期
        LocalDate endDate = null; // 默认为NULL，长期有效

        // 7. 创建模板
        ReservationTemplate template = new ReservationTemplate();
        template.setStoreId(storeIdBytes);
        template.setReservationTimeList(request.getReservationTimeList());
        template.setCancelRule(request.getCancelRule());
        template.setAdvanceDays(request.getAdvanceDays());
        template.setForbiddenDays(request.getForbiddenDays());
        template.setCustomizeForbidden(request.getCustomizeForbidden());
        template.setEffectiveStartTime(startDate);
        template.setEffectiveEndTime(endDate);

        template = templateRepository.save(template);

        // 8. 构建响应
        TemplateCreateResponse response = new TemplateCreateResponse();
        response.setReserveId(template.getReserveId());
        response.setCreateTime(template.getCreateTime().format(DATETIME_FORMATTER));

        return response;
    }

    @Override
    @Transactional
    public TemplateUpdateResponse updateTemplate(Long templateId, TemplateUpdateRequest request, String token) {
        log.info("修改预约模板：templateId={}", templateId);

        // 1. 解析令牌并验证权限
        Claims claims = jwtUtils.parseToken(token);
        Integer tokenType = claims.get("token_type", Integer.class);
        String role = claims.get("role", String.class);

        if (tokenType != 3) {
            throw new BusinessException(ErrorCode.TOKEN_PERMISSION_DENIED, "需要工作令牌");
        }

        // 仅商家和店长可修改
        if ("STAFF".equals(role)) {
            throw new BusinessException(ErrorCode.TOKEN_PERMISSION_DENIED, "店员无权修改模板");
        }

        // 2. 查询模板
        ReservationTemplate template = templateRepository.findById(templateId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESERVATION_TEMPLATE_NOT_FOUND));

        // 3. 验证店铺归属
        String tokenStoreId = claims.get("store_id", String.class);
        byte[] tokenStoreIdBytes = encryptUtils.uuidToBytes(tokenStoreId);
        if (!Arrays.equals(template.getStoreId(), tokenStoreIdBytes)) {
            throw new BusinessException(ErrorCode.NOT_MERCHANT_USER, "无权操作该店铺");
        }

        // 4. 更新字段
        if (request.getReservationTimeList() != null) {
            // 校验时间段格式
            TimeSlotValidator.validateTimeSlots(request.getReservationTimeList());
            
            // 检查是否存在未使用的用户预约记录
            boolean hasUnusedReservations = reservationRecordRepository.existsUnusedUserReservations(templateId);
            if (hasUnusedReservations) {
                throw new BusinessException(ErrorCode.PARAM_ERROR, "存在未使用的预约记录，无法修改时间段");
            }
            
            // 取消线下占用和资源停用记录
            int canceledCount = reservationRecordRepository.cancelOfflineReservations(
                    templateId, LocalDateTime.now());
            log.info("修改时间段，取消了{}条线下占用/资源停用记录", canceledCount);
            
            template.setReservationTimeList(request.getReservationTimeList());
        }

        if (request.getCancelRule() != null) {
            CancelRuleValidator.validateCancelRule(request.getCancelRule());
            template.setCancelRule(request.getCancelRule());
        }

        if (request.getAdvanceDays() != null) {
            template.setAdvanceDays(request.getAdvanceDays());
        }

        if (request.getForbiddenDays() != null) {
            template.setForbiddenDays(request.getForbiddenDays());
        }

        template = templateRepository.save(template);

        // 5. 构建响应
        TemplateUpdateResponse response = new TemplateUpdateResponse();
        response.setReserveId(template.getReserveId());
        response.setUpdateTime(template.getUpdateTime().format(DATETIME_FORMATTER));

        return response;
    }

    @Override
    public InconsistentReservationsResponse queryInconsistentReservations(InconsistentReservationsRequest request, String token) {
        log.info("查询不一致预约记录：storeId={}, startDate={}, endDate={}", 
                request.getStoreId(), request.getStartDate(), request.getEndDate());

        // 1. 解析令牌并验证权限
        Claims claims = jwtUtils.parseToken(token);
        Integer tokenType = claims.get("token_type", Integer.class);

        if (tokenType != 3) {
            throw new BusinessException(ErrorCode.TOKEN_PERMISSION_DENIED, "需要工作令牌");
        }

        // 2. 验证店铺归属
        String tokenStoreId = claims.get("store_id", String.class);
        if (!request.getStoreId().equals(tokenStoreId)) {
            throw new BusinessException(ErrorCode.NOT_MERCHANT_USER, "无权操作该店铺");
        }

        // 3. 校验日期参数
        LocalDate startDate = LocalDate.parse(request.getStartDate(), DATE_FORMATTER);
        LocalDate endDate = LocalDate.parse(request.getEndDate(), DATE_FORMATTER);
        LocalDate today = LocalDate.now();

        if (startDate.isBefore(today)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "开始日期不能早于今天");
        }

        if (endDate.isBefore(startDate)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "结束日期不能早于开始日期");
        }

        long daysBetween = java.time.temporal.ChronoUnit.DAYS.between(startDate, endDate);
        if (daysBetween > 10) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "日期段最多不超过10天");
        }

        // 4. 查询店铺的有效模板
        byte[] storeIdBytes = encryptUtils.uuidToBytes(request.getStoreId());
        Optional<ReservationTemplate> templateOpt = templateRepository.findEffectiveTemplate(storeIdBytes, today);
        if (templateOpt.isEmpty()) {
            // 没有模板，返回空列表
            return new InconsistentReservationsResponse(List.of());
        }

        ReservationTemplate template = templateOpt.get();

        // 5. 查询预约记录
        List<com.ecards.member_management.entity.ReservationRecord> records = 
                reservationRecordRepository.findInconsistentReservations(
                        template.getReserveId(), startDate, endDate);

        // 6. 过滤不一致的记录（那一天不能被预约）
        List<InconsistentReservationsResponse.InconsistentReservationItem> inconsistentItems = new ArrayList<>();
        
        for (com.ecards.member_management.entity.ReservationRecord record : records) {
            // 使用ForbiddenDaysValidator校验该日期是否可预约
            boolean isReservable = ForbiddenDaysValidator.isDateReservable(
                    record.getReservationDate(), template.getForbiddenDays());
            
            // 检查是否超出advanceDays范围
            long daysFromNow = java.time.temporal.ChronoUnit.DAYS.between(today, record.getReservationDate());
            boolean withinAdvanceDays = daysFromNow <= template.getAdvanceDays();

            // 如果不可预约或超出advanceDays，则为不一致记录
            if (!isReservable || !withinAdvanceDays) {
                // 查询资源信息
                com.ecards.member_management.entity.ReservationResource resource = 
                        resourceRepository.findById(record.getResourceId()).orElse(null);

                InconsistentReservationsResponse.InconsistentReservationItem item = 
                        new InconsistentReservationsResponse.InconsistentReservationItem();
                item.setReservationId(record.getReservationId());
                item.setUserId(encryptUtils.bytesToUuid(record.getUserId()));
                item.setUserPhone(record.getUserPhone());
                item.setReservationDate(record.getReservationDate().format(DATE_FORMATTER));
                item.setStartTime(record.getStartTime().toString());
                item.setEndTime(record.getEndTime().toString());
                item.setResourceId(resource != null ? resource.getResourceId().toString() : "");
                item.setResourceName(resource != null ? resource.getResourceName() : "未知资源");

                inconsistentItems.add(item);
            }
        }

        return new InconsistentReservationsResponse(inconsistentItems);
    }
}
