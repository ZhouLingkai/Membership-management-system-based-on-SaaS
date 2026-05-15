package com.ecards.member_management.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 商户资质审核结果查询请求
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QualificationAuditQueryRequest {
    
    /**
     * 用户ID（与令牌一致）
     */
    @NotBlank(message = "用户ID不能为空")
    private String userId;
}

