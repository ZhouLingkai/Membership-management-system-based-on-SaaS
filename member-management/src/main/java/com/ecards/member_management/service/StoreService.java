package com.ecards.member_management.service;

import com.ecards.member_management.common.ErrorCode;
import com.ecards.member_management.dto.request.StoreCreateRequest;
import com.ecards.member_management.dto.request.StoreUpdateRequest;
import com.ecards.member_management.dto.response.StoreCreateResponse;
import com.ecards.member_management.dto.response.StoreDetailResponse;
import com.ecards.member_management.dto.response.StoreListResponse;
import com.ecards.member_management.dto.response.StoreUpdateResponse;
import com.ecards.member_management.entity.MerchantExtend;
import com.ecards.member_management.entity.Store;
import com.ecards.member_management.exception.BusinessException;
import com.ecards.member_management.repository.MerchantExtendRepository;
import com.ecards.member_management.repository.StoreRepository;
import com.ecards.member_management.utils.EncryptUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 店铺服务
 * 提供店铺创建、查询、修改等功能
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StoreService {

    private final StoreRepository storeRepository;
    private final MerchantExtendRepository merchantExtendRepository;
    private final EncryptUtils encryptUtils;

    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * 创建店铺
     *
     * @param request 店铺创建请求
     * @return 店铺创建响应
     */
    @Transactional
    public StoreCreateResponse createStore(StoreCreateRequest request) {
        log.info("创建店铺：商户ID={}, 店铺名称={}", request.getMerchantId(), request.getStoreName());

        // 1. 查询商户信息
        byte[] merchantIdBytes = encryptUtils.uuidToBytes(request.getMerchantId());
        MerchantExtend merchant = merchantExtendRepository.findById(merchantIdBytes)
                .orElseThrow(() -> new BusinessException(ErrorCode.PARAM_ERROR.getCode(), "商户不存在"));

        // 2. 校验商户状态（已认证或测试中的商户可以创建店铺）
        Integer certification = merchant.getCertification();
        if (certification != 1 && certification != 2) {
            throw new BusinessException(ErrorCode.PERMISSION_DENIED.getCode(),
                    "只有已认证或测试中的商户才能创建店铺，当前认证状态：" + certification);
        }

        // 2.5 校验店铺名称唯一性（同一商家下不能有重名店铺）
        List<Store> existingStores = storeRepository.findByMerchantId(merchantIdBytes);
        boolean hasDuplicateName = existingStores.stream()
                .anyMatch(s -> s.getStoreName().equals(request.getStoreName()));
        if (hasDuplicateName) {
            throw new BusinessException(ErrorCode.PARAM_ERROR.getCode(), 
                "该商户下已存在同名店铺：" + request.getStoreName());
        }

        // 3. 创建店铺实体
        LocalDateTime now = LocalDateTime.now();
        byte[] storeId = encryptUtils.uuidToBytes(UUID.randomUUID());

        Store store = new Store();
        store.setStoreId(storeId);
        store.setMerchantId(merchantIdBytes);
        store.setStoreName(request.getStoreName());
        store.setStoreType(request.getStoreType());
        store.setAddress(request.getStoreAddress());
        store.setStorePhotos(request.getStorePhotos());
        store.setBusinessLicense(request.getBusinessLicense());
        store.setContactPhone(request.getContactPhone());
        store.setContactWx(request.getContactWx());
        store.setStatus(1); // 默认正常营业
        store.setBusinessTime(request.getBusinessHours());
        store.setAppointment(request.getAppointment() != null ? request.getAppointment() : 0);
        store.setOpenStoreTime(request.getOpenStoreTime());
        store.setCreateTime(now);
        store.setUpdateTime(now);

        storeRepository.save(store);

        log.info("店铺创建成功：店铺ID={}, 商户ID={}", encryptUtils.bytesToUuid(storeId), request.getMerchantId());

        // 4. 构造响应
        return StoreCreateResponse.builder()
                .storeId(encryptUtils.bytesToUuid(storeId))
                .merchantId(request.getMerchantId())
                .storeStatus("ENABLED")
                .createTime(now.format(DATETIME_FORMATTER))
                .build();
    }

    /**
     * 查询店铺详情
     *
     * @param storeId    店铺ID
     * @param merchantId 商户ID（用于权限校验）
     * @return 店铺详情
     */
    public StoreDetailResponse getStoreDetail(String storeId, String merchantId) {
        log.info("查询店铺详情：店铺ID={}, 商户ID={}", storeId, merchantId);

        // 1. 查询店铺信息
        byte[] storeIdBytes = encryptUtils.uuidToBytes(storeId);
        Store store = storeRepository.findById(storeIdBytes)
                .orElseThrow(() -> new BusinessException(ErrorCode.PARAM_ERROR.getCode(), "店铺不存在"));

        // 2. 校验店铺归属（店铺必须属于该商户）
        byte[] merchantIdBytes = encryptUtils.uuidToBytes(merchantId);
        if (!java.util.Arrays.equals(store.getMerchantId(), merchantIdBytes)) {
            throw new BusinessException(ErrorCode.PERMISSION_DENIED.getCode(), "无权限访问该店铺");
        }

        // 3. 构造响应
        return buildStoreDetailResponse(store);
    }

    /**
     * 查询商户的所有店铺列表
     *
     * @param merchantId 商户ID
     * @return 店铺列表
     */
    public StoreListResponse getStoreList(String merchantId) {
        log.info("查询店铺列表：商户ID={}", merchantId);

        // 1. 查询商户的所有店铺（按创建时间降序）
        byte[] merchantIdBytes = encryptUtils.uuidToBytes(merchantId);
        List<Store> stores = storeRepository.findByMerchantIdOrderByCreateTimeDesc(merchantIdBytes);

        // 2. 转换为响应DTO
        List<StoreListResponse.StoreItem> storeItems = stores.stream()
                .map(this::buildStoreItem)
                .collect(Collectors.toList());

        // 3. 构造响应
        return StoreListResponse.builder()
                .total(storeItems.size())
                .list(storeItems)
                .build();
    }

    /**
     * 修改店铺信息
     *
     * @param storeId 店铺ID
     * @param request 店铺修改请求
     * @return 店铺修改响应
     */
    @Transactional
    public StoreUpdateResponse updateStore(String storeId, StoreUpdateRequest request) {
        log.info("修改店铺信息：店铺ID={}, 商户ID={}", storeId, request.getMerchantId());

        // 1. 查询店铺信息
        byte[] storeIdBytes = encryptUtils.uuidToBytes(storeId);
        Store store = storeRepository.findById(storeIdBytes)
                .orElseThrow(() -> new BusinessException(ErrorCode.PARAM_ERROR.getCode(), "店铺不存在"));

        // 2. 校验店铺归属
        byte[] merchantIdBytes = encryptUtils.uuidToBytes(request.getMerchantId());
        if (!java.util.Arrays.equals(store.getMerchantId(), merchantIdBytes)) {
            throw new BusinessException(ErrorCode.PERMISSION_DENIED.getCode(), "无权限修改该店铺");
        }

        // 3. 更新字段（只更新非空字段）
        boolean updated = false;
        if (request.getStoreName() != null && !request.getStoreName().trim().isEmpty()) {
            store.setStoreName(request.getStoreName().trim());
            updated = true;
        }
        if (request.getStoreAddress() != null) {
            store.setAddress(request.getStoreAddress());
            updated = true;
        }
        if (request.getContactPhone() != null && !request.getContactPhone().trim().isEmpty()) {
            store.setContactPhone(request.getContactPhone().trim());
            updated = true;
        }
        if (request.getContactWx() != null && !request.getContactWx().trim().isEmpty()) {
            store.setContactWx(request.getContactWx().trim());
            updated = true;
        }
        if (request.getBusinessHours() != null) {
            store.setBusinessTime(request.getBusinessHours());
            updated = true;
        }
        if (request.getStorePhotos() != null && !request.getStorePhotos().trim().isEmpty()) {
            store.setStorePhotos(request.getStorePhotos().trim());
            updated = true;
        }
        if (request.getBusinessLicense() != null && !request.getBusinessLicense().trim().isEmpty()) {
            store.setBusinessLicense(request.getBusinessLicense().trim());
            updated = true;
        }
        if (request.getAppointment() != null) {
            store.setAppointment(request.getAppointment());
            updated = true;
        }
        if (request.getOpenStoreTime() != null) {
            store.setOpenStoreTime(request.getOpenStoreTime());
            updated = true;
        }

        if (!updated) {
            throw new BusinessException(ErrorCode.PARAM_ERROR.getCode(), "没有需要更新的字段");
        }

        // 4. 保存更新
        LocalDateTime now = LocalDateTime.now();
        store.setUpdateTime(now);
        storeRepository.save(store);

        log.info("店铺信息修改成功：店铺ID={}", storeId);

        // 5. 构造响应
        return StoreUpdateResponse.builder()
                .updateTime(now.format(DATETIME_FORMATTER))
                .build();
    }

    /**
     * 构建店铺详情响应
     *
     * @param store 店铺实体
     * @return 店铺详情响应
     */
    private StoreDetailResponse buildStoreDetailResponse(Store store) {
        StoreDetailResponse.StoreDetailResponseBuilder builder = StoreDetailResponse.builder()
                .storeId(encryptUtils.bytesToUuid(store.getStoreId()))
                .merchantId(encryptUtils.bytesToUuid(store.getMerchantId()))
                .storeName(store.getStoreName())
                .storeType(store.getStoreType())
                .storeAddress(store.getAddress())
                .contactPhone(store.getContactPhone())
                .contactWx(store.getContactWx())
                .businessHours(store.getBusinessTime())
                .storePhotos(store.getStorePhotos())
                .businessLicense(store.getBusinessLicense())
                .appointment(store.getAppointment())
                .storeStatus(store.getStatus())
                .createTime(store.getCreateTime().format(DATETIME_FORMATTER))
                .lastUpdateTime(store.getUpdateTime().format(DATETIME_FORMATTER));

        // 建店时间（可能为空）
        if (store.getOpenStoreTime() != null) {
            builder.openStoreTime(store.getOpenStoreTime().format(DATETIME_FORMATTER));
        }

        return builder.build();
    }

    /**
     * 构建店铺列表项
     *
     * @param store 店铺实体
     * @return 店铺列表项
     */
    private StoreListResponse.StoreItem buildStoreItem(Store store) {
        return StoreListResponse.StoreItem.builder()
                .storeId(encryptUtils.bytesToUuid(store.getStoreId()))
                .storeName(store.getStoreName())
                .storeType(store.getStoreType())
                .storeStatus(store.getStatus())
                .createTime(store.getCreateTime().format(DATETIME_FORMATTER))
                .build();
    }
}

