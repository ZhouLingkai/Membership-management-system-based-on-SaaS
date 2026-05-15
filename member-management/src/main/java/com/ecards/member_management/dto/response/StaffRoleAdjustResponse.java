package com.ecards.member_management.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 员工角色调整响应DTO
 * 
 * @author Ecards Team
 * @since 2025-10-31
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StaffRoleAdjustResponse {

    /**
     * 员工ID
     */
    private String staffId;

    /**
     * 调整前角色
     */
    private String oldRole;

    /**
     * 调整后角色
     */
    private String newRole;

    /**
     * 调整时间
     */
    private String adjustTime;
}

