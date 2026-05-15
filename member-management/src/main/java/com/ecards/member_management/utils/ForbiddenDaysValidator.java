package com.ecards.member_management.utils;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 禁止日期校验工具类
 */
public class ForbiddenDaysValidator {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final Map<String, DayOfWeek> WEEKDAY_MAP = new HashMap<>();

    static {
        WEEKDAY_MAP.put("周一", DayOfWeek.MONDAY);
        WEEKDAY_MAP.put("周二", DayOfWeek.TUESDAY);
        WEEKDAY_MAP.put("周三", DayOfWeek.WEDNESDAY);
        WEEKDAY_MAP.put("周四", DayOfWeek.THURSDAY);
        WEEKDAY_MAP.put("周五", DayOfWeek.FRIDAY);
        WEEKDAY_MAP.put("周六", DayOfWeek.SATURDAY);
        WEEKDAY_MAP.put("周日", DayOfWeek.SUNDAY);
    }

    /**
     * 清理过期日期（仅保留星期和未过期的具体日期）
     * 注意：此方法由定时任务调用，不在查询接口中使用
     */
    public static List<String> cleanExpiredDates(List<String> forbiddenDays) {
        if (forbiddenDays == null || forbiddenDays.isEmpty()) {
            return forbiddenDays;
        }

        // 预处理：去除所有空格
        for (int i = 0; i < forbiddenDays.size(); i++) {
            forbiddenDays.set(i, forbiddenDays.get(i).replaceAll("\\s+", ""));
        }

        List<String> cleaned = new ArrayList<>();
        LocalDate today = LocalDate.now();

        for (String day : forbiddenDays) {
            // 如果是星期，保留
            if (WEEKDAY_MAP.containsKey(day)) {
                cleaned.add(day);
                continue;
            }

            // 如果是具体日期，检查是否过期
            try {
                LocalDate date = LocalDate.parse(day, DATE_FORMATTER);
                if (!date.isBefore(today)) {
                    cleaned.add(day);
                }
            } catch (DateTimeParseException e) {
                // 格式错误，跳过
            }
        }

        return cleaned;
    }

    /**
     * 校验某日期是否可预约（单个列表）
     * flag=1不可预约，flag=2可预约（负负得正，支持节假日补班）
     */
    public static boolean isDateReservable(LocalDate date, List<String> forbiddenDays) {
        if (forbiddenDays == null || forbiddenDays.isEmpty()) {
            return true;
        }

        // 预处理：去除所有空格
        List<String> cleanedDays = new ArrayList<>();
        for (String day : forbiddenDays) {
            cleanedDays.add(day.replaceAll("\\s+", ""));
        }

        int flag = 0;
        DayOfWeek dayOfWeek = date.getDayOfWeek();
        String dateStr = date.format(DATE_FORMATTER);

        // 检查星期是否在禁止列表中
        for (Map.Entry<String, DayOfWeek> entry : WEEKDAY_MAP.entrySet()) {
            if (entry.getValue() == dayOfWeek && cleanedDays.contains(entry.getKey())) {
                flag++;
                break;
            }
        }

        // 检查具体日期是否在禁止列表中
        if (cleanedDays.contains(dateStr)) {
            flag++;
        }

        // flag=1不可预约，flag=2可预约
        return flag != 1;
    }

    /**
     * 校验某日期是否可预约（考虑模板和资源的自定义规则）
     * 
     * @param date 预约日期
     * @param templateForbiddenDays 模板的禁止日期列表
     * @param resourceForbiddenDays 资源的禁止日期列表（可为null）
     * @param customizeForbidden 是否启用资源自定义禁止日期：0-不启用，1-启用
     * @return true-可预约，false-不可预约
     */
    public static boolean isDateReservableWithCustomize(LocalDate date, 
                                                        List<String> templateForbiddenDays,
                                                        List<String> resourceForbiddenDays,
                                                        Integer customizeForbidden) {
        if (customizeForbidden == null || customizeForbidden == 0) {
            // 不启用资源自定义，仅使用模板的forbidden_days
            return isDateReservable(date, templateForbiddenDays);
        } else {
            // 启用资源自定义，需要模板和资源都能预约才能预约
            boolean templateAllows = isDateReservable(date, templateForbiddenDays);
            boolean resourceAllows = isDateReservable(date, resourceForbiddenDays);
            return templateAllows && resourceAllows;
        }
    }
}

