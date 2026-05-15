package com.ecards.member_management.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 员工解绑响应DTO
 * 
 * @author Ecards Team
 * @since 2025-10-31
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StaffUnbindResponse {

    /**
     * 已解绑员工ID
     */
    private String staffId;

    /**
     * 解绑时间
     */
    private String unbindTime;
}

