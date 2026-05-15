package com.ecards.member_management.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 商户基础信息修改响应DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MerchantInfoUpdateResponse {

    /**
     * 信息更新时间（yyyy-MM-dd HH:mm:ss格式）
     */
    private String updateTime;
}

