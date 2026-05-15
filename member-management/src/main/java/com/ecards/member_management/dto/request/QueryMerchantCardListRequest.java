package com.ecards.member_management.dto.request;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 查询商家会员卡列表请求DTO
 * 
 * @author Ecards Team
 * @since 2025-11-03
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QueryMerchantCardListRequest {

    /**
     * 店铺ID筛选（可选）
     */
    private String storeId;

    /**
     * 卡种ID筛选（可选）
     */
    private Long cardTypeId;

    /**
     * 卡种类型筛选（可选）
     * 1-余额卡，2-次数卡，3-时效卡，4-积分卡
     */
    @Min(value = 1, message = "卡种类型必须为1-4")
    @Max(value = 4, message = "卡种类型必须为1-4")
    private Integer cardTtype;

    /**
     * 状态筛选（可选）
     * 0-未激活，1-正常，2-已过期，3-已冻结，4-已注销
     */
    @Min(value = 0, message = "状态必须为0-4")
    @Max(value = 4, message = "状态必须为0-4")
    private Integer status;

    /**
     * 手机号模糊筛选（可选）
     */
    private String memberPhone;

    /**
     * 会员姓名模糊筛选（可选）
     */
    private String memberName;

    /**
     * 开卡开始时间（ISO8601格式，可选）
     */
    private String startTime;

    /**
     * 开卡结束时间（ISO8601格式，可选）
     */
    private String endTime;

    /**
     * 页码（从1开始）
     */
    @NotNull(message = "页码不能为空")
    @Min(value = 1, message = "页码必须从1开始")
    private Integer pageNum;

    /**
     * 每页条数（1-50）
     */
    @NotNull(message = "每页条数不能为空")
    @Min(value = 1, message = "每页条数必须为1-50")
    @Max(value = 50, message = "每页条数必须为1-50")
    private Integer pageSize;
}

