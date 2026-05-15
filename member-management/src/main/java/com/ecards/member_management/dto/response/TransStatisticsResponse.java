package com.ecards.member_management.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * 流水数据统计响应DTO
 * 
 * @author Ecards Team
 * @since 2025-12-17
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransStatisticsResponse {

    /**
     * 店铺ID
     */
    private String storeId;

    /**
     * 店铺名称
     */
    private String storeName;

    /**
     * 商家ID
     */
    private String merchantId;

    /**
     * 商家名称
     */
    private String merchantName;

    /**
     * 是否包含店铺统计
     */
    private Boolean haveStoreStatistics;

    /**
     * 店铺统计数据
     */
    private List<DailyStatistics> storeStatistics;

    /**
     * 是否包含商家统计
     */
    private Boolean haveMerchantStatistics;

    /**
     * 商家统计数据
     */
    private List<DailyStatistics> merchantStatistics;

    /**
     * 每日统计数据
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DailyStatistics {
        /**
         * 日期（YYYY-MM-DD）
         */
        private String date;

        /**
         * 是否有余额卡数据
         */
        private Boolean haveBalanceCard;

        /**
         * 是否有次数卡数据
         */
        private Boolean haveCountCard;

        /**
         * 余额卡数据
         */
        private BalanceCardData balanceCardData;

        /**
         * 次数卡数据
         */
        private CountCardData countCardData;

        /**
         * 会员数据
         */
        private MemberData memberData;
    }

    /**
     * 余额卡数据
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BalanceCardData {
        /**
         * 消费总额
         */
        private BigDecimal consumeAmount;

        /**
         * 消费笔数
         */
        private Integer consumeCount;

        /**
         * 充值总额
         */
        private BigDecimal rechargeAmount;

        /**
         * 充值笔数
         */
        private Integer rechargeCount;

        /**
         * 退款总额（正数）
         */
        private BigDecimal refundAmount;

        /**
         * 退款笔数
         */
        private Integer refundCount;
    }

    /**
     * 次数卡数据
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CountCardData {
        /**
         * 次数消耗统计
         */
        private Integer consumeTimes;

        /**
         * 次数充值统计
         */
        private Integer rechargeTimes;
    }

    /**
     * 会员数据
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MemberData {
        /**
         * 新增会员数量
         */
        private Integer newMembers;

        /**
         * 新办会员卡数量
         */
        private Integer newMemberCards;
    }
}
