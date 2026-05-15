package com.ecards.member_management.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 员工解绑请求DTO
 * 
 * 业务说明：
 * - 仅商家可操作，需要管理令牌（5分钟有效，最多用5次）
 * - 解除员工与店铺的绑定关系，永久删除工作关系记录
 * - 如果是员工最后一个工作关系，user_type改为1，普通令牌失效
 * 
 * @author Ecards Team
 * @since 2025-10-31
 */
@Data
public class StaffUnbindRequest {

    /**
     * 商家ID（与令牌一致，用于校验）
     */
    @NotBlank(message = "商家ID不能为空")
    private String merchantId;

    /**
     * 员工关联店铺ID
     */
    @NotBlank(message = "店铺ID不能为空")
    private String storeId;

    /**
     * 确认解绑（必须为true）
     */
    @NotNull(message = "请确认解绑操作")
    private Boolean confirm;
}

