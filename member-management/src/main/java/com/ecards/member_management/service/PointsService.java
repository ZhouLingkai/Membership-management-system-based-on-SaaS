package com.ecards.member_management.service;

import com.ecards.member_management.dto.request.PointsAdjustRequest;
import com.ecards.member_management.dto.request.PointsRecordsQueryRequest;
import com.ecards.member_management.dto.response.PointsAdjustResponse;
import com.ecards.member_management.dto.response.PointsRecordListResponse;

/**
 * 积分管理服务接口
 * 提供积分变动和积分记录查询功能
 * 
 * @author Ecards Team
 * @since 2025-11-04
 */
public interface PointsService {

    /**
     * 接口1：积分变动
     * 为指定会员卡增加或扣减积分
     *
     * @param request 积分变动请求
     * @param token   令牌（工作令牌或普通令牌）
     * @return 积分变动响应
     */
    PointsAdjustResponse adjustPoints(PointsAdjustRequest request, String token);

    /**
     * 接口2：积分记录查询
     * 查询指定会员卡的积分变动记录
     *
     * @param request 查询请求
     * @param token   令牌（工作令牌或普通令牌）
     * @return 积分记录列表响应
     */
    PointsRecordListResponse queryPointsRecords(PointsRecordsQueryRequest request, String token);
}

