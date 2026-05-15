package com.ecards.member_management.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 时效调整响应DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExpireAdjustResponse {

    /**
     * 交易记录ID
     */
    private Long transactionId;

    /**
     * 会员卡ID
     */
    private String memberCardId;

    /**
     * 卡种名称
     */
    private String cardTypeName;

    /**
     * 原到期时间
     */
    private String oldExpireTime;

    /**
     * 新到期时间
     */
    private String newExpireTime;

    /**
     * 调整类型：1-相对延期，2-绝对设置
     */
    private Integer adjustType;

    /**
     * 调整类型名称
     */
    private String adjustTypeName;

    /**
     * 操作时间
     */
    private String operateTime;
}

