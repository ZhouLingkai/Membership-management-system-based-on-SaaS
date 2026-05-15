package com.ecards.member_management.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 商户审核列表查询请求DTO
 * 
 * @author Ecards Team
 * @since 2025-10-28
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MerchantAuditListRequest {

    /**
     * 审核状态筛选
     * null - 全部
     * 3 - 待审核（包含3和13）
     * 1 - 已通过
     * 2 - 已拒绝
     */
    private Integer auditStatus;

    /**
     * 排序方向
     * 0 - 时间升序（ASC）
     * 1 - 时间降序（DESC，默认）
     */
    @Builder.Default
    private Integer sortOrder = 1;

    /**
     * 页码（从1开始）
     */
    @NotNull(message = "页码不能为空")
    @Min(value = 1, message = "页码必须大于0")
    @Builder.Default
    private Integer pageNum = 1;

    /**
     * 每页大小
     */
    @NotNull(message = "每页大小不能为空")
    @Min(value = 1, message = "每页大小必须大于0")
    @Max(value = 100, message = "每页大小不能超过100")
    @Builder.Default
    private Integer pageSize = 10;
}

