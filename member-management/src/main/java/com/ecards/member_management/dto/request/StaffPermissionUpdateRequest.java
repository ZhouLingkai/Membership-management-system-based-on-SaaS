package com.ecards.member_management.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 员工权限修改请求DTO
 * 
 * 业务说明：
 * - 商家可修改任意员工权限（可使用普通令牌或工作令牌）
 * - 店长只能修改店员权限（必须使用工作令牌）
 * - 权限修改后，token_version++，旧工作令牌失效
 * 
 * @author Ecards Team
 * @since 2025-10-31
 */
@Data
public class StaffPermissionUpdateRequest {

    /**
     * 店铺ID（商家使用普通令牌时必填）
     */
    private String storeId;

    /**
     * 新权限配置（JSON字符串）
     * 店长示例：{"manager": ["staff_add"]}
     * 店员示例：{"employee": ["member_card_create", "transaction_recharge"]}
     * 空权限：{"employee": []} 或 {"manager": []}
     */
    @NotBlank(message = "权限配置不能为空")
    private String newPermission;
}

