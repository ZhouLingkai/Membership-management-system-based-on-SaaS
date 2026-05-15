package com.ecards.member_management.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 商户审核详情查询请求DTO
 * 
 * @author Ecards Team
 * @since 2025-10-28
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MerchantAuditDetailRequest {

    /**
     * 审核记录ID
     */
    @NotNull(message = "审核记录ID不能为空")
    private Long auditId;
}

