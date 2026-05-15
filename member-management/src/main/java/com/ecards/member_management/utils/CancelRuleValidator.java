package com.ecards.member_management.utils;

import com.ecards.member_management.common.ErrorCode;
import com.ecards.member_management.exception.BusinessException;

import java.math.BigDecimal;
import java.util.List;

/**
 * 取消规则校验工具类
 */
public class CancelRuleValidator {

    /**
     * 校验取消规则格式
     * 格式：["分钟:费用","分钟:费用"]
     * 示例：["60:0.1","180:5"]
     */
    public static void validateCancelRule(List<String> cancelRule) {
        if (cancelRule == null || cancelRule.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "取消规则不能为空");
        }

        // 预处理：去除所有空格
        for (int i = 0; i < cancelRule.size(); i++) {
            cancelRule.set(i, cancelRule.get(i).replaceAll("\\s+", ""));
        }

        for (String rule : cancelRule) {
            if (!rule.matches("\\d+:(\\d+\\.?\\d*)")) {
                throw new BusinessException(ErrorCode.PARAM_ERROR, 
                    "取消规则格式错误：" + rule);
            }

            String[] parts = rule.split(":");
            int minutes = Integer.parseInt(parts[0]);
            BigDecimal fee = new BigDecimal(parts[1]);

            if (minutes <= 0) {
                throw new BusinessException(ErrorCode.PARAM_ERROR, 
                    "取消规则分钟数必须>0");
            }

            if (fee.compareTo(BigDecimal.ZERO) < 0) {
                throw new BusinessException(ErrorCode.PARAM_ERROR, 
                    "取消规则费用必须≥0");
            }
        }
    }

    /**
     * 计算取消费用
     * 费用规则：≤0.5为比例，≥1为固定金额
     */
    public static BigDecimal calculateCancelFee(int minutesBeforeStart, 
                                                  List<String> cancelRule, 
                                                  BigDecimal totalAmount) {
        if (cancelRule == null || cancelRule.isEmpty()) {
            return BigDecimal.ZERO;
        }

        for (String rule : cancelRule) {
            String[] parts = rule.split(":");
            int ruleMinutes = Integer.parseInt(parts[0]);
            BigDecimal fee = new BigDecimal(parts[1]);

            if (minutesBeforeStart <= ruleMinutes) {
                // ≤0.5为比例，≥1为固定金额
                if (fee.compareTo(new BigDecimal("0.5")) <= 0) {
                    return totalAmount.multiply(fee);
                } else {
                    return fee;
                }
            }
        }

        return BigDecimal.ZERO;
    }
}
