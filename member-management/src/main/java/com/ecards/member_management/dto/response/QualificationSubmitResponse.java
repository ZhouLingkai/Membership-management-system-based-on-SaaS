package com.ecards.member_management.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 商户资质补充提交响应
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QualificationSubmitResponse {
    
    /**
     * 审核状态：WAIT - 待审核
     */
    private String auditStatus;
    
    /**
     * 资质提交时间（yyyy-MM-dd HH:mm:ss 格式）
     */
    private String submitTime;
    
    /**
     * 更新后的认证状态：3 - 审核中，13 - 过期后审核中
     */
    private Integer certification;
}

