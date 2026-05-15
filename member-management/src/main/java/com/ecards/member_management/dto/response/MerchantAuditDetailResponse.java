package com.ecards.member_management.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 商户审核详情响应DTO
 * 
 * @author Ecards Team
 * @since 2025-10-28
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MerchantAuditDetailResponse {

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
     * 店铺规模
     */
    private Integer numStores;

    /**
     * 会员规模
     */
    private String numMembers;

    /**
     * 第一家店铺名称
     */
    private String storeName;

    /**
     * 门头店照（OSS URL，逗号分隔）
     */
    private String storePhotos;

    /**
     * 营业执照（OSS URL，逗号分隔）
     */
    private String businessLicense;

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
     * 拒绝原因
     */
    private String rejectReason;

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

