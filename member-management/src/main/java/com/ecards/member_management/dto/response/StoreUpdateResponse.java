package com.ecards.member_management.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 店铺信息修改响应DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StoreUpdateResponse {

    /**
     * 信息更新时间
     */
    private String updateTime;
}

