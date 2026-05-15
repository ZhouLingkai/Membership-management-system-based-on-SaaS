package com.ecards.member_management.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 员工详情响应DTO
 * 
 * 说明：
 * - 商家/店长查询时，返回完整信息（包含权限）
 * - 员工自查时，不返回权限信息
 * 
 * @author Ecards Team
 * @since 2025-10-30
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StaffDetailResponse {

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
     * 员工权限配置（商家/店长查询时返回，员工自查时为null）
     * 示例：{"employee": ["member_card_create"]}
     */
    private String staffPermission;

    /**
     * 关联店铺ID
     */
    private String storeId;

    /**
     * 店铺名称（员工自查时返回）
     */
    private String storeName;

    /**
     * 备注
     */
    private String remark;

    /**
     * 入职时间（yyyy-MM-dd HH:mm:ss）
     */
    private String entryTime;

    /**
     * 最后登录时间（yyyy-MM-dd HH:mm:ss）
     */
    private String lastLoginTime;
}

