package com.ecards.member_management.dto.reservation;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 接口1：查询高级预约模板 - 响应
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TemplateQueryResponse {
    private Long reserveId;
    private String storeId;
    private List<String> reservationTimeList;
    private List<String> cancelRule;
    private Integer advanceDays;
    private List<String> forbiddenDays;
    private Integer customizeForbidden;
    private String effectiveStartTime;
    private String effectiveEndTime;
    private String createTime;
    private String updateTime;
}
