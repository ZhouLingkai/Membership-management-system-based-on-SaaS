package com.ecards.member_management.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 商户基础信息查询响应DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MerchantInfoResponse {

    /**
     * 商户ID
     */
    private String merchantId;

    /**
     * 商户名称
     */
    private String merchantName;

    /**
     * 认证状态：1-已认证，2-测试中，3-审核中，4-审核拒绝，5-测试期过
     */
    private Integer certification;

    /**
     * 商家特权等级：1-普通，2-VIP，3-SVIP
     */
    private Integer merchantLevel;

    /**
     * 测试期过期时间（certification=2时返回）
     */
    private String testExpireTime;

    /**
     * 测试期剩余天数（certification=2时返回）
     */
    private Integer remainingDays;

    /**
     * 联系电话（来自用户表）
     */
    private String contactPhone;

    /**
     * 联系邮箱
     */
    private String contactEmail;

    /**
     * 商户简介
     */
    private String merchantIntro;

    /**
     * 剩余消息通知次数
     */
    private Integer remainingNoticeCount;

    /**
     * 商户创建时间
     */
    private String createTime;

    /**
     * 关联店铺数量
     */
    private Integer storeCount;
}

