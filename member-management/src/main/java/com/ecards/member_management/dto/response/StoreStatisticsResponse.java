package com.ecards.member_management.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 店铺交易统计响应DTO（接口6）
 * 
 * @author Ecards Team
 * @since 2025-11-05
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StoreStatisticsResponse {

    /**
     * 店铺信息
     */
    private StoreInfo storeInfo;

    /**
     * 统计日期范围
     */
    private DateRange dateRange;

    /**
     * 充值统计
     */
    private RechargeStats rechargeStats;

    /**
     * 消费统计
     */
    private ConsumeStats consumeStats;

    /**
     * 退款统计
     */
    private RefundStats refundStats;

    /**
     * 店铺信息内部类
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StoreInfo {
        /**
         * 店铺ID
         */
        private String storeId;

        /**
         * 店铺名称
         */
        private String storeName;
    }

    /**
     * 日期范围内部类
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DateRange {
        /**
         * 开始日期
         */
        private String startDate;

        /**
         * 结束日期
         */
        private String endDate;
    }

    /**
     * 充值统计内部类
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RechargeStats {
        /**
         * 充值总金额（余额卡）
         */
        private BigDecimal totalAmount;

        /**
         * 充值笔数
         */
        private Integer totalCount;
    }

    /**
     * 消费统计内部类
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ConsumeStats {
        /**
         * 消费总金额（余额卡）
         */
        private BigDecimal totalAmount;

        /**
         * 消费笔数
         */
        private Integer totalCount;
    }

    /**
     * 退款统计内部类
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RefundStats {
        /**
         * 退款总金额（余额卡，正数）
         */
        private BigDecimal totalAmount;

        /**
         * 退款笔数
         */
        private Integer totalCount;
    }
}
