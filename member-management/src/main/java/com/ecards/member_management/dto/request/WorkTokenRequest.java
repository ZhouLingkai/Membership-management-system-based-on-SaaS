package com.ecards.member_management.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 获取工作令牌请求DTO
 * 
 * 修改说明（2025-10-30）：
 * - 去掉 permissions 字段，权限自动从数据库 t_work_relation 读取
 * - 只需提供 storeId，系统自动查询该用户在该店铺的角色和权限
 */
@Data
public class WorkTokenRequest {

    /**
     * 工作店铺ID（UUID字符串）
     */
    @NotBlank(message = "店铺ID不能为空")
    private String storeId;
}

