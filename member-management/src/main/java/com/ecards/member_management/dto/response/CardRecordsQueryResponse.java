package com.ecards.member_management.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * 单卡交易记录查询响应DTO（接口4）
 * 
 * @author Ecards Team
 * @since 2025-11-05
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CardRecordsQueryResponse {

    /**
     * 会员卡信息
     */
    private CardInfo cardInfo;

    /**
     * 总记录数
     */
    private Long total;

    /**
     * 当前页码
     */
    private Integer pageNum;

    /**
     * 每页条数
     */
    private Integer pageSize;

    /**
     * 交易记录列表
     */
    private List<TransactionRecordVO> list;

    /**
     * 会员卡信息内部类
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CardInfo {
        /**
         * 会员卡ID
         */
        private String memberCardId;

        /**
         * 卡种名称
         */
        private String cardTypeName;

        /**
         * 卡种类型
         */
        private Integer cardTtype;

        /**
         * 当前余额/次数
         */
        private BigDecimal currentBalance;
    }
}

