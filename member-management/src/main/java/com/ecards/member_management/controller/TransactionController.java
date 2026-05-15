package com.ecards.member_management.controller;

import com.ecards.member_management.annotation.Idempotent;
import com.ecards.member_management.common.Result;
import com.ecards.member_management.constants.TokenConstants;
import com.ecards.member_management.dto.request.*;
import com.ecards.member_management.dto.response.*;
import com.ecards.member_management.service.TransactionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * 交易管理REST控制器
 * 
 * @author Ecards Team
 * @since 2025-11-05
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/transactions")
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionService transactionService;

    /**
     * 接口1：会员卡充值
     * 
     * @param request 充值请求
     * @param token Bearer令牌（工作令牌或普通令牌）
     * @param deviceId 设备ID
     * @return 充值响应
     */
    @PostMapping("/recharge")
    @Idempotent(timeout = 600, value = "会员卡充值")
    public Result<RechargeResponse> recharge(
            @Valid @RequestBody RechargeRequest request,
            @RequestHeader(TokenConstants.AUTHORIZATION_HEADER) String token,
            @RequestHeader(TokenConstants.DEVICE_ID_HEADER) String deviceId) {
        
        log.info("接收会员卡充值请求：memberCardId={}, storeId={}, amount={}", 
                request.getMemberCardId(), request.getStoreId(), request.getAmount());
        
        // 去除Bearer前缀
        String pureToken = token.replace("Bearer ", "").trim();
        
        RechargeResponse response = transactionService.recharge(request, pureToken);
        return Result.success("充值成功", response);
    }

    /**
     * 接口2：时效调整
     * 
     * @param request 时效调整请求
     * @param token Bearer令牌（工作令牌或普通令牌）
     * @param deviceId 设备ID
     * @return 时效调整响应
     */
    @PostMapping("/expire-adjust")
    @Idempotent(timeout = 600, value = "时效调整")
    public Result<ExpireAdjustResponse> expireAdjust(
            @Valid @RequestBody ExpireAdjustRequest request,
            @RequestHeader(TokenConstants.AUTHORIZATION_HEADER) String token,
            @RequestHeader(TokenConstants.DEVICE_ID_HEADER) String deviceId) {
        
        log.info("接收时效调整请求：memberCardId={}, storeId={}, adjustType={}", 
                request.getMemberCardId(), request.getStoreId(), request.getAdjustType());
        
        // 去除Bearer前缀
        String pureToken = token.replace("Bearer ", "").trim();
        
        ExpireAdjustResponse response = transactionService.expireAdjust(request, pureToken);
        String message = request.getAdjustType() == 1 ? "延期成功" : "到期时间调整成功";
        return Result.success(message, response);
    }

    /**
     * 接口3：会员卡消费
     * 
     * @param request 消费请求
     * @param token Bearer令牌（工作令牌或普通令牌）
     * @param deviceId 设备ID
     * @return 消费响应
     */
    @PostMapping("/consume")
    @Idempotent(timeout = 600, value = "会员卡消费")
    public Result<ConsumeResponse> consume(
            @Valid @RequestBody ConsumeRequest request,
            @RequestHeader(TokenConstants.AUTHORIZATION_HEADER) String token,
            @RequestHeader(TokenConstants.DEVICE_ID_HEADER) String deviceId) {
        
        log.info("接收会员卡消费请求：memberCardId={}, storeId={}, amount={}", 
                request.getMemberCardId(), request.getStoreId(), request.getAmount());
        
        // 去除Bearer前缀
        String pureToken = token.replace("Bearer ", "").trim();
        
        ConsumeResponse response = transactionService.consume(request, pureToken);
        return Result.success("消费成功", response);
    }

    /**
     * 接口4：单卡交易记录查询
     * 
     * @param memberCardId 会员卡ID
     * @param transactionType 交易类型（可选）
     * @param startDate 开始日期（可选）
     * @param endDate 结束日期（可选）
     * @param pageNum 页码
     * @param pageSize 每页条数
     * @param token Bearer令牌
     * @param deviceId 设备ID
     * @return 交易记录查询响应
     */
    @GetMapping("/card-records")
    public Result<CardRecordsQueryResponse> queryCardRecords(
            @RequestParam String memberCardId,
            @RequestParam(required = false) Integer transactionType,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam Integer pageNum,
            @RequestParam Integer pageSize,
            @RequestHeader(TokenConstants.AUTHORIZATION_HEADER) String token,
            @RequestHeader(TokenConstants.DEVICE_ID_HEADER) String deviceId) {
        
        log.info("接收单卡交易记录查询请求：memberCardId={}, pageNum={}, pageSize={}", 
                memberCardId, pageNum, pageSize);
        
        CardRecordsQueryRequest request = CardRecordsQueryRequest.builder()
                .memberCardId(memberCardId)
                .transactionType(transactionType)
                .startDate(startDate)
                .endDate(endDate)
                .pageNum(pageNum)
                .pageSize(pageSize)
                .build();
        
        // 去除Bearer前缀
        String pureToken = token.replace("Bearer ", "").trim();
        
        CardRecordsQueryResponse response = transactionService.queryCardRecords(request, pureToken);
        return Result.success("查询成功", response);
    }

    /**
     * 接口5：个人交易记录查询
     * 
     * @param transactionType 交易类型（可选）
     * @param startDate 开始日期（可选）
     * @param endDate 结束日期（可选）
     * @param pageNum 页码
     * @param pageSize 每页条数
     * @param token Bearer令牌（普通令牌）
     * @param deviceId 设备ID
     * @return 个人交易记录查询响应
     */
    @GetMapping("/my-records")
    public Result<MyRecordsQueryResponse> queryMyRecords(
            @RequestParam(required = false) Integer transactionType,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam Integer pageNum,
            @RequestParam Integer pageSize,
            @RequestHeader(TokenConstants.AUTHORIZATION_HEADER) String token,
            @RequestHeader(TokenConstants.DEVICE_ID_HEADER) String deviceId) {
        
        log.info("接收个人交易记录查询请求：pageNum={}, pageSize={}", pageNum, pageSize);
        
        MyRecordsQueryRequest request = MyRecordsQueryRequest.builder()
                .transactionType(transactionType)
                .startDate(startDate)
                .endDate(endDate)
                .pageNum(pageNum)
                .pageSize(pageSize)
                .build();
        
        // 去除Bearer前缀
        String pureToken = token.replace("Bearer ", "").trim();
        
        MyRecordsQueryResponse response = transactionService.queryMyRecords(request, pureToken);
        return Result.success("查询成功", response);
    }

    /**
     * 接口6：店铺交易统计
     * 
     * @param storeId 店铺ID
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @param transactionType 交易类型（可选）
     * @param token Bearer令牌
     * @param deviceId 设备ID
     * @return 店铺交易统计响应
     */
    @GetMapping("/store-statistics")
    public Result<StoreStatisticsResponse> queryStoreStatistics(
            @RequestParam String storeId,
            @RequestParam String startDate,
            @RequestParam String endDate,
            @RequestParam(required = false) Integer transactionType,
            @RequestHeader(TokenConstants.AUTHORIZATION_HEADER) String token,
            @RequestHeader(TokenConstants.DEVICE_ID_HEADER) String deviceId) {
        
        log.info("接收店铺交易统计请求：storeId={}, startDate={}, endDate={}", 
                storeId, startDate, endDate);
        
        StoreStatisticsRequest request = StoreStatisticsRequest.builder()
                .storeId(storeId)
                .startDate(startDate)
                .endDate(endDate)
                .transactionType(transactionType)
                .build();
        
        // 去除Bearer前缀
        String pureToken = token.replace("Bearer ", "").trim();
        
        StoreStatisticsResponse response = transactionService.queryStoreStatistics(request, pureToken);
        return Result.success("查询成功", response);
    }

    /**
     * 接口7：商家交易统计
     * 
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @param transactionType 交易类型（可选）
     * @param token Bearer令牌（普通令牌，商家）
     * @param deviceId 设备ID
     * @return 商家交易统计响应
     */
    @GetMapping("/merchant-statistics")
    public Result<MerchantStatisticsResponse> queryMerchantStatistics(
            @RequestParam String startDate,
            @RequestParam String endDate,
            @RequestParam(required = false) Integer transactionType,
            @RequestHeader(TokenConstants.AUTHORIZATION_HEADER) String token,
            @RequestHeader(TokenConstants.DEVICE_ID_HEADER) String deviceId) {
        
        log.info("接收商家交易统计请求：startDate={}, endDate={}", startDate, endDate);
        
        MerchantStatisticsRequest request = MerchantStatisticsRequest.builder()
                .startDate(startDate)
                .endDate(endDate)
                .transactionType(transactionType)
                .build();
        
        // 去除Bearer前缀
        String pureToken = token.replace("Bearer ", "").trim();
        
        MerchantStatisticsResponse response = transactionService.queryMerchantStatistics(request, pureToken);
        return Result.success("查询成功", response);
    }

    /**
     * 接口8：流水数据统计
     * 
     * @param request 统计请求
     * @param token Bearer令牌（工作令牌）
     * @param deviceId 设备ID
     * @return 流水数据统计响应
     */
    @PostMapping("/trans-statistics")
    public Result<TransStatisticsResponse> queryTransStatistics(
            @Valid @RequestBody TransStatisticsRequest request,
            @RequestHeader(TokenConstants.AUTHORIZATION_HEADER) String token,
            @RequestHeader(TokenConstants.DEVICE_ID_HEADER) String deviceId) {
        
        log.info("接收流水数据统计请求：storeId={}, merchantId={}, dateRange={}", 
                request.getStoreId(), request.getMerchantId(), request.getDateRange());
        
        // 去除Bearer前缀
        String pureToken = token.replace("Bearer ", "").trim();
        
        TransStatisticsResponse response = transactionService.queryTransStatistics(request, pureToken);
        return Result.success("查询成功", response);
    }
}

