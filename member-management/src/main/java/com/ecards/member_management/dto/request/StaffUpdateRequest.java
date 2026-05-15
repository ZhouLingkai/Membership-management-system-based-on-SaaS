package com.ecards.member_management.dto.request;

import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 员工信息修改请求DTO
 * 
 * 业务说明：
 * - 商家/店长修改员工非权限信息（姓名、备注）
 * - 商家使用普通令牌时需传 storeId，使用工作令牌时自动从令牌提取
 * - 店长必须使用工作令牌，自动从令牌提取 storeId
 * - 店长只能修改店员，不能修改其他店长
 * 
 * @author Ecards Team
 * @since 2025-10-31
 */
@Data
public class StaffUpdateRequest {

    /**
     * 店铺ID（商家使用普通令牌时必填）
     */
    private String storeId;

    /**
     * 新员工姓名（可选，不填则不修改）
     */
    @Size(max = 20, message = "员工姓名最多20位")
    private String staffName;

    /**
     * 新备注（可选，不填则不修改）
     */
    @Size(max = 100, message = "备注最多100位")
    private String remark;
}

