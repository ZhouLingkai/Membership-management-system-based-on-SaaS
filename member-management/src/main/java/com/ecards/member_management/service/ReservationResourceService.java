package com.ecards.member_management.service;

import com.ecards.member_management.dto.reservation.*;

/**
 * 预约资源Service接口
 */
public interface ReservationResourceService {

    /**
     * 接口4：查询资源列表
     */
    ResourceListResponse queryResourceList(String storeId, String keyword, 
                                          Integer isReservable, Integer supportCardTypes,
                                          Integer page, Integer pageSize, String token);

    /**
     * 接口5：查询资源详细信息
     */
    ResourceDetailResponse queryResourceDetail(Long resourceId, String token);

    /**
     * 接口6：创建资源
     */
    ResourceCreateResponse createResource(ResourceCreateRequest request, String token);

    /**
     * 接口7：修改资源
     */
    ResourceUpdateResponse updateResource(Long resourceId, ResourceUpdateRequest request, String token);

    /**
     * 接口8：删除资源
     */
    ResourceDeleteResponse deleteResource(Long resourceId, String token);

    /**
     * 接口9：启停用资源
     */
    ResourceToggleResponse toggleResource(Long resourceId, ResourceToggleRequest request, String token);

    /**
     * 接口10：查询某日预约情况
     */
    ReservationQueryResponse queryReservations(ReservationQueryRequest request, String token);

    /**
     * 接口11：获取预约列表
     */
    MyReservationsResponse getMyReservations(MyReservationsRequest request, String token);

    /**
     * 接口12：预约资源
     */
    ReserveResourceResponse reserveResource(ReserveResourceRequest request, String token);

    /**
     * 接口13：取消预约资源
     */
    CancelReservationResponse cancelReservation(CancelReservationRequest request, String token);

    /**
     * 接口14：查询某会员预约情况
     */
    MemberReservationsResponse queryMemberReservations(MemberReservationsRequest request, String token);

    /**
     * 接口15：停用预约资源
     */
    DisableResourceResponse disableResource(DisableResourceRequest request, String token);

    /**
     * 接口16：线下占用预约资源
     */
    OccupyResourceResponse occupyResource(OccupyResourceRequest request, String token);

    /**
     * 接口17：员工取消预约
     */
    StaffCancelResponse staffCancelReservation(StaffCancelRequest request, String token);

    /**
     * 接口19：创建/修改优惠策略
     */
    PromotionStrategyResponse createOrUpdatePromotionStrategy(PromotionStrategyRequest request, String token);

    /**
     * 接口20：设置优惠策略不生效日期
     */
    NonEffectiveDatesResponse setNonEffectiveDates(NonEffectiveDatesRequest request, String token);

    /**
     * 接口21：清空指定资源优惠策略
     */
    ClearPromotionStrategyResponse clearPromotionStrategy(ClearPromotionStrategyRequest request, String token);
}
