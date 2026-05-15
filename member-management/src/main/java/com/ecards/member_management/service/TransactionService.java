package com.ecards.member_management.service;

import com.ecards.member_management.dto.request.*;
import com.ecards.member_management.dto.response.*;

/**
 * 交易管理服务接口
 * 
 * @author Ecards Team
 * @since 2025-11-05
 */
public interface TransactionService {

    /**
     * 接口1：会员卡充值
     * 为会员卡充值余额或次数，支持余额卡和次数卡
     * 
     * @param request 充值请求
     * @param token 工作令牌或普通令牌（商家）
     * @return 充值响应
     */
    RechargeResponse recharge(RechargeRequest request, String token);

    /**
     * 接口2：时效调整
     * 为时效卡调整到期时间，支持相对延期和绝对设置
     * 
     * @param request 时效调整请求
     * @param token 工作令牌或普通令牌（商家）
     * @return 时效调整响应
     */
    ExpireAdjustResponse expireAdjust(ExpireAdjustRequest request, String token);

    /**
     * 接口3：会员卡消费
     * 核销会员卡余额或次数，支持余额卡和次数卡
     * 
     * @param request 消费请求
     * @param token 工作令牌或普通令牌（商家）
     * @return 消费响应
     */
    ConsumeResponse consume(ConsumeRequest request, String token);

    /**
     * 接口4：单卡交易记录查询
     * 查询指定会员卡的交易历史记录，支持分页和筛选
     * 
     * @param request 查询请求
     * @param token 工作令牌或普通令牌
     * @return 交易记录查询响应
     */
    CardRecordsQueryResponse queryCardRecords(CardRecordsQueryRequest request, String token);

    /**
     * 接口5：个人交易记录查询
     * 用户查询自己的所有交易记录（充值/消费）
     * 
     * @param request 查询请求
     * @param token 普通令牌
     * @return 个人交易记录查询响应
     */
    MyRecordsQueryResponse queryMyRecords(MyRecordsQueryRequest request, String token);

    /**
     * 接口6：店铺交易统计
     * 统计指定店铺的交易数据（仅统计余额卡）
     * 
     * @param request 统计请求
     * @param token 工作令牌或普通令牌（商家）
     * @return 店铺交易统计响应
     */
    StoreStatisticsResponse queryStoreStatistics(StoreStatisticsRequest request, String token);

    /**
     * 接口7：商家交易统计
     * 统计商家名下所有店铺的交易数据（仅统计余额卡）
     * 
     * @param request 统计请求
     * @param token 普通令牌（商家）
     * @return 商家交易统计响应
     */
    MerchantStatisticsResponse queryMerchantStatistics(MerchantStatisticsRequest request, String token);

    /**
     * 接口8：流水数据统计
     * 查询店铺/商家的流水数据，支持按日期范围统计
     * 
     * @param request 统计请求
     * @param token 工作令牌
     * @return 流水数据统计响应
     */
    TransStatisticsResponse queryTransStatistics(TransStatisticsRequest request, String token);
}

