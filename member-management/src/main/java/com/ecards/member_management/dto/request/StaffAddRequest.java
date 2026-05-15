package com.ecards.member_management.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 员工添加请求DTO
 * 
 * 业务说明：
 * - 商家/店长通过手机号和邀请码添加员工
 * - 商家使用普通令牌时需传 storeId，使用工作令牌时自动从令牌提取
 * - 店长必须使用工作令牌，自动从令牌提取 storeId
 * 
 * @author Ecards Team
 * @since 2025-10-30
 */
@Data
public class StaffAddRequest {

    /**
     * 店铺ID（商家使用普通令牌时必填）
     */
    private String storeId;

    /**
     * 待添加员工的手机号（明文，11位）
     */
    @NotBlank(message = "员工手机号不能为空")
    @Size(min = 11, max = 11, message = "手机号必须为11位")
    private String staffPhone;

    /**
     * 待添加员工的邀请码
     */
    @NotBlank(message = "员工邀请码不能为空")
    private String staffInviteCode;

    /**
     * 员工角色
     * STAFF - 普通员工
     * STORE_MANAGER - 店长
     */
    @NotBlank(message = "员工角色不能为空")
    private String staffRole;

    /**
     * 员工权限配置（JSON字符串，可选）
     * 示例：{"manager": ["staff_add"]} 或 {"employee": ["member_card_create", "transaction_recharge"]}
     * 空权限：{"employee": []} 或 {"manager": []}
     */
    private String staffPermission;

    /**
     * 员工姓名（可选）
     */
    @Size(max = 20, message = "员工姓名最多20位")
    private String staffName;

    /**
     * 备注（可选）
     */
    @Size(max = 100, message = "备注最多100位")
    private String remark;
}

