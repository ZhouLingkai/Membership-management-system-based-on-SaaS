package com.ecards.member_management.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 批量激活会员卡响应DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ActivateBatchResponse {
    
    /**
     * 激活的会员卡数量
     */
    private Integer activatedCount;
    
    /**
     * 已激活的会员卡列表
     */
    private List<ActivatedCardInfo> activatedCards;
    
    /**
     * 激活的卡信息
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ActivatedCardInfo {
        
        /**
         * 会员卡ID
         */
        private String memberCardId;
        
        /**
         * 卡种名称
         */
        private String cardTypeName;
        
        /**
         * 店铺名称
         */
        private String storeName;
    }
}

