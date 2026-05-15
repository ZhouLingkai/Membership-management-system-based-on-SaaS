package com.ecards.member_management.controller;

import com.ecards.member_management.annotation.Idempotent;
import com.ecards.member_management.common.Result;
import com.ecards.member_management.constants.TokenConstants;
import com.ecards.member_management.dto.request.PointsAdjustRequest;
import com.ecards.member_management.dto.request.PointsRecordsQueryRequest;
import com.ecards.member_management.dto.response.PointsAdjustResponse;
import com.ecards.member_management.dto.response.PointsRecordListResponse;
import com.ecards.member_management.service.PointsService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * 积分管理控制器
 * 提供积分变动和积分记录查询接口
 * 
 * @author Ecards Team
 * @since 2025-11-04
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/points")
@RequiredArgsConstructor
public class PointsController {

    private final PointsService pointsService;

    /**
     * 接口1：积分变动
     * POST /api/v1/points/adjust
     * 
     * 权限：商家/店长/店员（工作令牌），兼容商家普通令牌
     * 幂等性：需要前端携带X-Request-ID请求头
     */
    @PostMapping("/adjust")
    @Idempotent(timeout = 600, value = "积分变动")
    public Result<PointsAdjustResponse> adjustPoints(
            @Valid @RequestBody PointsAdjustRequest request,
            @RequestHeader("Authorization") String authorization,
            @RequestHeader(TokenConstants.DEVICE_ID_HEADER) String deviceId) {
        
        log.info("积分变动请求: memberCardId={}, storeId={}, pointsChange={}", 
                request.getMemberCardId(), request.getStoreId(), request.getPointsChange());
        
        // 提取token（去除"Bearer "前缀）
        String token = authorization.replace("Bearer ", "");
        
        PointsAdjustResponse response = pointsService.adjustPoints(request, token);
        
        log.info("积分变动成功: memberCardId={}, pointsSnapshot={}", 
                response.getMemberCardId(), response.getPointsSnapshot());
        
        return Result.success("积分变动成功", response);
    }

    /**
     * 接口2：积分记录查询
     * GET /api/v1/points/records
     * 
     * 权限：商家/店长/店员（工作令牌）、用户本人（普通令牌）
     */
    @GetMapping("/records")
    public Result<PointsRecordListResponse> queryPointsRecords(
            @Valid @ModelAttribute PointsRecordsQueryRequest request,
            @RequestHeader("Authorization") String authorization,
            @RequestHeader(TokenConstants.DEVICE_ID_HEADER) String deviceId) {
        
        log.info("积分记录查询请求: memberCardId={}, memberPhone={}, pageNum={}, pageSize={}", 
                request.getMemberCardId(), request.getMemberPhone() != null ? "***" : null,
                request.getPageNum(), request.getPageSize());
        
        // 提取token（去除"Bearer "前缀）
        String token = authorization.replace("Bearer ", "");
        
        PointsRecordListResponse response = pointsService.queryPointsRecords(request, token);
        
        log.info("积分记录查询成功: 当前积分={}, 总记录数={}", 
                response.getCurrentPoints(), response.getTotal());
        
        return Result.success("查询成功", response);
    }
}

