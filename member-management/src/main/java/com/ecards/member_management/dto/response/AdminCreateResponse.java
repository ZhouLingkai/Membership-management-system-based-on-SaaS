package com.ecards.member_management.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 创建管理员响应DTO
 * 
 * @author Ecards Team
 * @since 2025-10-28
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminCreateResponse {

    /**
     * 管理员ID（UUID字符串）
     */
    private String adminId;

    /**
     * 登录账号
     */
    private String account;

    /**
     * 手机号（脱敏）
     */
    private String phone;

    /**
     * 管理员角色
     * 1 - 超级管理员
     * 2 - 审核员
     */
    private Integer adminRole;

    /**
     * 角色代码
     * SUPER_ADMIN / AUDITOR
     */
    private String roleCode;

    /**
     * 创建时间
     */
    private String createTime;

    /**
     * 创建者ID（UUID字符串）
     */
    private String creatorId;

    /**
     * 备注
     */
    private String remark;
}


