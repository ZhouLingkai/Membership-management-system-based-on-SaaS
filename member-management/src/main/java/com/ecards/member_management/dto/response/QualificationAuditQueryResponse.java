package com.ecards.member_management.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 商户资质审核结果查询响应
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QualificationAuditQueryResponse {
    
    /**
     * 审核记录ID
     */
    private Long auditId;
    
    /**
     * 审核状态：WAIT - 待审核，PASSED - 通过，REJECTED - 驳回
     */
    private String auditStatus;
    
    /**
     * 申请提交时间（yyyy-MM-dd HH:mm:ss 格式）
     */
    private String submitTime;
    
    /**
     * 审核完成时间（仅 auditStatus≠WAIT 时返回）
     */
    private String auditTime;
    
    /**
     * 驳回原因（仅 auditStatus=REJECTED 时返回）
     */
    private String rejectReason;
}

