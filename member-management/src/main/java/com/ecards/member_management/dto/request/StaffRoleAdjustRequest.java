package com.ecards.member_management.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 员工角色调整请求DTO
 * 
 * 业务说明：
 * - 仅商家可操作，需要管理令牌（5分钟有效，最多用5次）
 * - 调整员工角色：普通员工 ↔ 店长
 * - 角色调整后，token_version++，旧工作令牌失效
 * 
 * @author Ecards Team
 * @since 2025-10-31
 */
@Data
public class StaffRoleAdjustRequest {

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
     * 目标角色
     * STAFF - 普通员工
     * STORE_MANAGER - 店长
     */
    @NotBlank(message = "目标角色不能为空")
    private String targetRole;
}

