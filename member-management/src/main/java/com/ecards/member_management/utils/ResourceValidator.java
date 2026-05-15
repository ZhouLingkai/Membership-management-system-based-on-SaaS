package com.ecards.member_management.utils;

import com.ecards.member_management.common.ErrorCode;
import com.ecards.member_management.exception.BusinessException;

/**
 * 资源校验工具类
 */
public class ResourceValidator {

    /**
     * 校验次数卡规则：min = max
     */
    public static void validateTimesCardResource(Integer minTime, Integer maxTime, Integer cardType) {
        if (cardType == 2) { // 次数卡
            if (!minTime.equals(maxTime)) {
                throw new BusinessException(ErrorCode.PARAM_ERROR, 
                    "次数卡要求最少连续时间等于最大连续时间");
            }
        }
    }

    /**
     * 校验连续时间范围
     */
    public static void validateContinuousTime(Integer minTime, Integer maxTime) {
        if (minTime < 0) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, 
                "最少连续时间必须≥0");
        }
        if (maxTime > 1440) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, 
                "最大连续时间必须≤1440");
        }
        if (minTime > maxTime) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, 
                "最少连续时间不能大于最大连续时间");
        }
    }
}
