package com.ecards.member_management.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 工作令牌响应DTO
 * 
 * 修改说明（2025-10-30）：
 * - 新增 role、permissions、tokenVersion 字段
 * - 这些字段从数据库 t_work_relation 读取
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkTokenResponse {

    /**
     * 工作令牌（Bearer格式）
     */
    private String token;

    /**
     * 令牌过期时间（格式：yyyy-MM-dd HH:mm:ss）
     */
    private String expireTime;

    /**
     * 令牌唯一标识（JTI）
     */
    private String jti;

    /**
     * 绑定的工作店铺ID
     */
    private String storeId;

    /**
     * 店铺所属商家ID
     */
    private String merchantId;

    /**
     * 员工角色（manager-店长，employee-店员）
     */
    private String role;

    /**
     * 员工权限列表
     * 示例：["staff_add", "member_card_create", "transaction_recharge"]
     */
    private List<String> permissions;

    /**
     * 工作令牌版本号
     * 角色/权限修改时递增，确保旧令牌失效
     */
    private Integer tokenVersion;
}

