package com.ecards.member_management.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;

/**
 * 积分记录查询请求（查询参数）
 */
@Data
public class PointsRecordsQueryRequest {

    /**
     * 会员卡ID（与memberPhone二选一）
     */
    @Pattern(regexp = "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$",
             message = "会员卡ID格式错误")
    private String memberCardId;

    /**
     * 会员手机号（AES256CBC加密，与memberCardId二选一）
     */
    private String memberPhone;

    /**
     * 店铺ID（工作令牌必填）
     */
    @Pattern(regexp = "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$",
             message = "店铺ID格式错误")
    private String storeId;

    /**
     * 开始日期（yyyy-MM-dd）
     */
    @Pattern(regexp = "^\\d{4}-\\d{2}-\\d{2}$", message = "开始日期格式错误，应为yyyy-MM-dd")
    private String startDate;

    /**
     * 结束日期（yyyy-MM-dd）
     */
    @Pattern(regexp = "^\\d{4}-\\d{2}-\\d{2}$", message = "结束日期格式错误，应为yyyy-MM-dd")
    private String endDate;

    /**
     * 页码（从1开始）
     */
    @NotNull(message = "页码不能为空")
    @Min(value = 1, message = "页码必须大于0")
    private Integer pageNum;

    /**
     * 每页条数（1-100）
     */
    @NotNull(message = "每页条数不能为空")
    @Min(value = 1, message = "每页条数必须大于0")
    @Max(value = 100, message = "每页条数不能超过100")
    private Integer pageSize;
}

