package com.ecards.member_management.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 工作关系列表项DTO
 * 
 * @author Ecards Team
 * @since 2025-10-30
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkRelationItem {

    /**
     * 工作关系ID
     */
    private Long relationId;

    /**
     * 店铺ID
     */
    private String storeId;

    /**
     * 店铺名称
     */
    private String storeName;

    /**
     * 所属商家ID
     */
    private String merchantId;

    /**
     * 员工角色
     * STAFF - 店员
     * STORE_MANAGER - 店长
     */
    private String role;

    /**
     * 入职时间（yyyy-MM-dd HH:mm:ss）
     */
    private String entryTime;

    /**
     * 在职状态（1-在职，0-离职）
     */
    private Integer status;
}

