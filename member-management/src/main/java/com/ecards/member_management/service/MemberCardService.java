package com.ecards.member_management.service;

import com.ecards.member_management.dto.request.*;
import com.ecards.member_management.dto.response.*;

/**
 * 会员卡服务接口
 * 提供会员卡办理、查询、状态变更等功能
 * 
 * @author Ecards Team
 * @since 2025-11-03
 */
public interface MemberCardService {

    /**
     * 接口1：会员卡办理（手机号快速办理）
     *
     * @param request 办卡请求
     * @param token   令牌（工作令牌或普通令牌）
     * @return 办卡响应
     */
    CreateMemberCardResponse createByPhone(CreateMemberCardByPhoneRequest request, String token);

    /**
     * 接口2：会员卡办理（线下扫码办理）
     *
     * @param request 办卡请求
     * @param token   令牌（工作令牌或普通令牌）
     * @return 办卡响应
     */
    CreateMemberCardResponse createByScan(CreateMemberCardByScanRequest request, String token);

    /**
     * 接口3：查询商家会员卡列表（分页）
     *
     * @param request 查询请求
     * @param token   令牌（普通令牌，商家）
     * @return 会员卡列表响应
     */
    MemberCardListResponse queryMerchantCardList(QueryMerchantCardListRequest request, String token);

    /**
     * 接口4：查询店铺会员卡列表（分页，支持本店卡/跨店卡切换）
     *
     * @param request 查询请求
     * @param token   令牌（工作令牌或普通令牌）
     * @return 会员卡列表响应
     */
    MemberCardListResponse queryStoreCardList(QueryStoreCardListRequest request, String token);

    /**
     * 接口5：查询商家会员统计数据
     * 注意：与交易管理模块的Response命名冲突，临时注释
     * TODO: 需要创建专门的会员统计Response类后重新启用
     *
     * @param merchantId 商家ID
     * @return 商家统计响应
     */
    // MerchantStatisticsResponse getMerchantStatistics(String merchantId);

    /**
     * 接口6：查询店铺会员统计数据
     * 注意：与交易管理模块的Response命名冲突，临时注释
     * TODO: 需要创建专门的会员统计Response类后重新启用
     *
     * @param storeId    店铺ID
     * @param merchantId 商家ID
     * @return 店铺统计响应
     */
    // StoreStatisticsResponse getStoreStatistics(String storeId, String merchantId);

    /**
     * 接口7：用户查询自己的会员卡列表
     *
     * @param userId  用户ID
     * @param request 查询请求
     * @return 会员卡列表响应
     */
    MemberCardListResponse getMyCardList(String userId, QueryMyCardListRequest request);

    /**
     * 接口8：通过手机号查询会员卡
     *
     * @param storeId        店铺ID
     * @param encryptedPhone 加密手机号
     * @param merchantId     商家ID
     * @return 查询结果（包含本店卡和跨店卡）
     */
    QueryByPhoneResponse queryByPhone(String storeId, String encryptedPhone, String merchantId);

    /**
     * 接口9：会员卡详情查询
     *
     * @param memberCardId 会员卡ID
     * @return 会员卡详情响应
     */
    MemberCardDetailResponse getCardDetail(String memberCardId);

    /**
     * 接口11：批量激活会员卡（手机号批量激活）
     *
     * @param userId 用户ID
     * @param phone  用户手机号
     * @return 批量激活响应
     */
    ActivateBatchResponse activateBatch(String userId, String phone);

    /**
     * 接口12：冻结会员卡
     *
     * @param request 冻结请求
     * @param token   令牌（工作令牌或普通令牌）
     * @return 冻结响应
     */
    FreezeCardResponse freezeCard(FreezeCardRequest request, String token);

    /**
     * 接口13：解冻会员卡
     *
     * @param request 解冻请求
     * @param token   令牌（工作令牌或普通令牌）
     * @return 解冻响应
     */
    UnfreezeCardResponse unfreezeCard(UnfreezeCardRequest request, String token);

    /**
     * 接口15：线下扫码查询个人会员卡（场景A：会员卡详情查询）
     *
     * @param storeId        店铺ID
     * @param privilegeToken 特权令牌
     * @param memberCardId   会员卡ID
     * @param merchantId     商家ID
     * @return 会员卡详情响应
     */
    MemberCardDetailResponse queryByScanDetail(String storeId, String privilegeToken, String memberCardId, String merchantId);

    /**
     * 接口15：线下扫码查询个人会员卡（场景B：会员卡列表查询）
     *
     * @param storeId        店铺ID
     * @param privilegeToken 特权令牌
     * @param merchantId     商家ID
     * @return 查询结果（包含本店卡和跨店卡）
     */
    QueryByPhoneResponse queryByScanList(String storeId, String privilegeToken, String merchantId);
}

