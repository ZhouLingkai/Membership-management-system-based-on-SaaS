package com.ecards.member_management.utils;

import com.ecards.member_management.common.ErrorCode;
import com.ecards.member_management.exception.BusinessException;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;

/**
 * 时间段校验工具类
 */
public class TimeSlotValidator {

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    /**
     * 校验时间段列表
     * 1. 时间格式合法
     * 2. 时间段从小到大排序
     * 3. 时间段不能交叉
     */
    public static void validateTimeSlots(List<String> timeSlots) {
        if (timeSlots == null || timeSlots.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "时间段列表不能为空");
        }

        // 预处理：去除所有空格
        for (int i = 0; i < timeSlots.size(); i++) {
            timeSlots.set(i, timeSlots.get(i).replaceAll("\\s+", ""));
        }

        LocalTime previousEnd = null;

        for (String slot : timeSlots) {
            // 校验格式：HH:mm-HH:mm
            if (!slot.matches("\\d{2}:\\d{2}-\\d{2}:\\d{2}")) {
                throw new BusinessException(ErrorCode.RESERVATION_TIMESLOT_FORMAT_ERROR, 
                    "时间段格式错误：" + slot);
            }

            String[] parts = slot.split("-");
            LocalTime start;
            LocalTime end;

            try {
                start = LocalTime.parse(parts[0], TIME_FORMATTER);
                end = LocalTime.parse(parts[1], TIME_FORMATTER);
            } catch (DateTimeParseException e) {
                throw new BusinessException(ErrorCode.RESERVATION_TIMESLOT_FORMAT_ERROR, 
                    "时间段格式错误：" + slot);
            }

            // 校验开始时间 < 结束时间
            if (!start.isBefore(end)) {
                throw new BusinessException(ErrorCode.RESERVATION_TIMESLOT_FORMAT_ERROR, 
                    "时间段开始时间必须早于结束时间：" + slot);
            }

            // 校验时间段长度至少30分钟
            long minutes = java.time.Duration.between(start, end).toMinutes();
            if (minutes < 30) {
                throw new BusinessException(ErrorCode.RESERVATION_TIMESLOT_FORMAT_ERROR, 
                    "时间段长度至少30分钟：" + slot + "（当前" + minutes + "分钟）");
            }

            // 校验时间段顺序和交叉
            if (previousEnd != null) {
                if (start.isBefore(previousEnd)) {
                    throw new BusinessException(ErrorCode.RESERVATION_TIMESLOT_OVERLAP, 
                        "时间段存在交叉");
                }
            }

            previousEnd = end;
        }
    }

    /**
     * 计算连续时间（各时间段长度之和）
     * 示例：["08:00-08:50", "09:00-09:50"] = 100分钟
     */
    public static int calculateContinuousTime(List<String> timeSlots) {
        if (timeSlots == null || timeSlots.isEmpty()) {
            return 0;
        }

        int totalMinutes = 0;

        for (String slot : timeSlots) {
            String[] parts = slot.split("-");
            LocalTime start = LocalTime.parse(parts[0], TIME_FORMATTER);
            LocalTime end = LocalTime.parse(parts[1], TIME_FORMATTER);
            
            // 计算每个时间段的分钟数
            int minutes = (int) java.time.Duration.between(start, end).toMinutes();
            totalMinutes += minutes;
        }

        return totalMinutes;
    }
}
