package com.ecards.member_management.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 商户注册响应DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MerchantRegistrationResponse {

    /**
     * 审核记录ID（直接认证通道返回）
     */
    private Long auditId;

    /**
     * 商户ID（免认证通道返回，UUID字符串）
     */
    private String merchantId;

    /**
     * 商户名称
     */
    private String merchantName;

    /**
     * 认证状态：2-测试中，3-审核中
     */
    private Integer certification;

    /**
     * 申请时间（yyyy-MM-dd HH:mm:ss格式）
     */
    private String applyTime;

    /**
     * 测试期过期时间（仅certification=2时返回）
     */
    private String testExpireTime;

    /**
     * 提示消息
     */
    private String message;
}

