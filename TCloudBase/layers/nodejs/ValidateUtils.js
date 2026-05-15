'use strict';

const WEEKDAY_MAP = { '周一': 1, '周二': 2, '周三': 3, '周四': 4, '周五': 5, '周六': 6, '周日': 0 };
const DATE_RE = /^\d{4}-\d{2}-\d{2}$/;
const TIME_SLOT_RE = /^\d{2}:\d{2}-\d{2}:\d{2}$/;

// ==================== 基础字段校验 ====================

/**
 * 校验手机号格式（11位，1开头中国大陆号段）
 * @param {string} phone
 * @returns {boolean}
 */
function validatePhone(phone) {
    return /^1[3-9]\d{9}$/.test(phone);
}

/**
 * 密码强度校验：8~20位，必须包含字母和数字
 * @param {string} password
 * @returns {{ valid: boolean, message: string }}
 */
function validatePassword(password) {
    if (!password || password.length < 8 || password.length > 20) {
        return { valid: false, message: '密码长度必须在8~20位之间' };
    }
    if (!/[a-zA-Z]/.test(password)) {
        return { valid: false, message: '密码必须包含字母' };
    }
    if (!/\d/.test(password)) {
        return { valid: false, message: '密码必须包含数字' };
    }
    return { valid: true, message: '' };
}

// ==================== 预约相关校验 ====================

/**
 * 解析 'HH:mm' 为分钟数
 */
function _timeToMinutes(t) {
    const [h, m] = t.split(':').map(Number);
    return h * 60 + m;
}

/**
 * 校验预约时间段列表
 * 格式：['HH:mm-HH:mm', ...]
 * 规则：格式合法、开始 < 结束、时长 ≥ 30 分钟、时间段不交叉、从小到大排序
 * @param {string[]} timeSlots
 * @throws {Error}
 */
function validateTimeSlots(timeSlots) {
    if (!timeSlots || timeSlots.length === 0) throw new Error('时间段列表不能为空');
    let prevEndMin = -1;
    for (const raw of timeSlots) {
        const slot = raw.replace(/\s+/g, '');
        if (!TIME_SLOT_RE.test(slot)) throw new Error(`时间段格式错误：${slot}`);
        const dashIdx = slot.indexOf('-', 3);
        const startStr = slot.slice(0, dashIdx);
        const endStr = slot.slice(dashIdx + 1);
        const startMin = _timeToMinutes(startStr);
        const endMin = _timeToMinutes(endStr);
        if (startMin >= endMin) throw new Error(`时间段开始时间必须早于结束时间：${slot}`);
        if (endMin - startMin < 30) throw new Error(`时间段长度至少30分钟：${slot}（当前${endMin - startMin}分钟）`);
        if (startMin < prevEndMin) throw new Error(`时间段存在交叉：${slot}`);
        prevEndMin = endMin;
    }
}

const PENALTY_TYPES = new Set(['prohibit', 'ratio', 'fixed']);

/**
 * 校验取消规则格式（对象数组格式）
 * 格式：[{ minBefore: Number, value: Number|null, penaltyType: String }, ...]
 * penaltyType: 'prohibit'（禁止取消，value=null）| 'ratio'（按比例，0<value<=1）| 'fixed'（固定金额，value>=0）
 * @param {Object[]} cancelRules
 * @throws {Error}
 */
function validateCancelRules(cancelRules) {
    if (!cancelRules || cancelRules.length === 0) throw new Error('取消规则不能为空');
    for (const rule of cancelRules) {
        if (typeof rule !== 'object' || rule === null) throw new Error('取消规则每项必须是对象');
        if (typeof rule.minBefore !== 'number' || rule.minBefore <= 0) {
            throw new Error('取消规则 minBefore 必须是大于0的数字');
        }
        if (!PENALTY_TYPES.has(rule.penaltyType)) {
            throw new Error(`取消规则 penaltyType 必须是 prohibit/ratio/fixed 之一，当前：${rule.penaltyType}`);
        }
        if (rule.penaltyType === 'prohibit') {
            if (rule.value !== null && rule.value !== undefined) {
                throw new Error('penaltyType=prohibit 时 value 必须为 null');
            }
        } else if (rule.penaltyType === 'ratio') {
            if (typeof rule.value !== 'number' || rule.value <= 0 || rule.value > 1) {
                throw new Error('penaltyType=ratio 时 value 必须在 (0, 1] 范围内');
            }
        } else if (rule.penaltyType === 'fixed') {
            if (typeof rule.value !== 'number' || rule.value < 0) {
                throw new Error('penaltyType=fixed 时 value 必须 ≥ 0');
            }
        }
    }
}

/**
 * 计算取消违约费
 * 匹配逻辑：找 minBefore >= minutesBeforeStart 中最小的那条规则（即最严格可适用规则）
 * 已过期（minutesBeforeStart < 0）：使用 minBefore 最小的规则
 * @param {number} minutesBeforeStart - 距离预约开始的分钟数（可为负数，表示已开始）
 * @param {Object[]} cancelRules - 对象数组格式
 * @param {number} totalAmount - 原交易金额
 * @returns {{ fee: number|null, prohibited: boolean, ruleDesc: string }}
 */
function calculateCancelFee(minutesBeforeStart, cancelRules, totalAmount) {
    if (!cancelRules || cancelRules.length === 0) return { fee: 0, prohibited: false, ruleDesc: '无违约费' };

    let matchedRule = null;

    if (minutesBeforeStart < 0) {
        let smallest = Infinity;
        for (const rule of cancelRules) {
            if (rule.minBefore < smallest) { smallest = rule.minBefore; matchedRule = rule; }
        }
    } else {
        let closest = Infinity;
        for (const rule of cancelRules) {
            if (minutesBeforeStart <= rule.minBefore && rule.minBefore < closest) {
                closest = rule.minBefore;
                matchedRule = rule;
            }
        }
    }

    if (!matchedRule) return { fee: 0, prohibited: false, ruleDesc: '无违约费' };

    if (matchedRule.penaltyType === 'prohibit') {
        return {
            fee: null,
            prohibited: true,
            ruleDesc: `距离预约开始时间${matchedRule.minBefore}分钟内禁止取消`,
        };
    }
    if (matchedRule.penaltyType === 'ratio') {
        return {
            fee: Math.round(totalAmount * matchedRule.value * 100) / 100,
            prohibited: false,
            ruleDesc: `距离预约开始时间${matchedRule.minBefore}分钟内取消，扣除${matchedRule.value * 100}%违约费`,
        };
    }
    return {
        fee: matchedRule.value,
        prohibited: false,
        ruleDesc: `距离预约开始时间${matchedRule.minBefore}分钟内取消，扣除${matchedRule.value}元违约费`,
    };
}

/**
 * 校验禁止日期列表格式
 * 支持中文星期（'周一'~'周日'）和具体日期（'YYYY-MM-DD'）
 * 过期的具体日期自动过滤（不报错）
 * @param {string[]} forbiddenDays
 * @throws {Error} 格式不合法时抛出
 */
function validateForbiddenDays(forbiddenDays) {
    if (!forbiddenDays) return;
    for (const raw of forbiddenDays) {
        const day = raw.replace(/\s+/g, '');
        if (Object.prototype.hasOwnProperty.call(WEEKDAY_MAP, day)) continue;
        if (DATE_RE.test(day)) continue;
        throw new Error(`禁止日期格式错误：${day}，应为'周一'~'周日'或'YYYY-MM-DD'`);
    }
}

/**
 * 判断某日期是否可预约
 * 支持"负负得正"逻辑：星期 + 具体日期同时命中 → 恢复可预约
 * @param {string} dateStr - 'YYYY-MM-DD'
 * @param {string[]} templateForbiddenDays - 模板禁止日期
 * @param {string[]|null} resourceForbiddenDays - 资源禁止日期（可为 null）
 * @param {0|1} customizeForbidden - 0=仅模板规则，1=模板+资源都需通过
 * @returns {boolean}
 */
function isDateReservable(dateStr, templateForbiddenDays, resourceForbiddenDays, customizeForbidden) {
    if (customizeForbidden === 1) {
        return _checkSingleList(dateStr, templateForbiddenDays) &&
               _checkSingleList(dateStr, resourceForbiddenDays);
    }
    return _checkSingleList(dateStr, templateForbiddenDays);
}

/**
 * 单列表可预约检查（flag=1不可预约，flag=2负负得正可预约）
 */
function _checkSingleList(dateStr, forbiddenDays) {
    if (!forbiddenDays || forbiddenDays.length === 0) return true;
    const date = new Date(dateStr + 'T00:00:00+08:00');
    if (isNaN(date.getTime())) return true;
    const dayOfWeek = date.getDay();
    const chineseDay = Object.keys(WEEKDAY_MAP).find(k => WEEKDAY_MAP[k] === dayOfWeek) || '';
    let flag = 0;
    if (forbiddenDays.includes(chineseDay)) flag++;
    if (forbiddenDays.includes(dateStr)) flag++;
    return flag !== 1;
}

/**
 * 校验连续预约时间范围：0 ≤ minTime ≤ maxTime ≤ 1440（分钟）
 * @param {number} minTime
 * @param {number} maxTime
 * @throws {Error}
 */
function validateReservationContinuousTime(minTime, maxTime) {
    if (minTime < 0) throw new Error('最少连续时间必须≥0');
    if (maxTime > 1440) throw new Error('最大连续时间必须≤1440');
    if (minTime > maxTime) throw new Error('最少连续时间不能大于最大连续时间');
}

module.exports = {
    validatePhone,
    validatePassword,
    validateTimeSlots,
    validateCancelRules,
    calculateCancelFee,
    validateForbiddenDays,
    isDateReservable,
    validateReservationContinuousTime,
};
