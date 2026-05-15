package com.ecards.member_management.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 商户审核通过请求DTO
 * 
 * @author Ecards Team
 * @since 2025-10-28
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MerchantAuditApproveRequest {

    /**
     * 审核记录ID
     */
    @NotNull(message = "审核记录ID不能为空")
    private Long auditId;

    /**
     * 审核备注
     */
    @Size(max = 255, message = "备注长度不能超过255个字符")
    private String remark;
}

