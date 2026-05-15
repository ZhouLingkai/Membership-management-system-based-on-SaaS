package com.ecards.member_management.utils;

import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * 取消规则计算工具类
 */
@Slf4j
public class CancelRuleCalculator {

    /**
     * 计算违约费结果
     */
    public static class PenaltyResult {
        private final BigDecimal penaltyAmount;
        private final String penaltyRule;

        public PenaltyResult(BigDecimal penaltyAmount, String penaltyRule) {
            this.penaltyAmount = penaltyAmount;
            this.penaltyRule = penaltyRule;
        }

        public BigDecimal getPenaltyAmount() {
            return penaltyAmount;
        }

        public String getPenaltyRule() {
            return penaltyRule;
        }
    }

    /**
     * 计算违约费（找到最接近的规则）
     *
     * @param minutesToStart 距离预约开始时间的分钟数（可以为负数）
     * @param cancelRules 取消规则列表，格式：["60:0.1","180:5"]
     * @param transactionAmount 原交易金额
     * @param allowNegative 是否允许负数（已开始的预约）
     * @return 违约费结果
     */
    public static PenaltyResult calculatePenalty(
            long minutesToStart,
            List<String> cancelRules,
            BigDecimal transactionAmount,
            boolean allowNegative) {

        BigDecimal penaltyAmount = BigDecimal.ZERO;
        String penaltyRule = "无违约费";

        if (cancelRules == null || cancelRules.isEmpty()) {
            return new PenaltyResult(penaltyAmount, penaltyRule);
        }

        // 如果已经开始（负数）且允许处理负数
        if (minutesToStart < 0 && allowNegative) {
            // 找到最小的规则时间（最接近开始时间的规则）
            long smallestMinutes = Long.MAX_VALUE;
            String smallestRule = null;

            for (String rule : cancelRules) {
                String[] parts = rule.split(":");
                if (parts.length == 2) {
                    try {
                        long ruleMinutes = Long.parseLong(parts[0]);
                        if (ruleMinutes < smallestMinutes) {
                            smallestMinutes = ruleMinutes;
                            smallestRule = rule;
                        }
                    } catch (Exception e) {
                        log.warn("解析取消规则失败：{}", rule, e);
                    }
                }
            }

            // 应用最小规则
            if (smallestRule != null) {
                return applyRule(smallestRule, smallestMinutes, transactionAmount);
            }
        } else if (minutesToStart >= 0) {
            // 正常情况：找到满足条件且最接近的规则
            long closestMinutes = Long.MAX_VALUE;
            String closestRule = null;

            for (String rule : cancelRules) {
                String[] parts = rule.split(":");
                if (parts.length == 2) {
                    try {
                        long ruleMinutes = Long.parseLong(parts[0]);
                        // 如果当前时间满足该规则（minutesToStart <= ruleMinutes）
                        // 且该规则比之前找到的规则更接近（ruleMinutes < closestMinutes）
                        if (minutesToStart <= ruleMinutes && ruleMinutes < closestMinutes) {
                            closestMinutes = ruleMinutes;
                            closestRule = rule;
                        }
                    } catch (Exception e) {
                        log.warn("解析取消规则失败：{}", rule, e);
                    }
                }
            }

            // 应用找到的最接近规则
            if (closestRule != null) {
                return applyRule(closestRule, closestMinutes, transactionAmount);
            }
        }

        return new PenaltyResult(penaltyAmount, penaltyRule);
    }

    /**
     * 应用具体规则
     */
    private static PenaltyResult applyRule(String rule, long ruleMinutes, BigDecimal transactionAmount) {
        String[] parts = rule.split(":");
        String penaltyStr = parts[1];
        BigDecimal penaltyAmount;
        String penaltyRule;

        if (penaltyStr.contains(".")) {
            // 比例违约费（≤0.5）
            BigDecimal percentage = new BigDecimal(penaltyStr);
            penaltyAmount = transactionAmount.multiply(percentage).setScale(2, RoundingMode.HALF_UP);
            penaltyRule = String.format("距离预约开始时间%d分钟内取消，扣除%.0f%%违约费",
                    ruleMinutes, percentage.multiply(BigDecimal.valueOf(100)).doubleValue());
        } else {
            // 固定金额违约费（≥1）
            penaltyAmount = new BigDecimal(penaltyStr);
            penaltyRule = String.format("距离预约开始时间%d分钟内取消，扣除%s元违约费",
                    ruleMinutes, penaltyStr);
        }

        return new PenaltyResult(penaltyAmount, penaltyRule);
    }
}
