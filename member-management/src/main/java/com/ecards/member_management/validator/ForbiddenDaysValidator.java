package com.ecards.member_management.validator;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * forbiddenDays字段校验工具类
 * 校验规则：
 * 1. 允许的星期：周一、周二、周三、周四、周五、周六、周日
 * 2. 允许的日期格式：YYYY-MM-DD，范围为今天到今天+30天
 * 3. 日期必须真实存在（考虑闰年等）
 */
public class ForbiddenDaysValidator {

    private static final Set<String> VALID_WEEKDAYS = new HashSet<>(Arrays.asList(
            "周一", "周二", "周三", "周四", "周五", "周六", "周日"
    ));

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    
    // 日期范围：30天
    private static final int MAX_DAYS_AHEAD = 30;

    /**
     * 校验forbiddenDays列表
     * 
     * @param forbiddenDays 禁止日期列表
     * @return 校验通过返回null，否则返回错误信息
     */
    public static String validate(List<String> forbiddenDays) {
        if (forbiddenDays == null || forbiddenDays.isEmpty()) {
            return null; // 空数组允许
        }

        LocalDate today = LocalDate.now();
        LocalDate maxDate = today.plusDays(MAX_DAYS_AHEAD);

        for (String item : forbiddenDays) {
            if (item == null || item.trim().isEmpty()) {
                return "forbiddenDays包含空元素";
            }

            String trimmedItem = item.trim();

            // 检查是否为星期格式
            if (VALID_WEEKDAYS.contains(trimmedItem)) {
                continue; // 有效的星期
            }

            // 检查是否为日期格式
            try {
                LocalDate date = LocalDate.parse(trimmedItem, DATE_FORMATTER);
                
                // 校验日期范围：今天 <= date <= 今天+30天
                if (date.isBefore(today)) {
                    return "forbiddenDays包含过去的日期: " + trimmedItem;
                }
                
                if (date.isAfter(maxDate)) {
                    return "forbiddenDays包含超出30天范围的日期: " + trimmedItem;
                }
                
            } catch (DateTimeParseException e) {
                // 既不是有效的星期，也不是有效的日期格式
                return "forbiddenDays包含无效元素: " + trimmedItem + 
                       "（应为周一~周日或yyyy-MM-dd格式的日期）";
            }
        }

        return null; // 校验通过
    }

    /**
     * 校验单个元素
     * 
     * @param item 单个禁止日期元素
     * @return 是否有效
     */
    public static boolean isValidItem(String item) {
        if (item == null || item.trim().isEmpty()) {
            return false;
        }

        String trimmedItem = item.trim();

        // 检查是否为星期
        if (VALID_WEEKDAYS.contains(trimmedItem)) {
            return true;
        }

        // 检查是否为有效日期
        try {
            LocalDate date = LocalDate.parse(trimmedItem, DATE_FORMATTER);
            LocalDate today = LocalDate.now();
            LocalDate maxDate = today.plusDays(MAX_DAYS_AHEAD);
            
            return !date.isBefore(today) && !date.isAfter(maxDate);
        } catch (DateTimeParseException e) {
            return false;
        }
    }
}
