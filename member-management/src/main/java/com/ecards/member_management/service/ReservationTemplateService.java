package com.ecards.member_management.service;

import com.ecards.member_management.dto.reservation.*;

/**
 * 预约模板Service接口
 */
public interface ReservationTemplateService {

    /**
     * 接口1：查询高级预约模板
     */
    TemplateQueryResponse queryTemplate(String storeId, String token);

    /**
     * 接口2：创建高级预约模板
     */
    TemplateCreateResponse createTemplate(TemplateCreateRequest request, String token);

    /**
     * 接口3：修改高级预约模板
     */
    TemplateUpdateResponse updateTemplate(Long templateId, TemplateUpdateRequest request, String token);

    /**
     * 接口18：查询不一致预约记录
     */
    InconsistentReservationsResponse queryInconsistentReservations(InconsistentReservationsRequest request, String token);
}
