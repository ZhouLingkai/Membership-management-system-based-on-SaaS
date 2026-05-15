package com.ecards.member_management.controller;

import com.ecards.member_management.annotation.CheckStoreLimit;
import com.ecards.member_management.annotation.Idempotent;
import com.ecards.member_management.annotation.RequireMerchantActive;
import com.ecards.member_management.common.Result;
import com.ecards.member_management.dto.request.StoreCreateRequest;
import com.ecards.member_management.dto.request.StoreUpdateRequest;
import com.ecards.member_management.dto.response.StoreCreateResponse;
import com.ecards.member_management.dto.response.StoreDetailResponse;
import com.ecards.member_management.dto.response.StoreListResponse;
import com.ecards.member_management.dto.response.StoreUpdateResponse;
import com.ecards.member_management.service.StoreService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * 店铺管理控制器
 * 提供店铺创建、查询、修改等接口
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/stores")
@RequiredArgsConstructor
public class StoreController {

    private final StoreService storeService;

    /**
     * 接口1：创建店铺
     * POST /api/v1/stores
     *
     * @param request 店铺创建请求
     * @return 店铺创建响应
     * 幂等性：需要前端携带X-Request-ID请求头
     */
    @PostMapping
    @RequireMerchantActive(message = "商户已被封禁，无法创建店铺")
    @CheckStoreLimit(message = "店铺数量已达上限，请升级VIP获取更多店铺配额")
    @Idempotent(timeout = 600, value = "店铺创建")
    public Result<StoreCreateResponse> createStore(@Valid @RequestBody StoreCreateRequest request) {
        log.info("接收到店铺创建请求：商户ID={}, 店铺名称={}", request.getMerchantId(), request.getStoreName());

        StoreCreateResponse response = storeService.createStore(request);

        return Result.success("店铺创建成功", response);
    }

    /**
     * 接口2：查询店铺详情（单个）
     * GET /api/v1/stores/{storeId}
     *
     * @param storeId    店铺ID（路径参数）
     * @param merchantId 商户ID（查询参数，用于权限校验）
     * @return 店铺详情
     */
    @GetMapping("/{storeId}")
    @RequireMerchantActive(message = "商户已被封禁，无法查询店铺信息")
    public Result<StoreDetailResponse> getStoreDetail(
            @PathVariable String storeId,
            @RequestParam String merchantId) {
        log.info("接收到店铺详情查询请求：店铺ID={}, 商户ID={}", storeId, merchantId);

        StoreDetailResponse response = storeService.getStoreDetail(storeId, merchantId);

        return Result.success("店铺信息查询成功", response);
    }

    /**
     * 接口3：查询店铺列表（商户下所有店铺）
     * GET /api/v1/stores
     *
     * @param merchantId 商户ID（查询参数）
     * @return 店铺列表
     */
    @GetMapping
    @RequireMerchantActive(message = "商户已被封禁，无法查询店铺列表")
    public Result<StoreListResponse> getStoreList(@RequestParam String merchantId) {
        log.info("接收到店铺列表查询请求：商户ID={}", merchantId);

        StoreListResponse response = storeService.getStoreList(merchantId);

        return Result.success("店铺列表查询成功", response);
    }

    /**
     * 接口4：修改店铺信息
     * PUT /api/v1/stores/{storeId}
     *
     * @param storeId 店铺ID（路径参数）
     * @param request 店铺修改请求
     * @return 店铺修改响应
     */
    @PutMapping("/{storeId}")
    @RequireMerchantActive(message = "商户已被封禁，无法修改店铺信息")
    public Result<StoreUpdateResponse> updateStore(
            @PathVariable String storeId,
            @Valid @RequestBody StoreUpdateRequest request) {
        log.info("接收到店铺信息修改请求：店铺ID={}, 商户ID={}", storeId, request.getMerchantId());

        StoreUpdateResponse response = storeService.updateStore(storeId, request);

        return Result.success("店铺信息修改成功", response);
    }
}

