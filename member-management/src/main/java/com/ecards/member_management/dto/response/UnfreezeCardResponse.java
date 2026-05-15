package com.ecards.member_management.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 解冻会员卡响应DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UnfreezeCardResponse {
    
    /**
     * 会员卡ID
     */
    private String memberCardId;
    
    /**
     * 会员卡状态：1-正常 或 2-已过期
     */
    private Integer status;
    
    /**
     * 解冻时间
     */
    private String unfreezeTime;
}
