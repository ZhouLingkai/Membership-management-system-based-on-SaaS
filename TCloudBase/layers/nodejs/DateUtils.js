'use strict';

const TZ_OFFSET_MS = 8 * 60 * 60 * 1000;

/**
 * 返回当前时间的 Date 对象（UTC+8）
 * 云数据库写入直接用
 * @returns {Date}
 */
function nowDate() {
    return new Date();
}

/**
 * 返回当前毫秒级时间戳
 * @returns {number}
 */
function nowTimestamp() {
    return Date.now();
}

/**
 * 返回次日零点的 Date 对象（UTC+8）
 * 用于 verify_codes.expireTime 等每日清理场景
 * @returns {Date}
 */
function tomorrowMidnight() {
    const now = new Date(Date.now() + TZ_OFFSET_MS);
    const tomorrow = new Date(Date.UTC(
        now.getUTCFullYear(),
        now.getUTCMonth(),
        now.getUTCDate() + 1,
        0, 0, 0, 0
    ));
    return new Date(tomorrow.getTime() - TZ_OFFSET_MS);
}

/**
 * 在给定 Date 基础上加秒数，返回新 Date
 * @param {Date} date
 * @param {number} seconds
 * @returns {Date}
 */
function addSeconds(date, seconds) {
    return new Date(date.getTime() + seconds * 1000);
}

/**
 * 在给定 Date 基础上加分钟数，返回新 Date
 * 用于验证码过期时间（+5分钟）、频率限制（+1分钟）等
 * @param {Date} date
 * @param {number} minutes
 * @returns {Date}
 */
function addMinutes(date, minutes) {
    return new Date(date.getTime() + minutes * 60 * 1000);
}

/**
 * 在给定 Date 基础上加天数，返回新 Date
 * 用于时长会员到期时间计算
 * @param {Date} date
 * @param {number} days
 * @returns {Date}
 */
function addDays(date, days) {
    return new Date(date.getTime() + days * 24 * 60 * 60 * 1000);
}

/**
 * 判断给定 Date 是否已过期
 * @param {Date} date
 * @returns {boolean}
 */
function isExpired(date) {
    return date < new Date();
}

/**
 * 返回 'YYYY-MM-DD' 格式字符串（UTC+8）
 * 用于预约记录 reservationDate 字段
 * @param {Date} date
 * @returns {string}
 */
function formatDate(date) {
    const local = new Date(date.getTime() + TZ_OFFSET_MS);
    const y = local.getUTCFullYear();
    const m = String(local.getUTCMonth() + 1).padStart(2, '0');
    const d = String(local.getUTCDate()).padStart(2, '0');
    return `${y}-${m}-${d}`;
}

/**
 * 返回中文星期字符串（'周一'~'周日'）
 * 用于 forbiddenDays 匹配
 * @param {Date} date
 * @returns {string}
 */
function getDayOfWeek(date) {
    const local = new Date(date.getTime() + TZ_OFFSET_MS);
    const map = ['周日', '周一', '周二', '周三', '周四', '周五', '周六'];
    return map[local.getUTCDay()];
}

module.exports = { nowDate, nowTimestamp, tomorrowMidnight, addSeconds, addMinutes, addDays, isExpired, formatDate, getDayOfWeek };
