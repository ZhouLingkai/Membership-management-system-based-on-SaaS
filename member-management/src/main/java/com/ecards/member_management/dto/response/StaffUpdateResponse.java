package com.ecards.member_management.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 员工信息修改响应DTO
 * 
 * @author Ecards Team
 * @since 2025-10-31
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StaffUpdateResponse {

    /**
     * 修改时间
     */
    private String updateTime;
}

