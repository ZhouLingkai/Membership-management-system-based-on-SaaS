package com.ecards.member_management.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 商户审核列表响应DTO
 * 
 * @author Ecards Team
 * @since 2025-10-28
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MerchantAuditListResponse {

    /**
     * 审核记录列表
     */
    private List<AuditRecordItem> records;

    /**
     * 总记录数
     */
    private Long total;

    /**
     * 当前页码
     */
    private Integer pageNum;

    /**
     * 每页大小
     */
    private Integer pageSize;

    /**
     * 总页数
     */
    private Integer totalPages;

    /**
     * 审核记录项
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AuditRecordItem {
        /**
         * 审核记录ID
         */
        private Long auditId;

        /**
         * 申请用户ID（UUID字符串）
         */
        private String userId;

        /**
         * 用户手机号（脱敏）
         */
        private String userPhone;

        /**
         * 店铺名称
         */
        private String storeName;

        /**
         * 申请方式
         * 1 - 直接认证
         * 2 - 免认证后续补充
         */
        private Integer applicationMethod;

        /**
         * 申请方式描述
         */
        private String applicationMethodDesc;

        /**
         * 审核状态
         * 3 - 待审核
         * 1 - 已通过
         * 2 - 已拒绝
         */
        private Integer auditStatus;

        /**
         * 审核状态描述
         */
        private String auditStatusDesc;

        /**
         * 申请时间
         */
        private String createTime;

        /**
         * 审核时间
         */
        private String auditTime;

        /**
         * 审核人账号
         */
        private String auditorAccount;
    }
}

