package com.ecards.member_management.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 商户审核拒绝请求DTO
 * 
 * @author Ecards Team
 * @since 2025-10-28
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MerchantAuditRejectRequest {

    /**
     * 审核记录ID
     */
    @NotNull(message = "审核记录ID不能为空")
    private Long auditId;

    /**
     * 拒绝原因
     */
    @NotBlank(message = "拒绝原因不能为空")
    @Size(min = 1, max = 200, message = "拒绝原因长度必须在1到200个字符之间")
    private String rejectReason;
}

