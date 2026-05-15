package com.ecards.member_management.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 冻结会员卡响应DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FreezeCardResponse {
    
    /**
     * 会员卡ID
     */
    private String memberCardId;
    
    /**
     * 会员卡状态：3-已冻结
     */
    private Integer status;
    
    /**
     * 冻结时间
     */
    private String freezeTime;
}
