package com.ecards.member_management.validator;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 时间段校验工具类
 * 用于校验预约时间段的合法性
 */
public class TimeSlotValidator {

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    /**
     * 校验时间段列表的合法性
     * 
     * @param timeSlots 用户请求的时间段列表
     * @param templateTimeSlots 模板的可预约时间段列表
     * @return 错误信息，null表示校验通过
     */
    public static String validateTimeSlots(List<String> timeSlots, List<String> templateTimeSlots) {
        if (timeSlots == null || timeSlots.isEmpty()) {
            return "预约时间段不能为空";
        }

        // 1. 校验每个时间段格式
        for (String slot : timeSlots) {
            String error = validateSlotFormat(slot);
            if (error != null) {
                return error;
            }
        }

        // 2. 校验时间段是否在模板允许范围内（子集校验）
        Set<String> templateSet = new HashSet<>(templateTimeSlots);
        for (String slot : timeSlots) {
            if (!templateSet.contains(slot)) {
                return "时间段 " + slot + " 不在可预约范围内";
            }
        }

        // 3. 校验时间段排序（从小到大）
        String sortError = validateSortOrder(timeSlots);
        if (sortError != null) {
            return sortError;
        }

        // 4. 校验时间段无交叉
        String overlapError = validateNoOverlap(timeSlots);
        if (overlapError != null) {
            return overlapError;
        }

        // 5. 校验不允许跳跃预约
        String jumpError = validateNoJump(timeSlots, templateTimeSlots);
        if (jumpError != null) {
            return jumpError;
        }

        return null; // 校验通过
    }

    /**
     * 校验单个时间段格式
     * 格式：HH:mm-HH:mm，如 "08:00-09:00"
     */
    private static String validateSlotFormat(String slot) {
        if (slot == null || slot.trim().isEmpty()) {
            return "时间段不能为空";
        }

        String[] parts = slot.split("-");
        if (parts.length != 2) {
            return "时间段格式错误: " + slot + "，应为 HH:mm-HH:mm";
        }

        try {
            LocalTime start = LocalTime.parse(parts[0].trim(), TIME_FORMATTER);
            LocalTime end = LocalTime.parse(parts[1].trim(), TIME_FORMATTER);

            if (!start.isBefore(end)) {
                return "时间段 " + slot + " 的结束时间必须晚于开始时间";
            }
        } catch (DateTimeParseException e) {
            return "时间段 " + slot + " 的时间格式错误";
        }

        return null;
    }

    /**
     * 校验时间段从小到大排序
     */
    private static String validateSortOrder(List<String> timeSlots) {
        for (int i = 0; i < timeSlots.size() - 1; i++) {
            LocalTime currentEnd = extractEndTime(timeSlots.get(i));
            LocalTime nextStart = extractStartTime(timeSlots.get(i + 1));

            if (currentEnd.isAfter(nextStart)) {
                return "时间段必须从小到大排列";
            }
        }
        return null;
    }

    /**
     * 校验时间段无交叉
     */
    private static String validateNoOverlap(List<String> timeSlots) {
        for (int i = 0; i < timeSlots.size() - 1; i++) {
            LocalTime currentEnd = extractEndTime(timeSlots.get(i));
            LocalTime nextStart = extractStartTime(timeSlots.get(i + 1));

            if (currentEnd.isAfter(nextStart)) {
                return "时间段 " + timeSlots.get(i) + " 和 " + timeSlots.get(i + 1) + " 存在交叉";
            }
        }
        return null;
    }

    /**
     * 校验不允许跳跃预约
     * 在模板时间段列表中，用户预约的时间段必须是连续的
     */
    private static String validateNoJump(List<String> timeSlots, List<String> templateTimeSlots) {
        if (timeSlots.size() <= 1) {
            return null; // 只有一个时间段，无需校验
        }

        // 找到第一个和最后一个时间段在模板中的位置
        int firstIndex = templateTimeSlots.indexOf(timeSlots.get(0));
        int lastIndex = templateTimeSlots.indexOf(timeSlots.get(timeSlots.size() - 1));

        if (firstIndex == -1 || lastIndex == -1) {
            return "时间段不在模板范围内"; // 理论上不会到这里，前面已经校验过
        }

        // 检查这个范围内的所有模板时间段是否都在用户预约中
        int expectedCount = lastIndex - firstIndex + 1;
        if (timeSlots.size() != expectedCount) {
            return "预约时间段不连续，不允许跳跃预约";
        }

        // 逐一检查每个位置的时间段是否匹配
        for (int i = 0; i < timeSlots.size(); i++) {
            if (!timeSlots.get(i).equals(templateTimeSlots.get(firstIndex + i))) {
                return "预约时间段不连续，不允许跳跃预约";
            }
        }

        return null;
    }

    /**
     * 计算时间段总时长（各时间段长度之和）
     * 
     * @param timeSlots 时间段列表
     * @return 总时长（分钟）
     */
    public static int calculateTotalDuration(List<String> timeSlots) {
        int totalMinutes = 0;
        for (String slot : timeSlots) {
            LocalTime start = extractStartTime(slot);
            LocalTime end = extractEndTime(slot);
            totalMinutes += java.time.Duration.between(start, end).toMinutes();
        }
        return totalMinutes;
    }

    /**
     * 从时间段列表提取开始时间（第一个时间段的开始时间）
     */
    public static String extractOverallStartTime(List<String> timeSlots) {
        if (timeSlots == null || timeSlots.isEmpty()) {
            return null;
        }
        return timeSlots.get(0).split("-")[0].trim();
    }

    /**
     * 从时间段列表提取结束时间（最后一个时间段的结束时间）
     */
    public static String extractOverallEndTime(List<String> timeSlots) {
        if (timeSlots == null || timeSlots.isEmpty()) {
            return null;
        }
        String lastSlot = timeSlots.get(timeSlots.size() - 1);
        return lastSlot.split("-")[1].trim();
    }

    /**
     * 从单个时间段提取开始时间
     */
    private static LocalTime extractStartTime(String slot) {
        String[] parts = slot.split("-");
        return LocalTime.parse(parts[0].trim(), TIME_FORMATTER);
    }

    /**
     * 从单个时间段提取结束时间
     */
    private static LocalTime extractEndTime(String slot) {
        String[] parts = slot.split("-");
        return LocalTime.parse(parts[1].trim(), TIME_FORMATTER);
    }

    /**
     * 校验预约时间是否过期（仅当预约日期为当天时）
     * 规则：如果预约日期是当天，第一个时间段的开始时间不能早于当前时间超过5分钟
     * 
     * @param timeSlots 用户请求的时间段列表
     * @param reservationDate 预约日期
     * @return 错误信息，null表示校验通过
     */
    public static String validateTimeNotExpired(List<String> timeSlots, LocalDate reservationDate) {
        if (timeSlots == null || timeSlots.isEmpty()) {
            return "预约时间段不能为空";
        }

        // 只有当预约日期是当天时才需要校验时间
        LocalDate today = LocalDate.now();
        if (!reservationDate.equals(today)) {
            return null; // 预约未来日期，无需校验时间
        }

        // 获取第一个时间段的开始时间
        String firstSlot = timeSlots.get(0);
        String[] parts = firstSlot.split("-");
        LocalTime slotStartTime = LocalTime.parse(parts[0].trim(), TIME_FORMATTER);
        
        // 获取当前时间
        LocalTime currentTime = LocalTime.now();
        
        // 计算时间差（分钟）
        long minutesDiff = java.time.Duration.between(slotStartTime, currentTime).toMinutes();
        
        // 如果当前时间晚于开始时间超过5分钟，则拒绝
        if (minutesDiff > 5) {
            return "预约时间已过期，第一个时间段开始时间不能早于当前时间超过5分钟";
        }
        
        return null;
    }
}
