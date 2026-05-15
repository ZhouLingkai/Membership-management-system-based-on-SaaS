package com.ecards.member_management.controller;

import com.ecards.member_management.common.ErrorCode;
import com.ecards.member_management.common.Result;
import com.ecards.member_management.constants.TokenConstants;
import com.ecards.member_management.context.TokenContext;
import com.ecards.member_management.dto.request.*;
import com.ecards.member_management.dto.response.*;
import com.ecards.member_management.exception.BusinessException;
import com.ecards.member_management.service.MemberCardService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * 会员卡管理Controller
 * 
 * 接口列表：
 * 1. POST /api/v1/member-cards/create-by-phone - 会员卡办理（手机号快速办理）
 * 2. POST /api/v1/member-cards/create-by-scan - 会员卡办理（线下扫码办理）
 * 3. GET /api/v1/member-cards/merchant-list - 查询商家会员卡列表
 * 4. GET /api/v1/member-cards/store-list - 查询店铺会员卡列表
 * 
 * 权限规则：
 * - 接口1、2：商家（工作令牌或普通令牌）、店长、店员
 * - 接口3：商家（普通令牌）
 * - 接口4：商家（工作令牌或普通令牌）、店长
 * 
 * @author Ecards Team
 * @since 2025-11-03
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/member-cards")
@RequiredArgsConstructor
public class MemberCardController {

    private final MemberCardService memberCardService;

    /**
     * 接口1：会员卡办理（手机号快速办理）
     * POST /api/v1/member-cards/create-by-phone
     * 
     * 权限：商家/店长/店员（工作令牌或普通令牌）
     */
    @PostMapping("/create-by-phone")
    public Result<CreateMemberCardResponse> createByPhone(
            @Valid @RequestBody CreateMemberCardByPhoneRequest request,
            @RequestHeader(TokenConstants.AUTHORIZATION_HEADER) String authorization) {
        
        try {
            // 1. 提取令牌
            String token = extractBearerToken(authorization);
            if (token == null) {
                return Result.fail("Authorization格式错误");
            }

            log.info("收到手机号办卡请求：店铺ID={}, 卡种ID={}, 手机号=***", 
                    request.getStoreId(), request.getCardTypeId());

            // 2. 调用Service层
            CreateMemberCardResponse response = memberCardService.createByPhone(request, token);

            return Result.success("会员卡办理成功", response);

        } catch (Exception e) {
            log.error("手机号办卡失败", e);
            throw e;
        }
    }

    /**
     * 接口2：会员卡办理（线下扫码办理）
     * POST /api/v1/member-cards/create-by-scan
     * 
     * 权限：商家/店长/店员（工作令牌或普通令牌）
     */
    @PostMapping("/create-by-scan")
    public Result<CreateMemberCardResponse> createByScan(
            @Valid @RequestBody CreateMemberCardByScanRequest request,
            @RequestHeader(TokenConstants.AUTHORIZATION_HEADER) String authorization) {
        
        try {
            // 1. 提取令牌
            String token = extractBearerToken(authorization);
            if (token == null) {
                return Result.fail("Authorization格式错误");
            }

            log.info("收到扫码办卡请求：店铺ID={}, 卡种ID={}", 
                    request.getStoreId(), request.getCardTypeId());

            // 2. 调用Service层
            CreateMemberCardResponse response = memberCardService.createByScan(request, token);

            return Result.success("会员卡办理成功", response);

        } catch (Exception e) {
            log.error("扫码办卡失败", e);
            throw e;
        }
    }

    /**
     * 接口3：查询商家会员卡列表（分页）
     * GET /api/v1/member-cards/merchant-list
     * 
     * 权限：商家（普通令牌）
     */
    @GetMapping("/merchant-list")
    public Result<MemberCardListResponse> queryMerchantCardList(
            @Valid QueryMerchantCardListRequest request,
            @RequestHeader(TokenConstants.AUTHORIZATION_HEADER) String authorization) {
        
        try {
            // 1. 提取令牌
            String token = extractBearerToken(authorization);
            if (token == null) {
                return Result.fail("Authorization格式错误");
            }

            log.info("收到查询商家会员卡列表请求：pageNum={}, pageSize={}", 
                    request.getPageNum(), request.getPageSize());

            // 2. 调用Service层
            MemberCardListResponse response = memberCardService.queryMerchantCardList(request, token);

            return Result.success("查询成功", response);

        } catch (Exception e) {
            log.error("查询商家会员卡列表失败", e);
            throw e;
        }
    }

    /**
     * 接口4：查询店铺会员卡列表（分页，支持本店卡/跨店卡切换）
     * GET /api/v1/member-cards/store-list
     * 
     * 权限：商家/店长（工作令牌或普通令牌）
     */
    @GetMapping("/store-list")
    public Result<MemberCardListResponse> queryStoreCardList(
            @Valid QueryStoreCardListRequest request,
            @RequestHeader(TokenConstants.AUTHORIZATION_HEADER) String authorization) {
        
        try {
            // 1. 提取令牌
            String token = extractBearerToken(authorization);
            if (token == null) {
                return Result.fail("Authorization格式错误");
            }

            log.info("收到查询店铺会员卡列表请求：店铺ID={}, cardScope={}, pageNum={}, pageSize={}", 
                    request.getStoreId(), request.getCardScope(), request.getPageNum(), request.getPageSize());

            // 2. 调用Service层
            MemberCardListResponse response = memberCardService.queryStoreCardList(request, token);

            return Result.success("查询成功", response);

        } catch (Exception e) {
            log.error("查询店铺会员卡列表失败", e);
            throw e;
        }
    }

    /**
     * 接口5：查询商家会员统计数据
     * GET /api/v1/member-cards/merchant-statistics
     * 
     * 权限：商家（普通令牌）
     * 
     * 注意：与交易管理模块命名冲突，临时注释
     * TODO: 需要创建专门的会员统计Response类后重新启用
     */
    /*
    @GetMapping("/merchant-statistics")
    public Result<MerchantStatisticsResponse> getMerchantStatistics(
            @RequestHeader(TokenConstants.AUTHORIZATION_HEADER) String authorization) {
        
        try {
            String token = extractBearerToken(authorization);
            if (token == null) {
                return Result.fail(401, "令牌缺失");
            }

            // 从TokenContext获取商家ID（不需要校验工作关系）
            TokenContext.TokenInfo tokenInfo = TokenContext.get();
            if (tokenInfo == null || !"MERCHANT".equals(tokenInfo.getRole())) {
                return Result.fail(40001, "非商家用户，无权限操作");
            }

            String merchantId = tokenInfo.getMerchantId();
            if (merchantId == null) {
                return Result.fail(30002, "商家信息不存在");
            }

            MerchantStatisticsResponse response = memberCardService.getMerchantStatistics(merchantId);
            return Result.success("查询成功", response);
            
        } catch (BusinessException e) {
            return Result.fail(e.getCode(), e.getMessage());
        } catch (Exception e) {
            log.error("查询商家会员统计数据失败", e);
            return Result.fail(ErrorCode.SYSTEM_ERROR.getCode(), ErrorCode.SYSTEM_ERROR.getMessage());
        }
    }
    */

    /**
     * 接口6：查询店铺会员统计数据
     * GET /api/v1/member-cards/store-statistics
     * 
     * 权限：商家/店长/店员（工作令牌或商家普通令牌）
     * 
     * 注意：与交易管理模块命名冲突，临时注释
     * TODO: 需要创建专门的会员统计Response类后重新启用
     */
    /*
    @GetMapping("/store-statistics")
    public Result<StoreStatisticsResponse> getStoreStatistics(
            @RequestParam("storeId") String storeId,
            @RequestHeader(TokenConstants.AUTHORIZATION_HEADER) String authorization) {
        
        try {
            String token = extractBearerToken(authorization);
            if (token == null) {
                return Result.fail(401, "令牌缺失");
            }

            TokenContext.TokenInfo tokenInfo = TokenContext.get();
            String merchantId;

            // 支持普通令牌（商家）和工作令牌
            if (tokenInfo.getTokenType() == 3) {
                // 工作令牌：验证工作店铺
                if (!storeId.equals(tokenInfo.getStoreId())) {
                    return Result.fail(40001, "无权查询该店铺");
                }
                merchantId = tokenInfo.getMerchantId();
            } else if (tokenInfo.getTokenType() == 1 && "MERCHANT".equals(tokenInfo.getRole())) {
                // 商家普通令牌
                merchantId = tokenInfo.getMerchantId();
            } else {
                return Result.fail(ErrorCode.TOKEN_INVALID.getCode(), "需要工作令牌或商家普通令牌");
            }

            StoreStatisticsResponse response = memberCardService.getStoreStatistics(storeId, merchantId);
            return Result.success("查询成功", response);
            
        } catch (BusinessException e) {
            return Result.fail(e.getCode(), e.getMessage());
        } catch (Exception e) {
            log.error("查询店铺会员统计数据失败", e);
            return Result.fail(ErrorCode.SYSTEM_ERROR.getCode(), ErrorCode.SYSTEM_ERROR.getMessage());
        }
    }
    */

    /**
     * 接口7：用户查询自己的会员卡列表
     * GET /api/v1/member-cards/my-list
     * 
     * 权限：任意用户（普通令牌）
     */
    @GetMapping("/my-list")
    public Result<MemberCardListResponse> getMyCardList(
            @Valid @ModelAttribute QueryMyCardListRequest request,
            @RequestHeader(TokenConstants.AUTHORIZATION_HEADER) String authorization) {
        
        try {
            String token = extractBearerToken(authorization);
            if (token == null) {
                return Result.fail(401, "令牌缺失");
            }

            TokenContext.TokenInfo tokenInfo = TokenContext.get();
            
            // 验证令牌类型为普通令牌
            if (tokenInfo.getTokenType() != 1) {
                return Result.fail(ErrorCode.TOKEN_INVALID.getCode(), "需要普通令牌");
            }

            String userId = tokenInfo.getUserId();
            if (userId == null) {
                return Result.fail(20003, "用户信息不存在");
            }

            MemberCardListResponse response = memberCardService.getMyCardList(userId, request);
            return Result.success("查询成功", response);
            
        } catch (BusinessException e) {
            return Result.fail(e.getCode(), e.getMessage());
        } catch (Exception e) {
            log.error("查询用户会员卡列表失败", e);
            return Result.fail(ErrorCode.SYSTEM_ERROR.getCode(), ErrorCode.SYSTEM_ERROR.getMessage());
        }
    }

    /**
     * 接口8：通过手机号查询会员卡
     * GET /api/v1/member-cards/query-by-phone
     * 
     * 权限：商家/店长/店员（工作令牌或商家普通令牌）
     */
    @GetMapping("/query-by-phone")
    public Result<QueryByPhoneResponse> queryByPhone(
            @RequestParam("storeId") String storeId,
            @RequestParam("memberPhone") String memberPhone,
            @RequestHeader(TokenConstants.AUTHORIZATION_HEADER) String authorization) {
        
        try {
            String token = extractBearerToken(authorization);
            if (token == null) {
                return Result.fail(401, "令牌缺失");
            }

            TokenContext.TokenInfo tokenInfo = TokenContext.get();
            String merchantId;

            // 验证令牌类型
            if (tokenInfo.getTokenType() == 3) {
                // 工作令牌：验证工作店铺
                if (!storeId.equals(tokenInfo.getStoreId())) {
                    return Result.fail(40001, "无权查询该店铺");
                }
                merchantId = tokenInfo.getMerchantId();
            } else if (tokenInfo.getTokenType() == 1 && "MERCHANT".equals(tokenInfo.getRole())) {
                // 商家普通令牌
                merchantId = tokenInfo.getMerchantId();
            } else {
                return Result.fail(ErrorCode.TOKEN_INVALID.getCode(), "需要工作令牌或商家普通令牌");
            }

            QueryByPhoneResponse response = memberCardService.queryByPhone(storeId, memberPhone, merchantId);
            return Result.success("查询成功", response);
            
        } catch (BusinessException e) {
            return Result.fail(e.getCode(), e.getMessage());
        } catch (Exception e) {
            log.error("通过手机号查询会员卡失败", e);
            return Result.fail(ErrorCode.SYSTEM_ERROR.getCode(), ErrorCode.SYSTEM_ERROR.getMessage());
        }
    }

    /**
     * 接口9：会员卡详情查询
     * GET /api/v1/member-cards/detail
     * 
     * 权限：任意持有合法令牌的用户
     */
    @GetMapping("/detail")
    public Result<MemberCardDetailResponse> getCardDetail(
            @RequestParam("memberCardId") String memberCardId,
            @RequestHeader(TokenConstants.AUTHORIZATION_HEADER) String authorization) {
        
        try {
            String token = extractBearerToken(authorization);
            if (token == null) {
                return Result.fail(401, "令牌缺失");
            }

            // 仅校验令牌合法性，无需额外权限校验
            TokenContext.TokenInfo tokenInfo = TokenContext.get();
            if (tokenInfo == null) {
                return Result.fail(ErrorCode.TOKEN_INVALID.getCode(), "令牌无效");
            }

            MemberCardDetailResponse response = memberCardService.getCardDetail(memberCardId);
            return Result.success("查询成功", response);
            
        } catch (BusinessException e) {
            return Result.fail(e.getCode(), e.getMessage());
        } catch (Exception e) {
            log.error("查询会员卡详情失败", e);
            return Result.fail(ErrorCode.SYSTEM_ERROR.getCode(), ErrorCode.SYSTEM_ERROR.getMessage());
        }
    }

    /**
     * 接口11：批量激活会员卡（手机号批量激活）
     * POST /api/v1/member-cards/activate-batch
     * 
     * 权限：普通用户（普通令牌）
     */
    @PostMapping("/activate-batch")
    public Result<ActivateBatchResponse> activateBatch(
            @RequestHeader(TokenConstants.AUTHORIZATION_HEADER) String authorization) {
        try {
            // 1. 提取并验证令牌
            String token = extractBearerToken(authorization);
            if (token == null) {
                return Result.fail(ErrorCode.TOKEN_INVALID.getCode(), "令牌格式错误");
            }

            // 2. 从TokenContext获取用户信息
            TokenContext.TokenInfo tokenInfo = TokenContext.get();
            if (tokenInfo == null || tokenInfo.getUserId() == null) {
                return Result.fail(ErrorCode.TOKEN_INVALID.getCode(), "无法获取用户信息");
            }
            String userId = tokenInfo.getUserId();

            // 3. 从令牌获取手机号（需要从用户表查询）
            // 注意：这里假设用户手机号存储在用户表中
            // 需要从UserRepository查询用户信息获取手机号
            // 暂时直接传userId给Service层处理
            
            // 调用服务层（注意：需要修改Service层方法，让其自行查询用户手机号）
            ActivateBatchResponse response = memberCardService.activateBatch(userId, null);
            return Result.success("批量激活成功", response);
            
        } catch (BusinessException e) {
            return Result.fail(e.getCode(), e.getMessage());
        } catch (Exception e) {
            log.error("批量激活会员卡失败", e);
            return Result.fail(ErrorCode.SYSTEM_ERROR.getCode(), ErrorCode.SYSTEM_ERROR.getMessage());
        }
    }

    /**
     * 接口12：冻结会员卡
     * POST /api/v1/member-cards/freeze
     * 
     * 权限：商家/店长（工作令牌）、用户本人（普通令牌）
     */
    @PostMapping("/freeze")
    public Result<FreezeCardResponse> freezeCard(
            @Valid @RequestBody FreezeCardRequest request,
            @RequestHeader(TokenConstants.AUTHORIZATION_HEADER) String authorization) {
        try {
            // 1. 提取并验证令牌
            String token = extractBearerToken(authorization);
            if (token == null) {
                return Result.fail(ErrorCode.TOKEN_INVALID.getCode(), "令牌格式错误");
            }

            // 2. 调用服务层进行冻结操作（权限验证在Service层）
            FreezeCardResponse response = memberCardService.freezeCard(request, token);
            return Result.success("会员卡冻结成功", response);
            
        } catch (BusinessException e) {
            return Result.fail(e.getCode(), e.getMessage());
        } catch (Exception e) {
            log.error("冻结会员卡失败", e);
            return Result.fail(ErrorCode.SYSTEM_ERROR.getCode(), ErrorCode.SYSTEM_ERROR.getMessage());
        }
    }

    /**
     * 接口13：解冻会员卡
     * POST /api/v1/member-cards/unfreeze
     * 
     * 权限：商家/店长（工作令牌）、用户本人（普通令牌）
     */
    @PostMapping("/unfreeze")
    public Result<UnfreezeCardResponse> unfreezeCard(
            @Valid @RequestBody UnfreezeCardRequest request,
            @RequestHeader(TokenConstants.AUTHORIZATION_HEADER) String authorization) {
        try {
            // 1. 提取并验证令牌
            String token = extractBearerToken(authorization);
            if (token == null) {
                return Result.fail(ErrorCode.TOKEN_INVALID.getCode(), "令牌格式错误");
            }

            // 2. 调用服务层进行解冻操作（权限验证在Service层）
            UnfreezeCardResponse response = memberCardService.unfreezeCard(request, token);
            return Result.success("会员卡解冻成功", response);
            
        } catch (BusinessException e) {
            return Result.fail(e.getCode(), e.getMessage());
        } catch (Exception e) {
            log.error("解冻会员卡失败", e);
            return Result.fail(ErrorCode.SYSTEM_ERROR.getCode(), ErrorCode.SYSTEM_ERROR.getMessage());
        }
    }

    /**
     * 接口15：线下扫码查询个人会员卡
     * POST /api/v1/member-cards/query-by-scan
     * 
     * 权限：商家/店长/店员（工作令牌或商家普通令牌）
     */
    @PostMapping("/query-by-scan")
    public Result<?> queryByScan(
            @RequestBody @Valid QueryByScanRequest request,
            @RequestHeader(TokenConstants.AUTHORIZATION_HEADER) String authorization) {
        
        try {
            String token = extractBearerToken(authorization);
            if (token == null) {
                return Result.fail(401, "令牌缺失");
            }

            TokenContext.TokenInfo tokenInfo = TokenContext.get();
            String merchantId;

            // 验证令牌类型
            if (tokenInfo.getTokenType() == 3) {
                // 工作令牌：验证工作店铺
                if (!request.getStoreId().equals(tokenInfo.getStoreId())) {
                    return Result.fail(40001, "无权查询该店铺");
                }
                merchantId = tokenInfo.getMerchantId();
            } else if (tokenInfo.getTokenType() == 1 && "MERCHANT".equals(tokenInfo.getRole())) {
                // 商家普通令牌
                merchantId = tokenInfo.getMerchantId();
                // 验证店铺归属
                // TODO: 这里可能需要查询店铺表验证merchantId，暂时简化处理
            } else {
                return Result.fail(ErrorCode.TOKEN_INVALID.getCode(), "需要工作令牌或商家普通令牌");
            }

            // 根据memberCardId有无决定调用哪个方法
            if (request.getMemberCardId() != null && !request.getMemberCardId().isEmpty()) {
                // 场景A：会员卡详情查询
                MemberCardDetailResponse response = memberCardService.queryByScanDetail(
                        request.getStoreId(),
                        request.getPrivilegeToken(),
                        request.getMemberCardId(),
                        merchantId
                );
                return Result.success("查询成功", response);
            } else {
                // 场景B：会员卡列表查询
                QueryByPhoneResponse response = memberCardService.queryByScanList(
                        request.getStoreId(),
                        request.getPrivilegeToken(),
                        merchantId
                );
                return Result.success("查询成功", response);
            }
            
        } catch (BusinessException e) {
            return Result.fail(e.getCode(), e.getMessage());
        } catch (Exception e) {
            log.error("线下扫码查询会员卡失败", e);
            return Result.fail(ErrorCode.SYSTEM_ERROR.getCode(), ErrorCode.SYSTEM_ERROR.getMessage());
        }
    }

    // ==================== 辅助方法 ====================

    /**
     * 从Authorization头部提取Bearer Token
     */
    private String extractBearerToken(String authorization) {
        if (authorization != null && authorization.startsWith("Bearer ")) {
            return authorization.substring(7);
        }
        return null;
    }
}

