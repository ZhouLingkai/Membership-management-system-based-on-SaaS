package com.ecards.member_management.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 员工添加响应DTO
 * 
 * @author Ecards Team
 * @since 2025-10-30
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StaffAddResponse {

    /**
     * 员工ID（用户ID）
     */
    private String staffId;

    /**
     * 关联店铺ID
     */
    private String storeId;

    /**
     * 工作关系ID
     */
    private Long relationId;

    /**
     * 工作令牌初始版本号
     */
    private Integer tokenVersion;

    /**
     * 添加时间（yyyy-MM-dd HH:mm:ss）
     */
    private String createTime;
}

