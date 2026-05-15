package com.ecards.member_management.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 员工列表项DTO
 * 
 * @author Ecards Team
 * @since 2025-10-30
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StaffListItem {

    /**
     * 员工ID（用户ID）
     */
    private String staffId;

    /**
     * 员工姓名
     */
    private String staffName;

    /**
     * 员工手机号（明文）
     */
    private String staffPhone;

    /**
     * 员工角色
     * STAFF - 店员
     * STORE_MANAGER - 店长
     */
    private String staffRole;

    /**
     * 入职时间（yyyy-MM-dd HH:mm:ss）
     */
    private String entryTime;
}

