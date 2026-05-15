package com.ecards.member_management.controller;

import com.ecards.member_management.common.Result;
import com.ecards.member_management.dto.reservation.*;
import com.ecards.member_management.service.ReservationResourceService;
import com.ecards.member_management.service.ReservationTemplateService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * 预约系统Controller（接口1-13）
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/advanced-reservation")
@RequiredArgsConstructor
public class ReservationController {

    private final ReservationTemplateService templateService;
    private final ReservationResourceService resourceService;

    /**
     * 接口1：查询高级预约模板
     */
    @GetMapping("/template")
    public Result<TemplateQueryResponse> queryTemplate(
            @RequestParam String storeId,
            @RequestHeader("Authorization") String authorization) {
        String token = authorization.substring(7); // 移除 "Bearer "
        TemplateQueryResponse response = templateService.queryTemplate(storeId, token);
        return Result.success(response);
    }

    /**
     * 接口2：创建高级预约模板
     */
    @PostMapping("/template")
    public Result<TemplateCreateResponse> createTemplate(
            @Valid @RequestBody TemplateCreateRequest request,
            @RequestHeader("Authorization") String authorization) {
        String token = authorization.substring(7);
        TemplateCreateResponse response = templateService.createTemplate(request, token);
        return Result.success(response);
    }

    /**
     * 接口3：修改高级预约模板
     */
    @PutMapping("/template/{templateId}")
    public Result<TemplateUpdateResponse> updateTemplate(
            @PathVariable Long templateId,
            @Valid @RequestBody TemplateUpdateRequest request,
            @RequestHeader("Authorization") String authorization) {
        String token = authorization.substring(7);
        TemplateUpdateResponse response = templateService.updateTemplate(templateId, request, token);
        return Result.success(response);
    }

    /**
     * 接口4：查询资源列表
     */
    @GetMapping("/resources")
    public Result<ResourceListResponse> queryResourceList(
            @RequestParam String storeId,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer isReservable,
            @RequestParam(required = false) Integer supportCardTypes,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer pageSize,
            @RequestHeader("Authorization") String authorization) {
        String token = authorization.substring(7);
        ResourceListResponse response = resourceService.queryResourceList(
                storeId, keyword, isReservable, supportCardTypes, page, pageSize, token);
        return Result.success(response);
    }

    /**
     * 接口5：查询资源详细信息
     */
    @GetMapping("/resources/{resourceId}")
    public Result<ResourceDetailResponse> queryResourceDetail(
            @PathVariable Long resourceId,
            @RequestHeader("Authorization") String authorization) {
        String token = authorization.substring(7);
        ResourceDetailResponse response = resourceService.queryResourceDetail(resourceId, token);
        return Result.success(response);
    }

    /**
     * 接口6：创建资源
     */
    @PostMapping("/resources")
    public Result<ResourceCreateResponse> createResource(
            @Valid @RequestBody ResourceCreateRequest request,
            @RequestHeader("Authorization") String authorization) {
        String token = authorization.substring(7);
        ResourceCreateResponse response = resourceService.createResource(request, token);
        return Result.success(response);
    }

    /**
     * 接口7：修改资源
     */
    @PutMapping("/resources/{resourceId}")
    public Result<ResourceUpdateResponse> updateResource(
            @PathVariable Long resourceId,
            @Valid @RequestBody ResourceUpdateRequest request,
            @RequestHeader("Authorization") String authorization) {
        String token = authorization.substring(7);
        ResourceUpdateResponse response = resourceService.updateResource(resourceId, request, token);
        return Result.success(response);
    }

    /**
     * 接口8：删除资源
     */
    @DeleteMapping("/resources/{resourceId}")
    public Result<ResourceDeleteResponse> deleteResource(
            @PathVariable Long resourceId,
            @RequestHeader("Authorization") String authorization) {
        String token = authorization.substring(7);
        ResourceDeleteResponse response = resourceService.deleteResource(resourceId, token);
        return Result.success(response);
    }

    /**
     * 接口9：启停用资源
     */
    @PutMapping("/resources/{resourceId}/toggle")
    public Result<ResourceToggleResponse> toggleResource(
            @PathVariable Long resourceId,
            @Valid @RequestBody ResourceToggleRequest request,
            @RequestHeader("Authorization") String authorization) {
        String token = authorization.substring(7);
        ResourceToggleResponse response = resourceService.toggleResource(resourceId, request, token);
        return Result.success(response);
    }

    /**
     * 接口10：查询某日预约情况
     */
    @GetMapping("/query")
    public Result<ReservationQueryResponse> queryReservations(
            @RequestParam String storeId,
            @RequestParam String requestDate,
            @RequestParam(required = false) String keyword,
            @RequestHeader("Authorization") String authorization) {
        String token = authorization.substring(7);
        ReservationQueryRequest request = new ReservationQueryRequest();
        request.setStoreId(storeId);
        request.setRequestDate(requestDate);
        request.setKeyword(keyword);
        ReservationQueryResponse response = resourceService.queryReservations(request, token);
        return Result.success(response);
    }

    /**
     * 接口11：获取预约列表
     */
    @GetMapping("/my-reservations")
    public Result<MyReservationsResponse> getMyReservations(
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer pageSize,
            @RequestHeader("Authorization") String authorization) {
        String token = authorization.substring(7);
        MyReservationsRequest request = new MyReservationsRequest();
        request.setStatus(status);
        request.setStartDate(startDate);
        request.setEndDate(endDate);
        request.setPage(page);
        request.setPageSize(pageSize);
        MyReservationsResponse response = resourceService.getMyReservations(request, token);
        return Result.success(response);
    }

    /**
     * 接口12：预约资源
     */
    @PostMapping("/reserve")
    public Result<ReserveResourceResponse> reserveResource(
            @Valid @RequestBody ReserveResourceRequest request,
            @RequestHeader("Authorization") String authorization) {
        String token = authorization.substring(7);
        ReserveResourceResponse response = resourceService.reserveResource(request, token);
        return Result.success(response);
    }

    /**
     * 接口13：取消预约资源
     */
    @PutMapping("/cancel")
    public Result<CancelReservationResponse> cancelReservation(
            @Valid @RequestBody CancelReservationRequest request,
            @RequestHeader("Authorization") String authorization) {
        String token = authorization.substring(7);
        CancelReservationResponse response = resourceService.cancelReservation(request, token);
        return Result.success(response);
    }

    /**
     * 接口14：查询某会员预约情况
     */
    @GetMapping("/member-reservations")
    public Result<MemberReservationsResponse> queryMemberReservations(
            @RequestParam String userPhone,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer pageSize,
            @RequestHeader("Authorization") String authorization) {
        String token = authorization.substring(7);
        MemberReservationsRequest request = new MemberReservationsRequest();
        request.setUserPhone(userPhone);
        request.setStatus(status);
        request.setStartDate(startDate);
        request.setEndDate(endDate);
        request.setPage(page != null ? page : 1);
        request.setPageSize(pageSize != null ? pageSize : 20);
        MemberReservationsResponse response = resourceService.queryMemberReservations(request, token);
        return Result.success(response);
    }

    /**
     * 接口15：停用预约资源
     */
    @PostMapping("/disable")
    public Result<DisableResourceResponse> disableResource(
            @Valid @RequestBody DisableResourceRequest request,
            @RequestHeader("Authorization") String authorization) {
        String token = authorization.substring(7);
        DisableResourceResponse response = resourceService.disableResource(request, token);
        return Result.success(response);
    }

    /**
     * 接口16：线下占用预约资源
     */
    @PostMapping("/occupy")
    public Result<OccupyResourceResponse> occupyResource(
            @Valid @RequestBody OccupyResourceRequest request,
            @RequestHeader("Authorization") String authorization) {
        String token = authorization.substring(7);
        OccupyResourceResponse response = resourceService.occupyResource(request, token);
        return Result.success(response);
    }

    /**
     * 接口17：员工取消预约
     */
    @PutMapping("/staff-cancel")
    public Result<StaffCancelResponse> staffCancelReservation(
            @Valid @RequestBody StaffCancelRequest request,
            @RequestHeader("Authorization") String authorization) {
        String token = authorization.substring(7);
        StaffCancelResponse response = resourceService.staffCancelReservation(request, token);
        return Result.success(response);
    }

    /**
     * 接口18：查询不一致预约记录
     */
    @GetMapping("/inconsistent-reservations")
    public Result<InconsistentReservationsResponse> queryInconsistentReservations(
            @RequestParam String storeId,
            @RequestParam String startDate,
            @RequestParam String endDate,
            @RequestHeader("Authorization") String authorization) {
        String token = authorization.substring(7);
        InconsistentReservationsRequest request = new InconsistentReservationsRequest(storeId, startDate, endDate);
        InconsistentReservationsResponse response = templateService.queryInconsistentReservations(request, token);
        return Result.success(response);
    }

    /**
     * 接口19：创建/修改优惠策略
     */
    @PostMapping("/promotion-strategy")
    public Result<PromotionStrategyResponse> createOrUpdatePromotionStrategy(
            @Valid @RequestBody PromotionStrategyRequest request,
            @RequestHeader("Authorization") String authorization) {
        String token = authorization.substring(7);
        PromotionStrategyResponse response = resourceService.createOrUpdatePromotionStrategy(request, token);
        return Result.success(response);
    }

    /**
     * 接口20：设置优惠策略不生效日期
     */
    @PutMapping("/promotion-strategy/non-effective-dates")
    public Result<NonEffectiveDatesResponse> setNonEffectiveDates(
            @Valid @RequestBody NonEffectiveDatesRequest request,
            @RequestHeader("Authorization") String authorization) {
        String token = authorization.substring(7);
        NonEffectiveDatesResponse response = resourceService.setNonEffectiveDates(request, token);
        return Result.success(response);
    }

    /**
     * 接口21：清空指定资源优惠策略
     */
    @DeleteMapping("/promotion-strategy/clear")
    public Result<ClearPromotionStrategyResponse> clearPromotionStrategy(
            @Valid @RequestBody ClearPromotionStrategyRequest request,
            @RequestHeader("Authorization") String authorization) {
        String token = authorization.substring(7);
        ClearPromotionStrategyResponse response = resourceService.clearPromotionStrategy(request, token);
        return Result.success(response);
    }
}
