package com.ecards.member_management.service;

import com.ecards.member_management.common.ErrorCode;
import com.ecards.member_management.dto.reservation.*;
import com.ecards.member_management.entity.*;
import com.ecards.member_management.exception.BusinessException;
import com.ecards.member_management.repository.*;
import com.ecards.member_management.utils.*;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 预约资源Service实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReservationResourceServiceImpl implements ReservationResourceService {

    private final ReservationResourceRepository resourceRepository;
    private final ReservationRecordRepository recordRepository;
    private final ReservationTemplateRepository templateRepository;
    private final MemberCardRepository memberCardRepository;
    private final TransactionRecordRepository transactionRepository;
    private final StoreRepository storeRepository;
    private final UserRepository userRepository;
    private final JwtUtils jwtUtils;
    private final EncryptUtils encryptUtils;
    private final ReservationLockUtil lockUtil;

    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    @Override
    public ResourceListResponse queryResourceList(String storeId, String keyword, 
                                                  Integer isReservable, Integer supportCardTypes,
                                                  Integer page, Integer pageSize, String token) {
        log.info("查询资源列表：storeId={}, keyword={}, page={}, pageSize={}", storeId, keyword, page, pageSize);

        // 1. 解析令牌并验证权限
        Claims claims = jwtUtils.parseToken(token);
        Integer tokenType = claims.get("token_type", Integer.class);

        if (tokenType != 3) {
            throw new BusinessException(ErrorCode.TOKEN_PERMISSION_DENIED, "需要工作令牌");
        }

        String tokenStoreId = claims.get("store_id", String.class);
        if (!storeId.equals(tokenStoreId)) {
            throw new BusinessException(ErrorCode.NOT_MERCHANT_USER, "无权操作该店铺");
        }

        // 2. 验证店铺存在
        byte[] storeIdBytes = encryptUtils.uuidToBytes(storeId);
        Store store = storeRepository.findById(storeIdBytes)
                .orElseThrow(() -> new BusinessException(ErrorCode.STORE_NOT_EXIST));

        // 3. 分页查询
        if (page == null || page < 1) page = 1;
        if (pageSize == null || pageSize < 1) pageSize = 50;

        Pageable pageable = PageRequest.of(page - 1, pageSize, Sort.by(Sort.Direction.DESC, "createTime"));
        Page<ReservationResource> resourcePage = resourceRepository.findByConditions(
                storeIdBytes, keyword, isReservable, supportCardTypes, pageable);

        // 4. 构建响应
        List<ResourceListResponse.ResourceItem> items = new ArrayList<>();
        for (ReservationResource resource : resourcePage.getContent()) {
            ResourceListResponse.ResourceItem item = new ResourceListResponse.ResourceItem();
            item.setId(resource.getResourceId());
            item.setResourceName(resource.getResourceName());
            item.setIsReservable(resource.getIsReservable());
            item.setDownTime(resource.getDownTime() != null ? 
                    resource.getDownTime().format(DATETIME_FORMATTER) : null);
            item.setSupportCardTypes(resource.getSupportCardTypes());
            item.setResourceDesc(resource.getResourceDesc());
            item.setResourceImg(resource.getResourceImg());
            item.setUnitPrice(resource.getUnitPrice());
            items.add(item);
        }

        ResourceListResponse response = new ResourceListResponse();
        response.setTotal((int) resourcePage.getTotalElements());
        response.setPage(page);
        response.setPageSize(pageSize);
        response.setList(items);

        return response;
    }

    @Override
    public ResourceDetailResponse queryResourceDetail(Long resourceId, String token) {
        log.info("查询资源详情：resourceId={}", resourceId);

        // 1. 解析令牌并验证权限
        Claims claims = jwtUtils.parseToken(token);
        Integer tokenType = claims.get("token_type", Integer.class);

        if (tokenType != 3) {
            throw new BusinessException(ErrorCode.TOKEN_PERMISSION_DENIED, "需要工作令牌");
        }

        // 2. 查询资源
        ReservationResource resource = resourceRepository.findByResourceId(resourceId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESERVATION_RESOURCE_NOT_FOUND));

        // 3. 验证店铺归属
        String tokenStoreId = claims.get("store_id", String.class);
        byte[] tokenStoreIdBytes = encryptUtils.uuidToBytes(tokenStoreId);
        if (!Arrays.equals(resource.getStoreId(), tokenStoreIdBytes)) {
            throw new BusinessException(ErrorCode.NOT_MERCHANT_USER, "无权操作该店铺");
        }

        // 4. 查询当前有效的模板
        LocalDate today = LocalDate.now();
        ReservationTemplate template = templateRepository.findEffectiveTemplate(resource.getStoreId(), today)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESERVATION_TEMPLATE_NOT_FOUND, 
                        "该店铺暂无有效的预约模板"));

        // 5. 构建响应
        ResourceDetailResponse response = new ResourceDetailResponse();
        response.setId(resource.getResourceId());
        response.setStoreId(encryptUtils.bytesToUuid(resource.getStoreId()));
        response.setResourceName(resource.getResourceName());
        response.setIsReservable(resource.getIsReservable());
        response.setSupportCardTypes(resource.getSupportCardTypes());
        response.setMinContinuousTime(resource.getMinContinuousTime());
        response.setMaxContinuousTime(resource.getMaxContinuousTime());
        response.setUnitPrice(resource.getUnitPrice());
        response.setResourceImg(resource.getResourceImg());
        response.setResourceDesc(resource.getResourceDesc());
        response.setDownTime(resource.getDownTime() != null ? 
                resource.getDownTime().format(DATETIME_FORMATTER) : null);
        response.setCreateTime(resource.getCreateTime().format(DATETIME_FORMATTER));
        response.setUpdateTime(resource.getUpdateTime().format(DATETIME_FORMATTER));

        // 6. 添加模板相关信息
        response.setTemplateForbiddenDays(template.getForbiddenDays());
        response.setCustomizeForbidden(template.getCustomizeForbidden());
        
        // 7. 如果支持自定义禁止日期，返回资源的forbidden_days
        if (template.getCustomizeForbidden() == 1) {
            response.setResourceForbiddenDays(resource.getForbiddenDays());
        }

        // 8. 添加优惠策略（仅余额卡资源返回）
        if (resource.getSupportCardTypes() == 1) {
            response.setPromotionStrategy(resource.getPromotionStrategy());
        }

        return response;
    }

    @Override
    @Transactional
    public ResourceCreateResponse createResource(ResourceCreateRequest request, String token) {
        log.info("创建资源：storeId={}, 数量={}", request.getStoreId(), request.getResources().size());

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
            throw new BusinessException(ErrorCode.TOKEN_PERMISSION_DENIED, "店员无权创建资源");
        }

        // 2. 验证店铺存在
        byte[] storeIdBytes = encryptUtils.uuidToBytes(request.getStoreId());
        Store store = storeRepository.findById(storeIdBytes)
                .orElseThrow(() -> new BusinessException(ErrorCode.STORE_NOT_EXIST));

        // 3. 批量创建
        List<ResourceCreateResponse.CreatedResource> createdList = new ArrayList<>();
        LocalDateTime createTime = LocalDateTime.now();

        for (ResourceCreateRequest.ResourceItem item : request.getResources()) {
            // 校验资源名称唯一性
            boolean exists = resourceRepository.existsByStoreIdAndResourceName(
                    storeIdBytes, item.getResourceName(), null);
            if (exists) {
                throw new BusinessException(ErrorCode.RESERVATION_RESOURCE_NAME_DUPLICATE, 
                        "资源名称重复：" + item.getResourceName());
            }

            // 校验连续时间
            ResourceValidator.validateContinuousTime(item.getMinContinuousTime(), item.getMaxContinuousTime());

            // 校验次数卡规则
            ResourceValidator.validateTimesCardResource(item.getMinContinuousTime(), 
                    item.getMaxContinuousTime(), item.getSupportCardTypes());

            // 创建资源
            ReservationResource resource = new ReservationResource();
            resource.setStoreId(storeIdBytes);
            resource.setResourceName(item.getResourceName());
            resource.setIsReservable(1);
            resource.setSupportCardTypes(item.getSupportCardTypes());
            resource.setMinContinuousTime(item.getMinContinuousTime());
            resource.setMaxContinuousTime(item.getMaxContinuousTime());
            resource.setUnitPrice(item.getUnitPrice());
            resource.setResourceImg(item.getResourceImg());
            resource.setResourceDesc(item.getResourceDesc());
            resource.setForbiddenDays(item.getForbiddenDays());

            resource = resourceRepository.save(resource);

            ResourceCreateResponse.CreatedResource created = new ResourceCreateResponse.CreatedResource();
            created.setId(resource.getResourceId());
            created.setResourceName(resource.getResourceName());
            createdList.add(created);
        }

        // 4. 构建响应
        ResourceCreateResponse response = new ResourceCreateResponse();
        response.setSuccessCount(createdList.size());
        response.setCreatedResources(createdList);
        response.setCreateTime(createTime.format(DATETIME_FORMATTER));

        return response;
    }

    @Override
    @Transactional
    public ResourceUpdateResponse updateResource(Long resourceId, ResourceUpdateRequest request, String token) {
        log.info("修改资源：resourceId={}", resourceId);

        // 1. 解析令牌并验证权限
        Claims claims = jwtUtils.parseToken(token);
        Integer tokenType = claims.get("token_type", Integer.class);
        String role = claims.get("role", String.class);

        if (tokenType != 3) {
            throw new BusinessException(ErrorCode.TOKEN_PERMISSION_DENIED, "需要工作令牌");
        }

        // 仅商家和店长可修改
        if ("STAFF".equals(role)) {
            throw new BusinessException(ErrorCode.TOKEN_PERMISSION_DENIED, "店员无权修改资源");
        }

        // 2. 查询资源
        ReservationResource resource = resourceRepository.findByResourceId(resourceId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESERVATION_RESOURCE_NOT_FOUND));

        // 3. 验证店铺归属
        String tokenStoreId = claims.get("store_id", String.class);
        byte[] tokenStoreIdBytes = encryptUtils.uuidToBytes(tokenStoreId);
        if (!Arrays.equals(resource.getStoreId(), tokenStoreIdBytes)) {
            throw new BusinessException(ErrorCode.NOT_MERCHANT_USER, "无权操作该店铺");
        }

        // 4. 更新字段
        if (request.getResourceName() != null) {
            boolean exists = resourceRepository.existsByStoreIdAndResourceName(
                    resource.getStoreId(), request.getResourceName(), resourceId);
            if (exists) {
                throw new BusinessException(ErrorCode.RESERVATION_RESOURCE_NAME_DUPLICATE, "资源名称重复");
            }
            resource.setResourceName(request.getResourceName());
        }

        if (request.getMinContinuousTime() != null || request.getMaxContinuousTime() != null) {
            Integer minTime = request.getMinContinuousTime() != null ? 
                    request.getMinContinuousTime() : resource.getMinContinuousTime();
            Integer maxTime = request.getMaxContinuousTime() != null ? 
                    request.getMaxContinuousTime() : resource.getMaxContinuousTime();

            ResourceValidator.validateContinuousTime(minTime, maxTime);
            ResourceValidator.validateTimesCardResource(minTime, maxTime, resource.getSupportCardTypes());

            if (request.getMinContinuousTime() != null) {
                resource.setMinContinuousTime(minTime);
            }
            if (request.getMaxContinuousTime() != null) {
                resource.setMaxContinuousTime(maxTime);
            }
        }

        if (request.getUnitPrice() != null) {
            resource.setUnitPrice(request.getUnitPrice());
        }

        if (request.getResourceImg() != null) {
            resource.setResourceImg(request.getResourceImg());
        }

        if (request.getResourceDesc() != null) {
            resource.setResourceDesc(request.getResourceDesc());
        }

        if (request.getForbiddenDays() != null) {
            resource.setForbiddenDays(request.getForbiddenDays());
        }

        resource = resourceRepository.save(resource);

        // 5. 构建响应
        ResourceUpdateResponse response = new ResourceUpdateResponse();
        response.setId(resource.getResourceId());
        response.setUpdateTime(resource.getUpdateTime().format(DATETIME_FORMATTER));

        return response;
    }

    @Override
    @Transactional
    public ResourceDeleteResponse deleteResource(Long resourceId, String token) {
        log.info("删除资源：resourceId={}", resourceId);

        // 1. 解析令牌并验证权限
        Claims claims = jwtUtils.parseToken(token);
        Integer tokenType = claims.get("token_type", Integer.class);
        String role = claims.get("role", String.class);

        if (tokenType != 3) {
            throw new BusinessException(ErrorCode.TOKEN_PERMISSION_DENIED, "需要工作令牌");
        }

        // 仅商家和店长可删除
        if ("STAFF".equals(role)) {
            throw new BusinessException(ErrorCode.TOKEN_PERMISSION_DENIED, "店员无权删除资源");
        }

        // 2. 查询资源
        ReservationResource resource = resourceRepository.findByResourceId(resourceId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESERVATION_RESOURCE_NOT_FOUND));

        // 3. 验证店铺归属
        String tokenStoreId = claims.get("store_id", String.class);
        byte[] tokenStoreIdBytes = encryptUtils.uuidToBytes(tokenStoreId);
        if (!Arrays.equals(resource.getStoreId(), tokenStoreIdBytes)) {
            throw new BusinessException(ErrorCode.NOT_MERCHANT_USER, "无权操作该店铺");
        }

        // 4. 校验删除条件
        // 必须处于停用状态
        if (resource.getIsReservable() != 0) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "资源必须先停用才能删除");
        }

        // 停用时间必须≥7天
        if (resource.getDownTime() == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "资源未停用，无法删除");
        }

        long daysSinceDown = Duration.between(resource.getDownTime(), LocalDateTime.now()).toDays();
        if (daysSinceDown < 7) {
            throw new BusinessException(ErrorCode.RESERVATION_RESOURCE_NOT_DISABLED_LONG, 
                    "资源停用未满7天，无法删除");
        }

        // 不存在未完成的预约记录
        boolean hasPending = recordRepository.existsPendingReservations(resourceId);
        if (hasPending) {
            throw new BusinessException(ErrorCode.RESERVATION_RESOURCE_PENDING, 
                    "资源存在未完成的预约记录，无法删除");
        }

        // 5. 物理删除
        resourceRepository.delete(resource);

        // 6. 构建响应
        ResourceDeleteResponse response = new ResourceDeleteResponse();
        response.setId(resourceId);
        response.setDeleteTime(LocalDateTime.now().format(DATETIME_FORMATTER));

        return response;
    }

    // ==================== 接口9：启停用资源 ====================
    @Override
    @Transactional
    public ResourceToggleResponse toggleResource(Long resourceId, ResourceToggleRequest request, String token) {
        log.info("启停用资源：resourceId={}, isReservable={}", resourceId, request.getIsReservable());

        // 1. 解析令牌并验证权限
        Claims claims = jwtUtils.parseToken(token);
        Integer tokenType = claims.get("token_type", Integer.class);
        String role = claims.get("role", String.class);

        // 工作令牌：商家或店长可操作
        if (tokenType == 2 && "STAFF".equals(role)) {
            throw new BusinessException(ErrorCode.TOKEN_PERMISSION_DENIED, "店员无权启停用资源");
        }

        // 2. 查询资源
        ReservationResource resource = resourceRepository.findByResourceId(resourceId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESERVATION_RESOURCE_NOT_FOUND));

        // 3. 验证店铺归属
        String tokenStoreId = claims.get("store_id", String.class);
        byte[] tokenStoreIdBytes = encryptUtils.uuidToBytes(tokenStoreId);
        if (!Arrays.equals(resource.getStoreId(), tokenStoreIdBytes)) {
            throw new BusinessException(ErrorCode.NOT_MERCHANT_USER, "无权操作该店铺");
        }

        // 4. 更新状态
        resource.setIsReservable(request.getIsReservable());
        if (request.getIsReservable() == 0) {
            // 停用时记录停用时间
            resource.setDownTime(LocalDateTime.now());
        } else {
            // 启用时清空停用时间
            resource.setDownTime(null);
        }
        resource.setUpdateTime(LocalDateTime.now());
        resourceRepository.save(resource);

        // 5. 构建响应
        ResourceToggleResponse response = new ResourceToggleResponse();
        response.setId(resourceId);
        response.setIsReservable(resource.getIsReservable());
        response.setDownTime(resource.getDownTime() != null ? 
                resource.getDownTime().format(DATETIME_FORMATTER) : null);
        response.setUpdateTime(resource.getUpdateTime().format(DATETIME_FORMATTER));

        return response;
    }

    // ==================== 接口10：查询某日预约情况 ====================
    @Override
    public ReservationQueryResponse queryReservations(ReservationQueryRequest request, String token) {
        log.info("查询某日预约情况：storeId={}, requestDate={}", request.getStoreId(), request.getRequestDate());

        // 1. 解析令牌
        Claims claims = jwtUtils.parseToken(token);
        Integer tokenType = claims.get("token_type", Integer.class);
        boolean isWorkToken = (tokenType == 2);

        // 2. 验证店铺
        byte[] storeIdBytes = encryptUtils.uuidToBytes(request.getStoreId());
        Store store = storeRepository.findById(storeIdBytes)
                .orElseThrow(() -> new BusinessException(ErrorCode.STORE_NOT_FOUND));

        // 3. 工作令牌：验证店铺归属
        if (isWorkToken) {
            String role = claims.get("role", String.class);
            String tokenStoreId = claims.get("store_id", String.class);
            byte[] tokenStoreIdBytes = encryptUtils.uuidToBytes(tokenStoreId);
            
            // 商家或员工都需要验证店铺归属
            if (!Arrays.equals(storeIdBytes, tokenStoreIdBytes)) {
                throw new BusinessException(ErrorCode.NOT_MERCHANT_USER, "无权查询该店铺的预约情况");
            }
        }

        // 4. 查询模板
        LocalDate queryDate = LocalDate.parse(request.getRequestDate(), DATE_FORMATTER);
        ReservationTemplate template = templateRepository.findEffectiveTemplate(storeIdBytes, queryDate)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESERVATION_TEMPLATE_NOT_FOUND));

        // 5. 构造forbiddenDays列表（从今日到可预约最后一天，仅考虑模板）
        LocalDate today = LocalDate.now();
        int advanceDays = template.getAdvanceDays();
        LocalDate lastReservableDate = today.plusDays(advanceDays);
        List<String> forbiddenDaysInRange = new ArrayList<>();
        
        if (template.getForbiddenDays() != null) {
            for (String day : template.getForbiddenDays()) {
                if (day.startsWith("周")) {
                    forbiddenDaysInRange.add(day);
                } else {
                    try {
                        LocalDate date = LocalDate.parse(day, DATE_FORMATTER);
                        if (!date.isBefore(today) && !date.isAfter(lastReservableDate)) {
                            forbiddenDaysInRange.add(day);
                        }
                    } catch (Exception e) {
                        // 忽略格式错误的日期
                    }
                }
            }
        }

        // 6. 查询资源列表
        List<ReservationResource> resources;
        if (request.getKeyword() != null && !request.getKeyword().isEmpty()) {
            // 有关键词筛选
            Page<ReservationResource> page = resourceRepository.findByConditions(
                    storeIdBytes, request.getKeyword(), null, null, 
                    PageRequest.of(0, 1000));
            resources = page.getContent();
        } else {
            // 无关键词，查询所有
            Page<ReservationResource> page = resourceRepository.findByConditions(
                    storeIdBytes, null, null, null, 
                    PageRequest.of(0, 1000));
            resources = page.getContent();
        }

        // 7. 查询该日所有预约记录
        List<ReservationRecord> allRecords = recordRepository.findStoreReservationsByDate(storeIdBytes, queryDate);

        // 8. 构建响应
        ReservationQueryResponse response = new ReservationQueryResponse();
        response.setRealTime(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")));
        response.setQueryDate(request.getRequestDate());
        response.setAdvanceDays(template.getAdvanceDays());
        response.setForbiddenDays(forbiddenDaysInRange);
        response.setTimeList(template.getReservationTimeList());
        response.setCancelRule(template.getCancelRule());

        List<ReservationQueryResponse.ResourceDetail> resourceDetails = new ArrayList<>();
        for (ReservationResource resource : resources) {
            // 检查该日期该资源是否可预约（综合判断）
            boolean dateReservable = ForbiddenDaysValidator.isDateReservableWithCustomize(
                    queryDate,
                    template.getForbiddenDays(),
                    resource.getForbiddenDays(),
                    template.getCustomizeForbidden()
            );

            ReservationQueryResponse.ResourceDetail detail = new ReservationQueryResponse.ResourceDetail();
            detail.setResourceId(resource.getResourceId());
            detail.setResourceName(resource.getResourceName());
            detail.setResourceDesc(resource.getResourceDesc());
            detail.setResourceImg(resource.getResourceImg());
            
            // 综合判断isReservable：资源启用状态 && 日期可预约
            int finalIsReservable = (resource.getIsReservable() == 1 && dateReservable) ? 1 : 0;
            detail.setIsReservable(finalIsReservable);

            // 资源限制
            ReservationQueryResponse.ResourceRestriction restriction = 
                    new ReservationQueryResponse.ResourceRestriction();
            restriction.setMinContinuousTime(resource.getMinContinuousTime());
            restriction.setMaxContinuousTime(resource.getMaxContinuousTime());
            restriction.setSupportCardTypes(resource.getSupportCardTypes());
            restriction.setUnitPrice(resource.getUnitPrice());
            detail.setResourceRestriction(restriction);

            // 优惠策略信息（仅余额卡资源返回）
            if (resource.getSupportCardTypes() == 1) {
                ReservationQueryResponse.DiscountInfo discountInfo = buildDiscountInfo(
                        resource.getPromotionStrategy(), queryDate);
                detail.setDiscount(discountInfo);
            }

            // 预约列表
            List<ReservationQueryResponse.ReservationItem> reservationList = new ArrayList<>();
            for (ReservationRecord record : allRecords) {
                if (record.getResourceId().equals(resource.getResourceId())) {
                    ReservationQueryResponse.ReservationItem item = 
                            new ReservationQueryResponse.ReservationItem();
                    item.setStartTime(record.getStartTime().format(TIME_FORMATTER));
                    item.setEndTime(record.getEndTime().format(TIME_FORMATTER));
                    item.setOperateType(record.getOperateType());

                    // 工作令牌返回更多信息
                    if (isWorkToken) {
                        ReservationQueryResponse.MoreInfo moreInfo = 
                                new ReservationQueryResponse.MoreInfo();
                        moreInfo.setUserId(encryptUtils.bytesToUuid(record.getUserId()));
                        moreInfo.setUserPhone(record.getUserPhone());
                        moreInfo.setTransactionId(record.getTransactionId());
                        moreInfo.setRemark(record.getRemark());
                        item.setMoreInfo(moreInfo);
                    }

                    reservationList.add(item);
                }
            }
            detail.setReservationList(reservationList);

            resourceDetails.add(detail);
        }

        response.setResourceDetails(resourceDetails);
        return response;
    }

    // ==================== 接口11：获取预约列表 ====================
    @Override
    public MyReservationsResponse getMyReservations(MyReservationsRequest request, String token) {
        log.info("获取预约列表：status={}, page={}", request.getStatus(), request.getPage());

        // 1. 解析令牌（仅普通令牌）
        Claims claims = jwtUtils.parseToken(token);
        Integer tokenType = claims.get("token_type", Integer.class);
        if (tokenType != 1) {
            throw new BusinessException(ErrorCode.TOKEN_PERMISSION_DENIED, "仅支持普通令牌");
        }

        String userId = claims.get("user_id", String.class);
        byte[] userIdBytes = encryptUtils.uuidToBytes(userId);

        // 2. 参数处理
        int page = request.getPage() != null && request.getPage() > 0 ? request.getPage() : 1;
        int pageSize = request.getPageSize() != null && request.getPageSize() > 0 ? request.getPageSize() : 20;

        LocalDate startDate = request.getStartDate() != null ? 
                LocalDate.parse(request.getStartDate(), DATE_FORMATTER) : null;
        LocalDate endDate = request.getEndDate() != null ? 
                LocalDate.parse(request.getEndDate(), DATE_FORMATTER) : null;

        // 3. 分页查询
        Pageable pageable = PageRequest.of(page - 1, pageSize);
        Page<ReservationRecord> recordPage = recordRepository.findUserReservations(
                userIdBytes, request.getStatus(), startDate, endDate, pageable);

        // 4. 构建响应
        MyReservationsResponse response = new MyReservationsResponse();
        response.setTotal((int) recordPage.getTotalElements());
        response.setPage(page);
        response.setPageSize(pageSize);

        List<MyReservationsResponse.ReservationItem> list = new ArrayList<>();
        for (ReservationRecord record : recordPage.getContent()) {
            // 查询资源和店铺信息
            ReservationResource resource = resourceRepository.findByResourceId(record.getResourceId())
                    .orElse(null);
            Store store = resource != null ? storeRepository.findById(resource.getStoreId()).orElse(null) : null;

            MyReservationsResponse.ReservationItem item = new MyReservationsResponse.ReservationItem();
            item.setReservationId(record.getReservationId());
            item.setResourceId(record.getResourceId());
            item.setResourceName(resource != null ? resource.getResourceName() : "未知资源");
            item.setStoreName(store != null ? store.getStoreName() : "未知店铺");
            item.setReservationDate(record.getReservationDate().format(DATE_FORMATTER));
            item.setStartTime(record.getStartTime().format(TIME_FORMATTER));
            item.setEndTime(record.getEndTime().format(TIME_FORMATTER));
            item.setOperateType(record.getOperateType());
            item.setTransactionAmount(record.getTransactionAmount());
            item.setReservationStatus(record.getReservationStatus());
            item.setRemark(record.getRemark());
            item.setCreateTime(record.getCreateTime().format(DATETIME_FORMATTER));

            list.add(item);
        }

        response.setList(list);
        return response;
    }

    // ==================== 接口12：预约资源 ====================
    @Override
    @Transactional
    public ReserveResourceResponse reserveResource(ReserveResourceRequest request, String token) {
        log.info("预约资源：resourceId={}, date={}, time={}-{}", 
                request.getResourceId(), request.getReservationDate(), 
                request.getStartTime(), request.getEndTime());

        // 1. 解析令牌
        Claims claims = jwtUtils.parseToken(token);
        Integer tokenType = claims.get("token_type", Integer.class);
        String userId = claims.get("user_id", String.class);
        byte[] userIdBytes = encryptUtils.uuidToBytes(userId);

        // 2. 校验令牌类型：只允许普通令牌
        if (tokenType != 1) {
            throw new BusinessException(ErrorCode.TOKEN_PERMISSION_DENIED, "预约资源只能使用普通令牌");
        }

        // 3. 查询资源
        ReservationResource resource = resourceRepository.findByResourceId(request.getResourceId())
                .orElseThrow(() -> new BusinessException(ErrorCode.RESERVATION_RESOURCE_NOT_FOUND));

        // 3.1 校验店铺ID是否匹配
        byte[] requestStoreIdBytes = encryptUtils.uuidToBytes(request.getStoreId());
        if (!Arrays.equals(resource.getStoreId(), requestStoreIdBytes)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "资源不属于指定店铺");
        }

        // 4. 查询模板
        LocalDate reservationDate = LocalDate.parse(request.getReservationDate(), DATE_FORMATTER);
        ReservationTemplate template = templateRepository.findEffectiveTemplate(resource.getStoreId(), reservationDate)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESERVATION_TEMPLATE_NOT_FOUND));

        // 5. 校验预约日期是否在可预约范围内
        LocalDate today = LocalDate.now();
        long daysFromToday = java.time.temporal.ChronoUnit.DAYS.between(today, reservationDate);
        if (daysFromToday < 0 || daysFromToday > template.getAdvanceDays()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "预约日期超出可预约范围");
        }

        // 6. 校验日期是否可预约
        boolean dateReservable = ForbiddenDaysValidator.isDateReservableWithCustomize(
                reservationDate,
                template.getForbiddenDays(),
                resource.getForbiddenDays(),
                template.getCustomizeForbidden()
        );
        if (!dateReservable) {
            throw new BusinessException(ErrorCode.RESERVATION_DATE_FORBIDDEN);
        }

        // 7. 校验时间段合法性
        String timeSlotsError = com.ecards.member_management.validator.TimeSlotValidator.validateTimeSlots(
                request.getTimeSlots(), 
                template.getReservationTimeList()
        );
        if (timeSlotsError != null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, timeSlotsError);
        }

        // 7.1 校验预约时间是否过期（仅当预约日期为当天时）
        String timeExpiredError = com.ecards.member_management.validator.TimeSlotValidator.validateTimeNotExpired(
                request.getTimeSlots(),
                reservationDate
        );
        if (timeExpiredError != null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, timeExpiredError);
        }

        // 8. 从timeSlots提取startTime和endTime（用于数据库存储）
        String startTimeStr = com.ecards.member_management.validator.TimeSlotValidator.extractOverallStartTime(request.getTimeSlots());
        String endTimeStr = com.ecards.member_management.validator.TimeSlotValidator.extractOverallEndTime(request.getTimeSlots());
        LocalTime startTime = LocalTime.parse(startTimeStr, TIME_FORMATTER);
        LocalTime endTime = LocalTime.parse(endTimeStr, TIME_FORMATTER);
        
        // 设置到request中（用于后续存储）
        request.setStartTime(startTimeStr);
        request.setEndTime(endTimeStr);

        // 9. 计算总时长（各时间段长度之和）
        int duration = com.ecards.member_management.validator.TimeSlotValidator.calculateTotalDuration(request.getTimeSlots());
        if (duration < resource.getMinContinuousTime() || duration > resource.getMaxContinuousTime()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, 
                    String.format("预约时长必须在%d-%d分钟之间", 
                            resource.getMinContinuousTime(), resource.getMaxContinuousTime()));
        }

        // 10. 查询会员卡及卡类型（优化：使用JOIN查询，2次合并为1次）
        byte[] memberCardIdBytes = encryptUtils.uuidToBytes(request.getMemberCardId());
        MemberCard memberCard = memberCardRepository.findByMemberCardIdWithType(memberCardIdBytes)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_CARD_NOT_FOUND));

        // 11. 校验会员卡归属：必须是用户本人的卡
        if (!Arrays.equals(memberCard.getUserId(), userIdBytes)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "只能使用本人的会员卡进行预约");
        }

        // 12. 校验会员卡类型
        if ((resource.getSupportCardTypes() & memberCard.getCardTtype()) == 0) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "该资源不支持此类型会员卡");
        }

        // 13. 计算费用（第一阶段，无状态计算）
        BigDecimal amount;
        if (resource.getSupportCardTypes() == 1) { // 余额卡 - 使用优惠策略计算
            amount = calculateDiscountedPrice(
                request.getTimeSlots(),
                resource.getPromotionStrategy(),
                resource.getUnitPrice(),
                reservationDate
            );
        } else { // 次数卡 - 按原价计算
            amount = resource.getUnitPrice()
                    .multiply(BigDecimal.valueOf(duration))
                    .divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP);
        }

        // 14. 校验余额/次数是否充足（第一阶段，快速失败）
        if (memberCard.getCardTtype() == 1) { // 余额卡
            BigDecimal newBalance = memberCard.getBalance().subtract(amount);
            if (newBalance.compareTo(BigDecimal.ZERO) < 0) {
                throw new BusinessException(ErrorCode.BALANCE_INSUFFICIENT, 
                        String.format("余额不足，需要%.2f元，当前余额%.2f元", 
                                amount, memberCard.getBalance()));
            }
        } else if (memberCard.getCardTtype() == 2) { // 次数卡
            int requiredTimes = (int) Math.ceil(amount.doubleValue());
            int currentTimes = memberCard.getTimes();
            if (currentTimes < requiredTimes) {
                throw new BusinessException(ErrorCode.TIMES_INSUFFICIENT,
                        String.format("次数不足，需要%d次，当前剩余%d次", 
                                requiredTimes, currentTimes));
            }
        }

        // 15. 使用分布式锁执行预约
        String lockKey = lockUtil.generateLockKey(reservationDate, 
                encryptUtils.bytesToUuid(resource.getStoreId()), 
                resource.getResourceId());

        return lockUtil.executeWithLock(lockKey, 3, 10, () -> {
            // 16. 检查时间段冲突（第二阶段，有状态校验）
            boolean hasConflict = recordRepository.existsTimeConflict(
                    request.getResourceId(), reservationDate, startTime, endTime);
            if (hasConflict) {
                throw new BusinessException(ErrorCode.RESERVATION_TIMESLOT_OCCUPIED);
            }

            // 17. 创建交易记录（扣款）
            TransactionRecord transaction = new TransactionRecord();
            transaction.setMemberCardId(memberCardIdBytes);
            transaction.setUserId(userIdBytes);
            transaction.setMerchantId(memberCard.getMerchantId());
            transaction.setTransactionType(2); // 消费
            transaction.setAmount(amount.negate()); // 负数表示支出
            
            // 更新余额快照（已在第一阶段校验过，这里直接扣款）
            if (memberCard.getCardTtype() == 1) { // 余额卡
                BigDecimal newBalance = memberCard.getBalance().subtract(amount);
                transaction.setBalanceSnapshot(newBalance);
                memberCard.setBalance(newBalance);
            } else if (memberCard.getCardTtype() == 2) { // 次数卡
                int requiredTimes = (int) Math.ceil(amount.doubleValue());
                int newTimes = memberCard.getTimes() - requiredTimes;
                transaction.setBalanceSnapshot(BigDecimal.valueOf(newTimes));
                memberCard.setTimes(newTimes);
            }

            transaction.setOperatorId(userIdBytes);
            transaction.setTransStoreId(resource.getStoreId());
            transaction.setRemark(request.getRemark() != null ? request.getRemark() : "预约资源");
            transaction.setTransactionTime(LocalDateTime.now());
            transactionRepository.save(transaction);
            memberCardRepository.save(memberCard);

            // 18. 创建预约记录
            ReservationRecord record = new ReservationRecord();
            record.setResourceId(request.getResourceId());
            record.setTemplateId(template.getReserveId());
            record.setReservationDate(reservationDate);
            record.setStartTime(startTime);
            record.setEndTime(endTime);
            record.setOperateType(1); // 接口12固定为1（用户预约）
            record.setUserId(userIdBytes);
            
            // 通过 user_id 查询用户手机号
            String userPhone = userRepository.findPhoneByUserId(userIdBytes)
                    .orElseThrow(() -> new BusinessException(ErrorCode.PARAM_ERROR, "用户信息不完整，缺少手机号"));
            record.setUserPhone(userPhone);

            record.setTransactionId(transaction.getTransactionId());
            record.setTransactionAmount(amount);
            record.setReservationStatus(0); // 未使用
            record.setRemark(request.getRemark());
            record.setCreateTime(LocalDateTime.now());
            record.setUpdateTime(LocalDateTime.now());
            recordRepository.save(record);

            // 19. 构建响应
            ReserveResourceResponse response = new ReserveResourceResponse();
            response.setReservationId(record.getReservationId());
            response.setResourceId(resource.getResourceId());
            response.setResourceName(resource.getResourceName());
            response.setReservationDate(request.getReservationDate());
            response.setStartTime(request.getStartTime());
            response.setEndTime(request.getEndTime());
            response.setOperateType(1); // 接口12固定为1（用户预约）
            response.setTransactionId(transaction.getTransactionId());
            response.setTransactionAmount(amount);
            response.setCreateTime(record.getCreateTime().format(DATETIME_FORMATTER));

            return response;
        });
    }

    // ==================== 接口13：取消预约资源 ====================
    @Override
    @Transactional
    public CancelReservationResponse cancelReservation(CancelReservationRequest request, String token) {
        log.info("取消预约：reservationId={}", request.getReservationId());

        // 1. 解析令牌（仅普通令牌）
        Claims claims = jwtUtils.parseToken(token);
        Integer tokenType = claims.get("token_type", Integer.class);
        if (tokenType != 1) {
            throw new BusinessException(ErrorCode.TOKEN_PERMISSION_DENIED, "仅支持普通令牌");
        }

        String userId = claims.get("user_id", String.class);
        byte[] userIdBytes = encryptUtils.uuidToBytes(userId);

        // 2. 查询预约记录
        ReservationRecord record = recordRepository.findByIdAndUserId(
                request.getReservationId(), userIdBytes)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESERVATION_NOT_FOUND));

        // 3. 校验预约状态
        if (record.getReservationStatus() != 0) {
            throw new BusinessException(ErrorCode.RESERVATION_NOT_FOUND, "预约不存在或已取消");
        }

        // 4. 查询模板获取取消规则
        ReservationResource resource = resourceRepository.findByResourceId(record.getResourceId())
                .orElseThrow(() -> new BusinessException(ErrorCode.RESERVATION_RESOURCE_NOT_FOUND));
        ReservationTemplate template = templateRepository.findEffectiveTemplate(
                resource.getStoreId(), record.getReservationDate())
                .orElseThrow(() -> new BusinessException(ErrorCode.RESERVATION_TEMPLATE_NOT_FOUND));

        // 5. 计算距离预约开始时间的分钟数
        LocalDateTime reservationStartTime = LocalDateTime.of(
                record.getReservationDate(), record.getStartTime());
        long minutesToStart = java.time.Duration.between(LocalDateTime.now(), reservationStartTime).toMinutes();
        
        // 6. 校验是否已经开始（用户取消不允许已开始的预约）
        if (minutesToStart < 0) {
            throw new BusinessException(ErrorCode.RESERVATION_EXPIRED);
        }

        // 7. 计算违约费（使用工具类）
        CancelRuleCalculator.PenaltyResult penaltyResult = CancelRuleCalculator.calculatePenalty(
                minutesToStart,
                template.getCancelRule(),
                record.getTransactionAmount(),
                false  // 用户取消不允许已开始的预约
        );
        BigDecimal penaltyAmount = penaltyResult.getPenaltyAmount();
        String penaltyRule = penaltyResult.getPenaltyRule();

        // 8. 计算退款金额
        BigDecimal refundAmount = record.getTransactionAmount().subtract(penaltyAmount);

        // 8. 查询原交易记录和会员卡
        TransactionRecord originalTransaction = transactionRepository.findById(record.getTransactionId())
                .orElseThrow(() -> new BusinessException(ErrorCode.SYSTEM_ERROR, "原交易记录不存在"));
        MemberCard memberCard = memberCardRepository.findByMemberCardId(originalTransaction.getMemberCardId())
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_CARD_NOT_FOUND));

        // 9. 创建退款交易记录
        TransactionRecord refundTransaction = new TransactionRecord();
        refundTransaction.setMemberCardId(originalTransaction.getMemberCardId());
        refundTransaction.setUserId(userIdBytes);
        refundTransaction.setMerchantId(originalTransaction.getMerchantId());
        refundTransaction.setTransactionType(3);
        refundTransaction.setAmount(refundAmount);
        
        if (memberCard.getCardTtype() == 1) {
            BigDecimal newBalance = memberCard.getBalance().add(refundAmount);
            refundTransaction.setBalanceSnapshot(newBalance);
            memberCard.setBalance(newBalance);
        } else if (memberCard.getCardTtype() == 2) {
            int refundTimes = (int) Math.ceil(refundAmount.doubleValue());
            int newTimes = memberCard.getTimes() + refundTimes;
            refundTransaction.setBalanceSnapshot(BigDecimal.valueOf(newTimes));
            memberCard.setTimes(newTimes);
        }

        refundTransaction.setOperatorId(userIdBytes);
        refundTransaction.setTransStoreId(resource.getStoreId());
        refundTransaction.setRemark(penaltyRule);
        refundTransaction.setTransactionTime(LocalDateTime.now());
        transactionRepository.save(refundTransaction);
        memberCardRepository.save(memberCard);

        // 10. 更新预约记录状态
        record.setReservationStatus(2);
        record.setUpdateTime(LocalDateTime.now());
        if (request.getRemark() != null) {
            record.setRemark(record.getRemark() + " | 取消原因：" + request.getRemark());
        }
        recordRepository.save(record);

        // 11. 构建响应
        CancelReservationResponse response = new CancelReservationResponse();
        response.setReservationId(record.getReservationId());
        response.setReservationStatus(record.getReservationStatus());
        response.setRefundTransactionId(refundTransaction.getTransactionId());
        response.setRefundAmount(refundAmount);
        response.setPenaltyAmount(penaltyAmount);
        response.setPenaltyRule(penaltyRule);
        response.setUpdateTime(record.getUpdateTime().format(DATETIME_FORMATTER));

        return response;
    }

    // ==================== 接口14：查询某会员预约情况 ====================
    @Override
    public MemberReservationsResponse queryMemberReservations(MemberReservationsRequest request, String token) {
        log.info("查询会员预约情况：userPhone={}", request.getUserPhone());

        // 1. 解析令牌（工作令牌）
        Claims claims = jwtUtils.parseToken(token);
        Integer tokenType = claims.get("token_type", Integer.class);
        if (tokenType != 3) {
            throw new BusinessException(ErrorCode.TOKEN_PERMISSION_DENIED, "仅支持工作令牌");
        }

        // 2. 获取工作令牌中的店铺ID
        String storeId = claims.get("store_id", String.class);
        if (storeId == null || storeId.isEmpty()) {
            throw new BusinessException(ErrorCode.TOKEN_PERMISSION_DENIED, "工作令牌缺少店铺信息");
        }

        // 3. 解密手机号
        String userPhone = encryptUtils.decryptAES(request.getUserPhone());

        // 4. 查询用户信息
        User user = userRepository.findByPhone(userPhone)
                .orElseThrow(() -> new BusinessException(ErrorCode.PARAM_ERROR, "用户不存在"));

        // 5. 解析日期参数
        LocalDate startDate = request.getStartDate() != null ? 
                LocalDate.parse(request.getStartDate(), DATE_FORMATTER) : null;
        LocalDate endDate = request.getEndDate() != null ? 
                LocalDate.parse(request.getEndDate(), DATE_FORMATTER) : null;

        // 6. 分页查询预约记录（只查询本店铺的预约）
        Pageable pageable = PageRequest.of(request.getPage() - 1, request.getPageSize());
        // 将storeId转换为byte[]类型
        byte[] storeIdBytes = encryptUtils.uuidToBytes(storeId);
        Page<ReservationRecord> recordPage = recordRepository.findMemberReservations(
                userPhone,
                storeIdBytes,
                request.getStatus(),
                startDate,
                endDate,
                pageable
        );

        // 7. 构建响应
        MemberReservationsResponse response = new MemberReservationsResponse();
        
        // 用户信息
        MemberReservationsResponse.UserInfo userInfo = new MemberReservationsResponse.UserInfo();
        userInfo.setUserId(encryptUtils.bytesToUuid(user.getUserId()));
        userInfo.setNickname(user.getNickname());
        userInfo.setPhone(request.getUserPhone()); // 返回加密的手机号
        response.setUserInfo(userInfo);

        // 分页信息
        response.setTotal((int) recordPage.getTotalElements());
        response.setPage(request.getPage());
        response.setPageSize(request.getPageSize());

        // 预约列表
        List<MemberReservationsResponse.ReservationItem> list = new ArrayList<>();
        for (ReservationRecord record : recordPage.getContent()) {
            ReservationResource resource = resourceRepository.findByResourceId(record.getResourceId())
                    .orElse(null);
            
            MemberReservationsResponse.ReservationItem item = new MemberReservationsResponse.ReservationItem();
            item.setReservationId(record.getReservationId());
            item.setResourceId(record.getResourceId());
            item.setResourceName(resource != null ? resource.getResourceName() : "未知资源");
            item.setReservationDate(record.getReservationDate().format(DATE_FORMATTER));
            item.setStartTime(record.getStartTime().format(DateTimeFormatter.ofPattern("HH:mm:ss")));
            item.setEndTime(record.getEndTime().format(DateTimeFormatter.ofPattern("HH:mm:ss")));
            item.setOperateType(record.getOperateType());
            item.setTransactionId(record.getTransactionId());
            item.setTransactionAmount(record.getTransactionAmount());
            item.setReservationStatus(record.getReservationStatus());
            item.setRemark(record.getRemark());
            item.setCreateTime(record.getCreateTime().format(DATETIME_FORMATTER));
            
            list.add(item);
        }
        response.setList(list);

        return response;
    }

    // ==================== 接口16：线下占用预约资源 ====================
    @Override
    @Transactional
    public OccupyResourceResponse occupyResource(OccupyResourceRequest request, String token) {
        log.info("线下占用资源：resourceId={}, date={}", request.getResourceId(), request.getReservationDate());

        // ========== 第一阶段：无锁并发校验 ==========

        // 1. 解析令牌（工作令牌）
        Claims claims = jwtUtils.parseToken(token);
        Integer tokenType = claims.get("token_type", Integer.class);
        if (tokenType != 3) {
            throw new BusinessException(ErrorCode.TOKEN_PERMISSION_DENIED, "仅支持工作令牌");
        }

        // 2. 校验权限（商家/店长/店员默认拥有权限，无需检查）
        // 线下占用资源：所有角色(MERCHANT/MANAGER/STAFF)都默认拥有权限

        String operatorId = claims.get("user_id", String.class);
        byte[] operatorIdBytes = encryptUtils.uuidToBytes(operatorId);

        // 3. 查询资源
        ReservationResource resource = resourceRepository.findByResourceId(request.getResourceId())
                .orElseThrow(() -> new BusinessException(ErrorCode.RESERVATION_RESOURCE_NOT_FOUND));

        // 4. 查询模板
        LocalDate reservationDate = LocalDate.parse(request.getReservationDate(), DATE_FORMATTER);
        ReservationTemplate template = templateRepository.findEffectiveTemplate(resource.getStoreId(), reservationDate)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESERVATION_TEMPLATE_NOT_FOUND));

        // 5. 校验时间段合法性
        String timeSlotsError = com.ecards.member_management.validator.TimeSlotValidator.validateTimeSlots(
                request.getTimeSlots(),
                template.getReservationTimeList()
        );
        if (timeSlotsError != null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, timeSlotsError);
        }

        // 6. 提取startTime和endTime
        String startTimeStr = com.ecards.member_management.validator.TimeSlotValidator.extractOverallStartTime(request.getTimeSlots());
        String endTimeStr = com.ecards.member_management.validator.TimeSlotValidator.extractOverallEndTime(request.getTimeSlots());
        LocalTime startTime = LocalTime.parse(startTimeStr, TIME_FORMATTER);
        LocalTime endTime = LocalTime.parse(endTimeStr, TIME_FORMATTER);
        
        request.setStartTime(startTimeStr);
        request.setEndTime(endTimeStr);

        // 7. 解密客户手机号
        String customerPhone = encryptUtils.decryptAES(request.getCustomerPhone());

        // 8. 查询操作员信息
        String operatorPhone = userRepository.findPhoneByUserId(operatorIdBytes)
                .orElseThrow(() -> new BusinessException(ErrorCode.PARAM_ERROR, "操作员信息不完整"));

        // ========== 第二阶段：加锁后校验+修改 ==========

        String lockKey = lockUtil.generateLockKey(reservationDate,
                encryptUtils.bytesToUuid(resource.getStoreId()),
                resource.getResourceId());

        return lockUtil.executeWithLock(lockKey, 3, 10, () -> {
            // 9. 检查时间段冲突
            boolean hasConflict = recordRepository.existsTimeConflict(
                    request.getResourceId(), reservationDate, startTime, endTime);
            if (hasConflict) {
                throw new BusinessException(ErrorCode.RESERVATION_TIMESLOT_OCCUPIED);
            }

            // 10. 创建预约记录（operateType=2，线下占用）
            ReservationRecord record = new ReservationRecord();
            record.setResourceId(request.getResourceId());
            record.setTemplateId(template.getReserveId());
            record.setReservationDate(reservationDate);
            record.setStartTime(startTime);
            record.setEndTime(endTime);
            record.setOperateType(2); // 线下占用
            record.setUserId(operatorIdBytes); // 操作员ID
            record.setUserPhone(customerPhone); // 客户手机号（明文）
            record.setTransactionId(0L); // 无交易记录
            record.setTransactionAmount(BigDecimal.ZERO); // 无交易金额
            record.setReservationStatus(0); // 待使用
            record.setRemark(request.getRemark());
            record.setCreateTime(LocalDateTime.now());
            record.setUpdateTime(LocalDateTime.now());
            recordRepository.save(record);

            // 11. 构建响应
            OccupyResourceResponse response = new OccupyResourceResponse();
            response.setReservationId(record.getReservationId());
            response.setResourceName(resource.getResourceName());
            response.setOccupyDate(request.getReservationDate());
            response.setStartTime(record.getStartTime().format(DateTimeFormatter.ofPattern("HH:mm:ss")));
            response.setEndTime(record.getEndTime().format(DateTimeFormatter.ofPattern("HH:mm:ss")));
            response.setCustomerPhone(request.getCustomerPhone()); // 返回加密的手机号
            response.setOperatorName(operatorPhone); // 操作员手机号作为名称
            response.setCreateTime(record.getCreateTime().format(DATETIME_FORMATTER));

            return response;
        });
    }

    // ==================== 接口15：停用预约资源 ====================
    @Override
    @Transactional
    public DisableResourceResponse disableResource(DisableResourceRequest request, String token) {
        log.info("停用资源：resourceId={}, date={}", request.getResourceId(), request.getReservationDate());

        // ========== 第一阶段：无锁并发校验 ==========

        // 1. 解析令牌（工作令牌）
        Claims claims = jwtUtils.parseToken(token);
        Integer tokenType = claims.get("token_type", Integer.class);
        if (tokenType != 3) {
            throw new BusinessException(ErrorCode.TOKEN_PERMISSION_DENIED, "仅支持工作令牌");
        }

        // 2. 校验权限（商家/店长默认拥有权限，店员需检查权限）
        String role = claims.get("role", String.class);
        if ("STAFF".equals(role)) {
            // 店员需要检查权限
            List<String> permissions = claims.get("permissions", List.class);
            if (permissions == null || !permissions.contains("reservation_disable")) {
                throw new BusinessException(ErrorCode.TOKEN_PERMISSION_DENIED, "无资源停用权限");
            }
        }
        // 商家(MERCHANT)和店长(MANAGER)默认拥有权限，无需检查

        String operatorId = claims.get("user_id", String.class);
        byte[] operatorIdBytes = encryptUtils.uuidToBytes(operatorId);

        // 3. 查询资源
        ReservationResource resource = resourceRepository.findByResourceId(request.getResourceId())
                .orElseThrow(() -> new BusinessException(ErrorCode.RESERVATION_RESOURCE_NOT_FOUND));

        // 4. 查询模板
        LocalDate reservationDate = LocalDate.parse(request.getReservationDate(), DATE_FORMATTER);
        ReservationTemplate template = templateRepository.findEffectiveTemplate(resource.getStoreId(), reservationDate)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESERVATION_TEMPLATE_NOT_FOUND));

        // 5. 校验时间段合法性
        String timeSlotsError = com.ecards.member_management.validator.TimeSlotValidator.validateTimeSlots(
                request.getTimeSlots(),
                template.getReservationTimeList()
        );
        if (timeSlotsError != null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, timeSlotsError);
        }

        // 6. 提取startTime和endTime
        String startTimeStr = com.ecards.member_management.validator.TimeSlotValidator.extractOverallStartTime(request.getTimeSlots());
        String endTimeStr = com.ecards.member_management.validator.TimeSlotValidator.extractOverallEndTime(request.getTimeSlots());
        LocalTime startTime = LocalTime.parse(startTimeStr, TIME_FORMATTER);
        LocalTime endTime = LocalTime.parse(endTimeStr, TIME_FORMATTER);
        
        request.setStartTime(startTimeStr);
        request.setEndTime(endTimeStr);

        // 7. 查询操作员信息
        String operatorPhone = userRepository.findPhoneByUserId(operatorIdBytes)
                .orElseThrow(() -> new BusinessException(ErrorCode.PARAM_ERROR, "操作员信息不完整"));

        // ========== 第二阶段：加锁后校验+修改 ==========

        String lockKey = lockUtil.generateLockKey(reservationDate,
                encryptUtils.bytesToUuid(resource.getStoreId()),
                resource.getResourceId());

        return lockUtil.executeWithLock(lockKey, 3, 10, () -> {
            // 8. 检查时间段冲突
            boolean hasConflict = recordRepository.existsTimeConflict(
                    request.getResourceId(), reservationDate, startTime, endTime);
            if (hasConflict) {
                throw new BusinessException(ErrorCode.RESERVATION_TIMESLOT_OCCUPIED);
            }

            // 9. 创建预约记录（operateType=3，资源停用）
            ReservationRecord record = new ReservationRecord();
            record.setResourceId(request.getResourceId());
            record.setTemplateId(template.getReserveId());
            record.setReservationDate(reservationDate);
            record.setStartTime(startTime);
            record.setEndTime(endTime);
            record.setOperateType(3); // 资源停用
            record.setUserId(operatorIdBytes); // 操作员ID
            record.setUserPhone(operatorPhone); // 操作员手机号
            record.setTransactionId(0L); // 无交易记录
            record.setTransactionAmount(BigDecimal.ZERO); // 无交易金额
            record.setReservationStatus(0); // 待使用
            record.setRemark(request.getReason()); // 停用原因
            record.setCreateTime(LocalDateTime.now());
            record.setUpdateTime(LocalDateTime.now());
            recordRepository.save(record);

            // 10. 构建响应
            DisableResourceResponse response = new DisableResourceResponse();
            response.setReservationId(record.getReservationId());
            response.setResourceName(resource.getResourceName());
            response.setDisableDate(request.getReservationDate());
            response.setStartTime(record.getStartTime().format(DateTimeFormatter.ofPattern("HH:mm:ss")));
            response.setEndTime(record.getEndTime().format(DateTimeFormatter.ofPattern("HH:mm:ss")));
            response.setOperatorName(operatorPhone); // 操作员手机号作为名称
            response.setCreateTime(record.getCreateTime().format(DATETIME_FORMATTER));

            return response;
        });
    }

    // ==================== 接口17：员工取消预约 ====================
    @Override
    @Transactional
    public StaffCancelResponse staffCancelReservation(StaffCancelRequest request, String token) {
        log.info("员工取消预约：reservationId={}, useCancelRule={}", request.getReservationId(), request.getUseCancelRule());

        // 1. 解析令牌（工作令牌）
        Claims claims = jwtUtils.parseToken(token);
        Integer tokenType = claims.get("token_type", Integer.class);
        if (tokenType != 3) {
            throw new BusinessException(ErrorCode.TOKEN_PERMISSION_DENIED, "仅支持工作令牌");
        }

        // 2. 校验权限（商家/店长默认拥有权限，店员需检查权限）
        String role = claims.get("role", String.class);
        if ("STAFF".equals(role)) {
            // 店员需要检查权限
            List<String> permissions = claims.get("permissions", List.class);
            if (permissions == null || !permissions.contains("reservation_cancel")) {
                throw new BusinessException(ErrorCode.TOKEN_PERMISSION_DENIED, "无预约取消权限");
            }
        }
        // 商家(MERCHANT)和店长(MANAGER)默认拥有权限，无需检查

        String operatorId = claims.get("user_id", String.class);
        byte[] operatorIdBytes = encryptUtils.uuidToBytes(operatorId);

        // 3. 查询预约记录
        ReservationRecord record = recordRepository.findById(request.getReservationId())
                .orElseThrow(() -> new BusinessException(ErrorCode.RESERVATION_NOT_FOUND));

        // 4. 校验预约状态（不能取消已取消的记录）
        if (record.getReservationStatus() == 2 || record.getReservationStatus() == 3) {
            throw new BusinessException(ErrorCode.RESERVATION_NOT_FOUND, "预约已取消");
        }

        // 5. 校验是否已使用（仅对用户预约operateType=1校验）
        if (record.getOperateType() == 1 && record.getReservationStatus() == 1) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "预约已使用，无法取消");
        }

        // 6. 查询模板获取取消规则
        ReservationResource resource = resourceRepository.findByResourceId(record.getResourceId())
                .orElseThrow(() -> new BusinessException(ErrorCode.RESERVATION_RESOURCE_NOT_FOUND));
        ReservationTemplate template = templateRepository.findEffectiveTemplate(
                resource.getStoreId(), record.getReservationDate())
                .orElseThrow(() -> new BusinessException(ErrorCode.RESERVATION_TEMPLATE_NOT_FOUND));

        // 7. 计算违约费（仅对用户预约operateType=1计算）
        BigDecimal penaltyAmount = BigDecimal.ZERO;
        String penaltyRule = "无违约费";

        if (record.getOperateType() == 1 && request.getUseCancelRule()) {
            // 仅用户预约支持取消规则
            LocalDateTime reservationStartTime = LocalDateTime.of(
                    record.getReservationDate(), record.getStartTime());
            long minutesToStart = java.time.Duration.between(LocalDateTime.now(), reservationStartTime).toMinutes();
            
            // 使用取消规则，允许已开始的预约（负数）
            CancelRuleCalculator.PenaltyResult penaltyResult = CancelRuleCalculator.calculatePenalty(
                    minutesToStart,
                    template.getCancelRule(),
                    record.getTransactionAmount(),
                    true  // 员工取消允许已开始的预约
            );
            penaltyAmount = penaltyResult.getPenaltyAmount();
            penaltyRule = penaltyResult.getPenaltyRule();
        }
        // operateType=2/3 或不使用取消规则时，违约费为0，全额退款

        // 9. 计算退款金额
        BigDecimal refundAmount = record.getTransactionAmount().subtract(penaltyAmount);

        // 10. 查询原交易记录和会员卡（仅operateType=1需要退款）
        if (record.getOperateType() == 1) {
            TransactionRecord originalTransaction = transactionRepository.findById(record.getTransactionId())
                    .orElseThrow(() -> new BusinessException(ErrorCode.SYSTEM_ERROR, "原交易记录不存在"));
            MemberCard memberCard = memberCardRepository.findByMemberCardId(originalTransaction.getMemberCardId())
                    .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_CARD_NOT_FOUND));

            // 11. 创建退款交易记录
            TransactionRecord refundTransaction = new TransactionRecord();
            refundTransaction.setMemberCardId(originalTransaction.getMemberCardId());
            refundTransaction.setUserId(record.getUserId());
            refundTransaction.setMerchantId(originalTransaction.getMerchantId());
            refundTransaction.setTransactionType(3);
            refundTransaction.setAmount(refundAmount);

            if (memberCard.getCardTtype() == 1) {
                BigDecimal newBalance = memberCard.getBalance().add(refundAmount);
                refundTransaction.setBalanceSnapshot(newBalance);
                memberCard.setBalance(newBalance);
            } else if (memberCard.getCardTtype() == 2) {
                int refundTimes = (int) Math.ceil(refundAmount.doubleValue());
                int newTimes = memberCard.getTimes() + refundTimes;
                refundTransaction.setBalanceSnapshot(BigDecimal.valueOf(newTimes));
                memberCard.setTimes(newTimes);
            }

            refundTransaction.setOperatorId(operatorIdBytes);
            refundTransaction.setTransStoreId(resource.getStoreId());
            refundTransaction.setRemark("员工取消：" + penaltyRule);
            refundTransaction.setTransactionTime(LocalDateTime.now());
            transactionRepository.save(refundTransaction);
            memberCardRepository.save(memberCard);
        }

        // 12. 更新预约记录状态（员工取消=3）
        record.setReservationStatus(3);
        record.setUpdateTime(LocalDateTime.now());
        record.setRemark(record.getRemark() + " | 员工取消原因：" + request.getCancelReason());
        recordRepository.save(record);

        // 13. 查询操作员信息
        String operatorPhone = userRepository.findPhoneByUserId(operatorIdBytes)
                .orElse("未知操作员");

        // 14. 构建响应
        StaffCancelResponse response = new StaffCancelResponse();
        response.setReservationId(record.getReservationId());
        response.setCancelTime(LocalDateTime.now().format(DATETIME_FORMATTER));
        response.setRefundAmount(refundAmount);
        response.setPenaltyAmount(penaltyAmount);
        
        // 获取最新余额快照
        if (record.getOperateType() == 1) {
            TransactionRecord originalTransaction = transactionRepository.findById(record.getTransactionId()).orElse(null);
            if (originalTransaction != null) {
                MemberCard memberCard = memberCardRepository.findByMemberCardId(originalTransaction.getMemberCardId()).orElse(null);
                if (memberCard != null) {
                    if (memberCard.getCardTtype() == 1) {
                        response.setBalanceSnapshot(memberCard.getBalance());
                    } else if (memberCard.getCardTtype() == 2) {
                        response.setBalanceSnapshot(BigDecimal.valueOf(memberCard.getTimes()));
                    }
                }
            }
        } else {
            response.setBalanceSnapshot(BigDecimal.ZERO);
        }
        
        response.setOperatorName(operatorPhone);
        response.setCancelReason(request.getCancelReason());

        return response;
    }

    /**
     * 接口19：创建/修改优惠策略
     */
    @Override
    @Transactional
    public PromotionStrategyResponse createOrUpdatePromotionStrategy(PromotionStrategyRequest request, String token) {
        log.info("创建/修改优惠策略：resourceIds={}, effectiveWeek={}", request.getResourceIds(), request.getEffectiveWeek());

        // 1. 解析令牌并验证权限
        Claims claims = jwtUtils.parseToken(token);
        Integer tokenType = claims.get("token_type", Integer.class);
        if (tokenType != 3) {
            throw new BusinessException(ErrorCode.TOKEN_PERMISSION_DENIED, "需要工作令牌");
        }

        String tokenStoreId = claims.get("store_id", String.class);
        byte[] tokenStoreIdBytes = encryptUtils.uuidToBytes(tokenStoreId);

        // 2. 验证所有资源存在且归属于当前店铺
        List<ReservationResource> resources = new ArrayList<>();
        for (Long resourceId : request.getResourceIds()) {
            ReservationResource resource = resourceRepository.findByResourceId(resourceId)
                    .orElseThrow(() -> new BusinessException(ErrorCode.RESERVATION_RESOURCE_NOT_FOUND, 
                            "资源ID " + resourceId + " 不存在"));

            if (!Arrays.equals(resource.getStoreId(), tokenStoreIdBytes)) {
                throw new BusinessException(ErrorCode.NOT_MERCHANT_USER, "无权操作该店铺的资源");
            }

            // 验证资源类型（仅余额卡支持优惠策略）
            if (resource.getSupportCardTypes() != 1) {
                throw new BusinessException(ErrorCode.PARAM_ERROR, 
                        "资源 " + resource.getResourceName() + " 不是余额卡类型，不支持优惠策略");
            }

            resources.add(resource);
        }

        // 3. 校验星期合法性
        List<String> validWeeks = Arrays.asList("周一", "周二", "周三", "周四", "周五", "周六", "周日");
        for (String week : request.getEffectiveWeek()) {
            if (!validWeeks.contains(week)) {
                throw new BusinessException(ErrorCode.PARAM_ERROR, "星期值不合法：" + week);
            }
        }

        // 4. 校验时间段格式和不重复/不交叉
        validateTimeSlots(request.getDiscountsTime());

        // 5. 校验优惠规则格式
        for (PromotionStrategyRequest.DiscountTimeSlot slot : request.getDiscountsTime()) {
            validateDiscountRule(slot.getDiscount());
        }

        // 6. 更新每个资源的优惠策略
        int successCount = 0;
        for (ReservationResource resource : resources) {
            // 获取现有策略或创建新策略
            java.util.Map<String, Object> strategy = resource.getPromotionStrategy();
            if (strategy == null) {
                strategy = new java.util.HashMap<>();
                strategy.put("non_effective_dates", new ArrayList<>());
                strategy.put("week", new ArrayList<>());
            }

            // 获取week数组
            @SuppressWarnings("unchecked")
            List<java.util.Map<String, Object>> weekList = 
                    (List<java.util.Map<String, Object>>) strategy.get("week");
            if (weekList == null) {
                weekList = new ArrayList<>();
            }

            // 更新或新增指定星期的策略
            for (String effectiveWeek : request.getEffectiveWeek()) {
                // 查找是否已存在该星期的策略
                java.util.Map<String, Object> existingWeek = null;
                for (java.util.Map<String, Object> week : weekList) {
                    if (effectiveWeek.equals(week.get("effective_week"))) {
                        existingWeek = week;
                        break;
                    }
                }

                // 构建新的discount_time
                List<java.util.Map<String, Object>> discountTimeList = new ArrayList<>();
                for (PromotionStrategyRequest.DiscountTimeSlot slot : request.getDiscountsTime()) {
                    java.util.Map<String, Object> discountTime = new java.util.HashMap<>();
                    discountTime.put("time_slot", slot.getTimeSlot());
                    discountTime.put("discount", slot.getDiscount());
                    discountTimeList.add(discountTime);
                }

                if (existingWeek != null) {
                    // 更新现有策略
                    existingWeek.put("discount_time", discountTimeList);
                } else {
                    // 新增策略
                    java.util.Map<String, Object> newWeek = new java.util.HashMap<>();
                    newWeek.put("effective_week", effectiveWeek);
                    newWeek.put("discount_time", discountTimeList);
                    weekList.add(newWeek);
                }
            }

            strategy.put("week", weekList);
            resource.setPromotionStrategy(strategy);
            resourceRepository.save(resource);
            successCount++;
        }

        // 7. 构建响应
        PromotionStrategyResponse response = new PromotionStrategyResponse();
        response.setSuccessCount(successCount);
        response.setUpdateTime(LocalDateTime.now().format(DATETIME_FORMATTER));

        return response;
    }

    /**
     * 接口20：设置优惠策略不生效日期
     */
    @Override
    @Transactional
    public NonEffectiveDatesResponse setNonEffectiveDates(NonEffectiveDatesRequest request, String token) {
        log.info("设置优惠策略不生效日期：resourceIds={}, operation={}, dates={}", 
                request.getResourceIds(), request.getOperation(), request.getDates());

        // 1. 解析令牌并验证权限
        Claims claims = jwtUtils.parseToken(token);
        Integer tokenType = claims.get("token_type", Integer.class);
        if (tokenType != 3) {
            throw new BusinessException(ErrorCode.TOKEN_PERMISSION_DENIED, "需要工作令牌");
        }

        String tokenStoreId = claims.get("store_id", String.class);
        byte[] tokenStoreIdBytes = encryptUtils.uuidToBytes(tokenStoreId);

        // 2. 验证所有资源存在且归属于当前店铺
        List<ReservationResource> resources = new ArrayList<>();
        for (Long resourceId : request.getResourceIds()) {
            ReservationResource resource = resourceRepository.findByResourceId(resourceId)
                    .orElseThrow(() -> new BusinessException(ErrorCode.RESERVATION_RESOURCE_NOT_FOUND, 
                            "资源ID " + resourceId + " 不存在"));

            if (!Arrays.equals(resource.getStoreId(), tokenStoreIdBytes)) {
                throw new BusinessException(ErrorCode.NOT_MERCHANT_USER, "无权操作该店铺的资源");
            }

            if (resource.getSupportCardTypes() != 1) {
                throw new BusinessException(ErrorCode.PARAM_ERROR, 
                        "资源 " + resource.getResourceName() + " 不是余额卡类型，不支持优惠策略");
            }

            resources.add(resource);
        }

        // 3. 校验日期格式和合法性
        LocalDate today = LocalDate.now();
        for (String dateStr : request.getDates()) {
            try {
                LocalDate date = LocalDate.parse(dateStr, DATE_FORMATTER);
                if (date.isBefore(today)) {
                    throw new BusinessException(ErrorCode.PARAM_ERROR, "日期不能早于今天：" + dateStr);
                }
            } catch (Exception e) {
                throw new BusinessException(ErrorCode.PARAM_ERROR, "日期格式错误：" + dateStr);
            }
        }

        // 4. 更新每个资源的不生效日期
        int successCount = 0;
        for (ReservationResource resource : resources) {
            // 获取现有策略或创建新策略
            java.util.Map<String, Object> strategy = resource.getPromotionStrategy();
            if (strategy == null) {
                strategy = new java.util.HashMap<>();
                strategy.put("non_effective_dates", new ArrayList<>());
                strategy.put("week", new ArrayList<>());
            }

            // 获取non_effective_dates数组
            @SuppressWarnings("unchecked")
            List<String> nonEffectiveDates = (List<String>) strategy.get("non_effective_dates");
            if (nonEffectiveDates == null) {
                nonEffectiveDates = new ArrayList<>();
            }

            // 自动清理过期日期（超过3天）
            LocalDate threeDaysAgo = today.minusDays(3);
            nonEffectiveDates.removeIf(dateStr -> {
                try {
                    LocalDate date = LocalDate.parse(dateStr, DATE_FORMATTER);
                    return date.isBefore(threeDaysAgo);
                } catch (Exception e) {
                    return true; // 格式错误的也删除
                }
            });

            // 根据操作类型处理
            switch (request.getOperation()) {
                case "add":
                    // 添加日期（去重）
                    for (String date : request.getDates()) {
                        if (!nonEffectiveDates.contains(date)) {
                            nonEffectiveDates.add(date);
                        }
                    }
                    break;
                case "replace":
                    // 覆盖
                    nonEffectiveDates = new ArrayList<>(request.getDates());
                    break;
                case "remove":
                    // 移除
                    nonEffectiveDates.removeAll(request.getDates());
                    break;
            }

            // 排序（降序）
            nonEffectiveDates.sort((a, b) -> b.compareTo(a));

            strategy.put("non_effective_dates", nonEffectiveDates);
            resource.setPromotionStrategy(strategy);
            resourceRepository.save(resource);
            successCount++;
        }

        // 5. 构建响应
        NonEffectiveDatesResponse response = new NonEffectiveDatesResponse();
        response.setSuccessCount(successCount);
        response.setUpdateTime(LocalDateTime.now().format(DATETIME_FORMATTER));

        return response;
    }

    /**
     * 接口21：清空指定资源优惠策略
     */
    @Override
    @Transactional
    public ClearPromotionStrategyResponse clearPromotionStrategy(ClearPromotionStrategyRequest request, String token) {
        log.info("清空指定资源优惠策略：resourceIds={}", request.getResourceIds());

        // 1. 解析令牌并验证权限
        Claims claims = jwtUtils.parseToken(token);
        Integer tokenType = claims.get("token_type", Integer.class);
        if (tokenType != 3) {
            throw new BusinessException(ErrorCode.TOKEN_PERMISSION_DENIED, "需要工作令牌");
        }

        String tokenStoreId = claims.get("store_id", String.class);
        byte[] tokenStoreIdBytes = encryptUtils.uuidToBytes(tokenStoreId);

        // 2. 验证所有资源存在且归属于当前店铺
        List<ReservationResource> resources = new ArrayList<>();
        for (Long resourceId : request.getResourceIds()) {
            ReservationResource resource = resourceRepository.findByResourceId(resourceId)
                    .orElseThrow(() -> new BusinessException(ErrorCode.RESERVATION_RESOURCE_NOT_FOUND, 
                            "资源ID " + resourceId + " 不存在"));

            if (!Arrays.equals(resource.getStoreId(), tokenStoreIdBytes)) {
                throw new BusinessException(ErrorCode.NOT_MERCHANT_USER, "无权操作该店铺的资源");
            }

            resources.add(resource);
        }

        // 3. 清空每个资源的优惠策略
        int successCount = 0;
        for (ReservationResource resource : resources) {
            java.util.Map<String, Object> emptyStrategy = new java.util.HashMap<>();
            emptyStrategy.put("non_effective_dates", new ArrayList<>());
            emptyStrategy.put("week", new ArrayList<>());
            
            resource.setPromotionStrategy(emptyStrategy);
            resourceRepository.save(resource);
            successCount++;
        }

        // 4. 构建响应
        ClearPromotionStrategyResponse response = new ClearPromotionStrategyResponse();
        response.setSuccessCount(successCount);
        response.setUpdateTime(LocalDateTime.now().format(DATETIME_FORMATTER));

        return response;
    }

    /**
     * 校验时间段格式和不重复/不交叉
     */
    private void validateTimeSlots(List<PromotionStrategyRequest.DiscountTimeSlot> discountsTime) {
        List<LocalTime[]> timeRanges = new ArrayList<>();

        for (PromotionStrategyRequest.DiscountTimeSlot slot : discountsTime) {
            String timeSlot = slot.getTimeSlot();
            
            // 校验格式
            if (!timeSlot.matches("\\d{2}:\\d{2}-\\d{2}:\\d{2}")) {
                throw new BusinessException(ErrorCode.PARAM_ERROR, "时间段格式错误：" + timeSlot);
            }

            String[] parts = timeSlot.split("-");
            LocalTime start = LocalTime.parse(parts[0]);
            LocalTime end = parts[1].equals("24:00") ? LocalTime.MAX : LocalTime.parse(parts[1]);

            // 校验开始时间小于结束时间
            if (!start.isBefore(end) && !parts[1].equals("24:00")) {
                throw new BusinessException(ErrorCode.PARAM_ERROR, "开始时间必须小于结束时间：" + timeSlot);
            }

            // 检查是否与已有时间段重复或交叉
            for (LocalTime[] existingRange : timeRanges) {
                LocalTime existingStart = existingRange[0];
                LocalTime existingEnd = existingRange[1];

                // 判断是否交叉
                if (!(end.isBefore(existingStart) || end.equals(existingStart) || 
                      start.isAfter(existingEnd) || start.equals(existingEnd))) {
                    throw new BusinessException(ErrorCode.PARAM_ERROR, 
                            "时间段重复或交叉：" + timeSlot);
                }
            }

            timeRanges.add(new LocalTime[]{start, end});
        }
    }

    /**
     * 校验优惠规则格式
     */
    private void validateDiscountRule(String discount) {
        if (discount == null || discount.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "优惠规则不能为空");
        }

        char firstChar = discount.charAt(0);
        String value = discount.substring(1);

        try {
            switch (firstChar) {
                case '*':
                    // 打折：*0.9
                    double rate = Double.parseDouble(value);
                    if (rate <= 0 || rate > 1) {
                        throw new BusinessException(ErrorCode.PARAM_ERROR, 
                                "打折比例必须在0-1之间：" + discount);
                    }
                    break;
                case '-':
                    // 减价：-10
                    double minus = Double.parseDouble(value);
                    if (minus <= 0) {
                        throw new BusinessException(ErrorCode.PARAM_ERROR, 
                                "减价金额必须大于0：" + discount);
                    }
                    break;
                case ':':
                case '^':
                    // 满减打折：:100*0.9 或 满减：^100-10
                    if (!value.contains("*") && !value.contains("-")) {
                        throw new BusinessException(ErrorCode.PARAM_ERROR, 
                                "满减规则格式错误：" + discount);
                    }
                    String[] parts = value.split("[*-]");
                    if (parts.length != 2) {
                        throw new BusinessException(ErrorCode.PARAM_ERROR, 
                                "满减规则格式错误：" + discount);
                    }
                    double threshold = Double.parseDouble(parts[0]);
                    double discountValue = Double.parseDouble(parts[1]);
                    if (threshold <= 0 || discountValue <= 0) {
                        throw new BusinessException(ErrorCode.PARAM_ERROR, 
                                "满减规则数值必须大于0：" + discount);
                    }
                    if (firstChar == ':' && (discountValue <= 0 || discountValue > 1)) {
                        throw new BusinessException(ErrorCode.PARAM_ERROR, 
                                "满减打折比例必须在0-1之间：" + discount);
                    }
                    break;
                default:
                    throw new BusinessException(ErrorCode.PARAM_ERROR, 
                            "优惠规则格式错误，必须以*、-、:或^开头：" + discount);
            }
        } catch (NumberFormatException e) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "优惠规则数值格式错误：" + discount);
        }
    }

    /**
     * 构建优惠策略信息（用于接口10）
     */
    private ReservationQueryResponse.DiscountInfo buildDiscountInfo(
            java.util.Map<String, Object> promotionStrategy, LocalDate queryDate) {
        
        if (promotionStrategy == null || promotionStrategy.isEmpty()) {
            return new ReservationQueryResponse.DiscountInfo(new ArrayList<>(), new ArrayList<>());
        }

        // 获取non_effective_dates
        @SuppressWarnings("unchecked")
        List<String> nonEffectiveDates = (List<String>) promotionStrategy.get("non_effective_dates");
        if (nonEffectiveDates == null) {
            nonEffectiveDates = new ArrayList<>();
        }

        // 如果查询日期在不生效日期中，返回空的discountsTime
        if (nonEffectiveDates.contains(queryDate.format(DATE_FORMATTER))) {
            return new ReservationQueryResponse.DiscountInfo(nonEffectiveDates, new ArrayList<>());
        }

        // 计算查询日期是星期几
        String weekDay = getWeekDay(queryDate);

        // 获取week数组
        @SuppressWarnings("unchecked")
        List<java.util.Map<String, Object>> weekList = 
                (List<java.util.Map<String, Object>>) promotionStrategy.get("week");
        
        if (weekList == null || weekList.isEmpty()) {
            return new ReservationQueryResponse.DiscountInfo(nonEffectiveDates, new ArrayList<>());
        }

        // 查找对应星期的优惠策略
        for (java.util.Map<String, Object> week : weekList) {
            String effectiveWeek = (String) week.get("effective_week");
            if (weekDay.equals(effectiveWeek)) {
                @SuppressWarnings("unchecked")
                List<java.util.Map<String, Object>> discountTimeList = 
                        (List<java.util.Map<String, Object>>) week.get("discount_time");
                
                if (discountTimeList != null) {
                    List<ReservationQueryResponse.DiscountTimeSlot> discountsTime = new ArrayList<>();
                    for (java.util.Map<String, Object> dt : discountTimeList) {
                        ReservationQueryResponse.DiscountTimeSlot slot = 
                                new ReservationQueryResponse.DiscountTimeSlot();
                        slot.setTimeSlot((String) dt.get("time_slot"));
                        slot.setDiscount((String) dt.get("discount"));
                        discountsTime.add(slot);
                    }
                    return new ReservationQueryResponse.DiscountInfo(nonEffectiveDates, discountsTime);
                }
            }
        }

        // 没有找到对应星期的策略
        return new ReservationQueryResponse.DiscountInfo(nonEffectiveDates, new ArrayList<>());
    }

    /**
     * 获取星期几（中文）
     */
    private String getWeekDay(LocalDate date) {
        int dayOfWeek = date.getDayOfWeek().getValue(); // 1=周一, 7=周日
        String[] weekDays = {"周一", "周二", "周三", "周四", "周五", "周六", "周日"};
        return weekDays[dayOfWeek - 1];
    }

    /**
     * 计算优惠后的价格（用于接口12）
     * @param timeSlots 预约时间段列表
     * @param promotionStrategy 优惠策略
     * @param unitPrice 单价
     * @param reservationDate 预约日期
     * @return 优惠后的总价格
     */
    private BigDecimal calculateDiscountedPrice(
            List<String> timeSlots,
            java.util.Map<String, Object> promotionStrategy,
            BigDecimal unitPrice,
            LocalDate reservationDate) {
        
        // 如果没有优惠策略，直接按原价计算
        if (promotionStrategy == null || promotionStrategy.isEmpty()) {
            return unitPrice.multiply(BigDecimal.valueOf(timeSlots.size()));
        }

        // 1. 检查预约日期是否在non_effective_dates中
        @SuppressWarnings("unchecked")
        List<String> nonEffectiveDates = (List<String>) promotionStrategy.get("non_effective_dates");
        if (nonEffectiveDates != null && nonEffectiveDates.contains(reservationDate.format(DATE_FORMATTER))) {
            // 不应用优惠，按原价计算
            return unitPrice.multiply(BigDecimal.valueOf(timeSlots.size()));
        }

        // 2. 计算预约日期是星期几
        String weekDay = getWeekDay(reservationDate);

        // 3. 获取对应星期的discounts_time
        @SuppressWarnings("unchecked")
        List<java.util.Map<String, Object>> weekList = 
                (List<java.util.Map<String, Object>>) promotionStrategy.get("week");
        
        if (weekList == null || weekList.isEmpty()) {
            return unitPrice.multiply(BigDecimal.valueOf(timeSlots.size()));
        }

        List<java.util.Map<String, Object>> discountsTime = null;
        for (java.util.Map<String, Object> week : weekList) {
            if (weekDay.equals(week.get("effective_week"))) {
                @SuppressWarnings("unchecked")
                List<java.util.Map<String, Object>> dt = 
                        (List<java.util.Map<String, Object>>) week.get("discount_time");
                discountsTime = dt;
                break;
            }
        }

        if (discountsTime == null || discountsTime.isEmpty()) {
            return unitPrice.multiply(BigDecimal.valueOf(timeSlots.size()));
        }

        // 4. 计算原价总额（用于满减判断）
        BigDecimal originalTotal = unitPrice.multiply(BigDecimal.valueOf(timeSlots.size()));

        // 5. 遍历每个时间段，应用优惠
        BigDecimal totalPrice = BigDecimal.ZERO;
        for (String timeSlot : timeSlots) {
            BigDecimal slotPrice = applyDiscount(timeSlot, discountsTime, unitPrice, originalTotal);
            totalPrice = totalPrice.add(slotPrice);
        }

        return totalPrice.setScale(1, RoundingMode.HALF_UP);
    }

    /**
     * 对单个时间段应用优惠
     */
    private BigDecimal applyDiscount(
            String timeSlot,
            List<java.util.Map<String, Object>> discountsTime,
            BigDecimal unitPrice,
            BigDecimal originalTotal) {
        
        // 解析时间段
        String[] parts = timeSlot.split("-");
        LocalTime slotStart = LocalTime.parse(parts[0]);
        LocalTime slotEnd = parts[1].equals("24:00") ? LocalTime.MAX : LocalTime.parse(parts[1]);

        // 查找匹配的优惠时间段
        for (java.util.Map<String, Object> dt : discountsTime) {
            String discountTimeSlot = (String) dt.get("time_slot");
            String discount = (String) dt.get("discount");

            String[] discountParts = discountTimeSlot.split("-");
            LocalTime discountStart = LocalTime.parse(discountParts[0]);
            LocalTime discountEnd = discountParts[1].equals("24:00") ? LocalTime.MAX : LocalTime.parse(discountParts[1]);

            // 判断时间段是否完全在优惠时间段内
            if ((slotStart.equals(discountStart) || slotStart.isAfter(discountStart)) &&
                (slotEnd.equals(discountEnd) || slotEnd.isBefore(discountEnd))) {
                
                // 应用优惠规则
                return applyDiscountRule(discount, unitPrice, originalTotal);
            }
        }

        // 没有匹配的优惠，返回原价
        return unitPrice;
    }

    /**
     * 应用具体的优惠规则
     */
    private BigDecimal applyDiscountRule(String discount, BigDecimal unitPrice, BigDecimal originalTotal) {
        char firstChar = discount.charAt(0);
        String value = discount.substring(1);

        try {
            switch (firstChar) {
                case '*':
                    // 打折：*0.9
                    double rate = Double.parseDouble(value);
                    BigDecimal discountedPrice = unitPrice.multiply(BigDecimal.valueOf(rate));
                    return discountedPrice.max(BigDecimal.ZERO);
                    
                case '-':
                    // 减价：-10
                    double minus = Double.parseDouble(value);
                    BigDecimal reducedPrice = unitPrice.subtract(BigDecimal.valueOf(minus));
                    return reducedPrice.max(BigDecimal.ZERO);
                    
                case ':':
                    // 满减打折：:100*0.9
                    String[] parts1 = value.split("\\*");
                    double threshold1 = Double.parseDouble(parts1[0]);
                    double discountRate = Double.parseDouble(parts1[1]);
                    if (originalTotal.compareTo(BigDecimal.valueOf(threshold1)) >= 0) {
                        BigDecimal discountedPrice1 = unitPrice.multiply(BigDecimal.valueOf(discountRate));
                        return discountedPrice1.max(BigDecimal.ZERO);
                    }
                    return unitPrice;
                    
                case '^':
                    // 满减：^100-10
                    String[] parts2 = value.split("-");
                    double threshold2 = Double.parseDouble(parts2[0]);
                    double reduction = Double.parseDouble(parts2[1]);
                    if (originalTotal.compareTo(BigDecimal.valueOf(threshold2)) >= 0) {
                        BigDecimal reducedPrice2 = unitPrice.subtract(BigDecimal.valueOf(reduction));
                        return reducedPrice2.max(BigDecimal.ZERO);
                    }
                    return unitPrice;
                    
                default:
                    return unitPrice;
            }
        } catch (Exception e) {
            log.error("应用优惠规则失败：discount={}, error={}", discount, e.getMessage());
            return unitPrice;
        }
    }
}
