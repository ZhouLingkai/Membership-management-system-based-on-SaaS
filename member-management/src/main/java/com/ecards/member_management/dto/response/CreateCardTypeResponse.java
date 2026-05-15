package com.ecards.member_management.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 会员卡种创建响应DTO
 * 
 * @author Ecards Team
 * @since 2025-11-02
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateCardTypeResponse {

    /**
     * 卡种ID（自增主键）
     */
    private Long cardTypeId;

    /**
     * 店铺ID
     */
    private String storeId;

    /**
     * 商家ID
     */
    private String merchantId;

    /**
     * 卡种名称
     */
    private String cardTypeName;

    /**
     * 卡种类型
     */
    private Integer cardTtype;

    /**
     * 创建时间（yyyy-MM-dd HH:mm:ss）
     */
    private String createTime;
}

