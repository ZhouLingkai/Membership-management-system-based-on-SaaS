package com.ecards.member_management.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 线下扫码查询个人会员卡请求DTO（接口15）
 * 
 * @author Ecards Team
 * @since 2025-11-04
 */
@Data
public class QueryByScanRequest {

    /**
     * 店铺ID（UUID格式）
     */
    @NotBlank(message = "店铺ID不能为空")
    private String storeId;

    /**
     * 用户特权令牌（从二维码中获取，5分钟有效，一次性）
     */
    @NotBlank(message = "特权令牌不能为空")
    private String privilegeToken;

    /**
     * 会员卡ID（UUID格式，可选）
     * 有值：返回该卡详情
     * 无值：返回该用户在该店铺可用的会员卡列表
     */
    private String memberCardId;
}

