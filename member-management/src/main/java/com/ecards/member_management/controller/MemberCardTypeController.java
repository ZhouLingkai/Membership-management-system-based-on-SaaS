package com.ecards.member_management.controller;

import com.ecards.member_management.common.Result;
import com.ecards.member_management.constants.TokenConstants;
import com.ecards.member_management.dto.request.CreateCardTypeRequest;
import com.ecards.member_management.dto.request.DetailCardTypeRequest;
import com.ecards.member_management.dto.request.ListCardTypeRequest;
import com.ecards.member_management.dto.request.UpdateCardTypeRequest;
import com.ecards.member_management.dto.response.CardTypeDetailResponse;
import com.ecards.member_management.dto.response.CardTypeListResponse;
import com.ecards.member_management.dto.response.CreateCardTypeResponse;
import com.ecards.member_management.dto.response.UpdateCardTypeResponse;
import com.ecards.member_management.service.MemberCardTypeService;
import com.ecards.member_management.utils.JwtUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * 会员卡种管理Controller
 * 
 * 接口列表：
 * 1. POST /api/v1/member-card-types/create - 会员卡种创建
 * 2. GET /api/v1/member-card-types/list-query - 会员卡种列表查询
 * 3. GET /api/v1/member-card-types/detail-query - 会员卡种详情查询
 * 4. POST /api/v1/member-card-types/set - 会员卡种修改设置
 * 
 * 权限规则：
 * - 创建：仅商家（普通令牌）
 * - 列表/详情查询：任意有效令牌
 * - 修改：商家（普通令牌）或店长（工作令牌）
 * 
 * @author Ecards Team
 * @since 2025-11-02
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/member-card-types")
@RequiredArgsConstructor
public class MemberCardTypeController {

    private final MemberCardTypeService memberCardTypeService;
    private final JwtUtils jwtUtils;

    /**
     * 接口1：会员卡种创建
     * POST /api/v1/member-card-types/create
     * 
     * 权限：仅商家（普通令牌）
     */
    @PostMapping("/create")
    public Result<CreateCardTypeResponse> createCardType(
            @Valid @RequestBody CreateCardTypeRequest request,
            @RequestHeader(TokenConstants.AUTHORIZATION_HEADER) String authorization) {
        
        try {
            // 1. 提取令牌
            String token = extractBearerToken(authorization);
            if (token == null) {
                return Result.fail("Authorization格式错误");
            }

            // 2. 验证是否为普通令牌
            Integer tokenType = jwtUtils.extractTokenType(token);
            if (tokenType != 1) {
                return Result.fail(10004, "只能使用普通令牌创建卡种");
            }

            // 3. 验证用户角色是否为商家
            String role = jwtUtils.extractRole(token);
            if (!"MERCHANT".equals(role)) {
                return Result.fail(40001, "非商家用户，无权限操作");
            }

            // 4. 提取商家ID
            String merchantId = jwtUtils.extractMerchantId(token);
            if (merchantId == null || merchantId.isEmpty()) {
                return Result.fail("令牌中缺少商家ID");
            }

            log.info("收到会员卡种创建请求：商家ID={}, 店铺ID={}, 卡种名称={}", 
                    merchantId, request.getStoreId(), request.getCardTypeName());

            // 5. 调用Service层
            CreateCardTypeResponse response = memberCardTypeService.createCardType(request, merchantId);

            return Result.success("会员卡种创建成功", response);

        } catch (Exception e) {
            log.error("会员卡种创建失败", e);
            throw e;
        }
    }

    /**
     * 接口2：会员卡种列表查询
     * GET /api/v1/member-card-types/list-query
     * 
     * 权限：任意有效令牌
     */
    @GetMapping("/list-query")
    public Result<CardTypeListResponse> listCardTypes(
            @Valid ListCardTypeRequest request,
            @RequestHeader(TokenConstants.AUTHORIZATION_HEADER) String authorization) {
        
        try {
            // 1. 验证令牌有效性（仅验证格式，不验证权限）
            String token = extractBearerToken(authorization);
            if (token == null) {
                return Result.fail("Authorization格式错误");
            }

            log.info("收到会员卡种列表查询请求：店铺ID={}, 页码={}, 每页条数={}", 
                    request.getStoreId(), request.getPageNum(), request.getPageSize());

            // 2. 调用Service层
            CardTypeListResponse response = memberCardTypeService.listCardTypes(request);

            return Result.success("会员卡种列表查询成功", response);

        } catch (Exception e) {
            log.error("会员卡种列表查询失败", e);
            throw e;
        }
    }

    /**
     * 接口3：会员卡种详情查询
     * GET /api/v1/member-card-types/detail-query
     * 
     * 权限：任意有效令牌
     */
    @GetMapping("/detail-query")
    public Result<CardTypeDetailResponse> getCardTypeDetail(
            @Valid DetailCardTypeRequest request,
            @RequestHeader(TokenConstants.AUTHORIZATION_HEADER) String authorization) {
        
        try {
            // 1. 验证令牌有效性（仅验证格式，不验证权限）
            String token = extractBearerToken(authorization);
            if (token == null) {
                return Result.fail("Authorization格式错误");
            }

            log.info("收到会员卡种详情查询请求：卡种ID={}, 店铺ID={}", 
                    request.getCardTypeId(), request.getStoreId());

            // 2. 调用Service层
            CardTypeDetailResponse response = memberCardTypeService.getCardTypeDetail(request);

            return Result.success("会员卡种详情查询成功", response);

        } catch (Exception e) {
            log.error("会员卡种详情查询失败", e);
            throw e;
        }
    }

    /**
     * 接口4：会员卡种修改设置
     * POST /api/v1/member-card-types/set
     * 
     * 权限：商家（普通令牌）或店长（工作令牌）
     */
    @PostMapping("/set")
    public Result<UpdateCardTypeResponse> updateCardType(
            @Valid @RequestBody UpdateCardTypeRequest request,
            @RequestHeader(TokenConstants.AUTHORIZATION_HEADER) String authorization) {
        
        try {
            // 1. 提取令牌
            String token = extractBearerToken(authorization);
            if (token == null) {
                return Result.fail("Authorization格式错误");
            }

            // 2. 获取令牌类型
            Integer tokenType = jwtUtils.extractTokenType(token);

            String merchantId = null;
            String storeIdFromToken = null;

            // 3. 根据令牌类型提取必要信息
            if (tokenType == 1) {
                // 普通令牌（商家）
                String role = jwtUtils.extractRole(token);
                if (!"MERCHANT".equals(role)) {
                    return Result.fail(40001, "非商家用户，无权限操作");
                }
                merchantId = jwtUtils.extractMerchantId(token);
                
            } else if (tokenType == 3) {
                // 工作令牌（商家或店长）
                String role = jwtUtils.extractRole(token);
                if ("merchant".equals(role)) {
                    // 商家使用工作令牌（注意：工作令牌中商家role为小写"merchant"）
                    storeIdFromToken = jwtUtils.extractStoreId(token);
                } else if ("manager".equals(role)) {
                    // 店长使用工作令牌
                    storeIdFromToken = jwtUtils.extractStoreId(token);
                } else {
                    return Result.fail(10004, "工作令牌角色无效，只有商家或店长才能修改卡种");
                }
                
            } else {
                return Result.fail(10004, "令牌类型不支持该操作");
            }

            log.info("收到会员卡种修改请求：卡种ID={}, 店铺ID={}, 令牌类型={}", 
                    request.getCardTypeId(), request.getStoreId(), tokenType);

            // 4. 调用Service层
            UpdateCardTypeResponse response = memberCardTypeService.updateCardType(
                    request, merchantId, storeIdFromToken);

            return Result.success("会员卡种修改成功", response);

        } catch (Exception e) {
            log.error("会员卡种修改失败", e);
            throw e;
        }
    }

    /**
     * 提取Bearer令牌
     */
    private String extractBearerToken(String authorization) {
        if (authorization != null && authorization.startsWith("Bearer ")) {
            return authorization.substring(7);
        }
        return null;
    }
}

