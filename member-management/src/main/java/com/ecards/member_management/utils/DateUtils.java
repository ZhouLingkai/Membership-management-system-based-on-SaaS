package com.ecards.member_management.utils;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

/**
 * 日期工具类
 * 提供时间戳与LocalDateTime之间的转换功能
 * 时区统一为 Asia/Shanghai
 */
public class DateUtils {

    /**
     * 时区：Asia/Shanghai
     */
    private static final ZoneId ZONE_ID = ZoneId.of("Asia/Shanghai");

    /**
     * LocalDateTime 转时间戳（毫秒级）
     * 适配业务时间记录如开卡时间、交易时间
     *
     * @param localDateTime LocalDateTime对象
     * @return 毫秒级时间戳
     */
    public static Long toTimestamp(LocalDateTime localDateTime) {
        if (localDateTime == null) {
            return null;
        }
        ZonedDateTime zonedDateTime = localDateTime.atZone(ZONE_ID);
        return zonedDateTime.toInstant().toEpochMilli();
    }

    /**
     * 时间戳（毫秒级）转 LocalDateTime
     *
     * @param timestamp 毫秒级时间戳
     * @return LocalDateTime对象
     */
    public static LocalDateTime toLocalDateTime(Long timestamp) {
        if (timestamp == null) {
            return null;
        }
        Instant instant = Instant.ofEpochMilli(timestamp);
        return LocalDateTime.ofInstant(instant, ZONE_ID);
    }

    /**
     * 获取当前时间戳（毫秒级）
     *
     * @return 当前毫秒级时间戳
     */
    public static Long getCurrentTimestamp() {
        return System.currentTimeMillis();
    }

    /**
     * 获取当前LocalDateTime（Asia/Shanghai时区）
     *
     * @return 当前LocalDateTime对象
     */
    public static LocalDateTime getCurrentLocalDateTime() {
        return LocalDateTime.now(ZONE_ID);
    }
}

