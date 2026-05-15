/**
 * utils/time-util.js
 * 时间戳与日期处理工具（微信小程序/通用 JS）
 *
 * 功能：
 * ① 时间戳 → 年月日/时分秒/年月日时分秒（默认用 - 连接，示例：2025-11-10 12-03-05）
 * ② 获取当前时间戳（毫秒）
 * ③ 获取 X 天后的时间戳（基于当前或指定时间戳）
 * ④ 年月日时分秒（字符串） → 时间戳（毫秒）
 *
 * 设计说明：
 * - 工具默认使用毫秒级时间戳（与 Date.now() 一致）。
 * - 输入时间戳若为 10 位（秒），会自动转为毫秒处理。
 * - 字符串解析尽量兼容常见格式（-、/、:、空格），也支持仅日期或仅时间（日期缺省为当天零点）。
 */

/**
 * 内部：将可能是秒的时间戳转为毫秒
 * @param {number} ts
 * @returns {number}
 */
function normalizeToMs(ts) {
	if (typeof ts !== 'number') return NaN;
	// 10位认为是秒，转换为毫秒
	if (ts < 1e12) return ts * 1000;
	return ts;
}

/**
 * 内部：将数字补零到两位
 * @param {number} n
 * @returns {string}
 */
function pad2(n) {
	return n < 10 ? '0' + n : String(n);
}

/**
 * ① 时间戳格式化
 * @param {number} timestamp 时间戳（毫秒或秒；秒会自动转换）
 * @param {'date'|'time'|'datetime'} mode 返回模式
 * - 'date' 返回：YYYY-MM-DD（默认 - 连接）
 * - 'time' 返回：HH-mm-ss（默认 - 连接）
 * - 'datetime' 返回：YYYY-MM-DD HH-mm-ss（默认 - 连接）
 * @param {string} sep 分隔符（默认 '-'，同时用于日期与时间分隔）
 * @returns {string}
 */
function formatTimestamp(timestamp, mode = 'datetime', sep = '-') {
	const ms = normalizeToMs(Number(timestamp));
	if (!isFinite(ms)) return '';
	const d = new Date(ms);
	if (isNaN(d.getTime())) return '';

	const yyyy = d.getFullYear();
	const MM = pad2(d.getMonth() + 1);
	const dd = pad2(d.getDate());
	const HH = pad2(d.getHours());
	const mm = pad2(d.getMinutes());
	const ss = pad2(d.getSeconds());

	const dateStr = [yyyy, MM, dd].join(sep);
	const timeStr = [HH, mm, ss].join(sep);

	if (mode === 'date') return dateStr;
	if (mode === 'time') return timeStr;
	return dateStr + ' ' + timeStr;
}

/**
 * ② 当前时间戳（毫秒）
 * @returns {number}
 */
function nowTimestamp() {
	return Date.now();
}

/**
 * ⑤ 获取当前时间（统一入口，方便后续切换为网络时间）
 * TODO: 后续实现网络时间获取，替换系统时间
 * @returns {{timestamp: number, dateTimeStr: string, dateStr: string, timeStr: string}}
 * - timestamp: 毫秒时间戳
 * - dateTimeStr: "YYYY-MM-DD HH:MM:SS"
 * - dateStr: "YYYY-MM-DD"
 * - timeStr: "HH:MM:SS"
 */
function getCurrentTime() {
	// TODO: 替换为网络时间获取
	// 目前暂用系统时间
	const now = new Date();
	const timestamp = now.getTime();
	
	const yyyy = now.getFullYear();
	const MM = pad2(now.getMonth() + 1);
	const dd = pad2(now.getDate());
	const HH = pad2(now.getHours());
	const mm = pad2(now.getMinutes());
	const ss = pad2(now.getSeconds());
	
	const dateStr = `${yyyy}-${MM}-${dd}`;
	const timeStr = `${HH}:${mm}:${ss}`;
    const dateTimeStr = `${dateStr} ${timeStr}`;
    const res = {
		timestamp,
		dateTimeStr,
		dateStr,
		timeStr
	};
    console.log(res)
	return res;
}

/**
 * ③ 获取 X 天后的时间戳（毫秒）
 * @param {number} days 正数为将来，负数为过去
 * @param {number} [baseTimestamp] 基准时间戳（毫秒或秒），默认当前时间
 * @returns {number}
 */
function timestampAfterDays(days, baseTimestamp) {
	const base = baseTimestamp == null ? Date.now() : normalizeToMs(Number(baseTimestamp));
	const delta = Number(days) * 24 * 60 * 60 * 1000;
	return base + delta;
}

/**
 * 解析常见日期时间字符串为 Date 对象
 * 支持：
 * - 'YYYY-MM-DD'
 * - 'YYYY/MM/DD'
 * - 'YYYY-MM-DD HH:mm:ss'
 * - 'YYYY/MM/DD HH:mm:ss'
 * - 'YYYY-MM-DD HH-mm-ss'（时间也用 - 分隔）
 * - 'HH:mm:ss'（仅时间，日期默认当天）
 * - 'HH-mm-ss'（仅时间，日期默认当天）
 * @param {string} input
 * @returns {Date|null}
 */
function parseToDate(input) {
	if (!input || typeof input !== 'string') return null;
	const raw = input.trim();
	if (!raw) return null;

	// 拆分日期与时间
	let datePart = '';
	let timePart = '';
	if (raw.includes(' ')) {
		const [d, t] = raw.split(/\s+/, 2);
		datePart = d;
		timePart = t;
	} else if (/\d{1,2}[:\-]\d{1,2}/.test(raw)) {
		// 类似 '12:03:05' 或 '12-03-05'，仅时间
		timePart = raw;
	} else {
		// 仅日期
		datePart = raw;
	}

	// 解析日期
	let y, M, d;
	if (datePart) {
		const dateSeg = datePart.split(/[\/\-]/);
		if (dateSeg.length >= 3) {
			y = Number(dateSeg[0]);
			M = Number(dateSeg[1]);
			d = Number(dateSeg[2]);
		}
	}

	// 默认为今天
	const today = new Date();
	if (!y || !M || !d) {
		y = y || today.getFullYear();
		M = M || (today.getMonth() + 1);
		d = d || today.getDate();
	}

	// 解析时间
	let H = 0, m = 0, s = 0;
	if (timePart) {
		const timeSeg = timePart.split(/[:\-]/);
		if (timeSeg[0] != null) H = Number(timeSeg[0]) || 0;
		if (timeSeg[1] != null) m = Number(timeSeg[1]) || 0;
		if (timeSeg[2] != null) s = Number(timeSeg[2]) || 0;
	}

	// 构建日期（月份从0开始）
	const result = new Date(y, (M - 1), d, H, m, s, 0);
	// 校验生成是否有效（避免 2025-13-40 这类）
	if (isNaN(result.getTime())) return null;
	if (result.getFullYear() !== y || (result.getMonth() + 1) !== M || result.getDate() !== d) {
		return null;
	}
	return result;
}

/**
 * ④ 年月日时分秒 → 时间戳（毫秒）
 * 接受常见格式（参考 parseToDate）
 * @param {string} input
 * @returns {number} 毫秒时间戳；无法解析时返回 NaN
 */
function parseToTimestamp(input) {
	const d = parseToDate(input);
	return d ? d.getTime() : NaN;
}

const timeUtil = {
	formatTimestamp,
	nowTimestamp,
	getCurrentTime,
	timestampAfterDays,
	parseToTimestamp,
};

module.exports = {
	timeUtil,
	formatTimestamp,
	nowTimestamp,
	getCurrentTime,
	timestampAfterDays,
	parseToTimestamp,
};

/**
 * 使用示例：
 *
 * const { timeUtil } = require('../../utils/time-util.js');
 *
 * // ① 时间戳转字符串
 * const ts = 1768046400000; // 举例
 * const d1 = timeUtil.formatTimestamp(ts, 'date');      // '2025-11-10'
 * const d2 = timeUtil.formatTimestamp(ts, 'time');      // '12-03-05'
 * const d3 = timeUtil.formatTimestamp(ts, 'datetime');  // '2025-11-10 12-03-05'
 *
 * // ② 当前时间戳
 * const now = timeUtil.nowTimestamp(); // 毫秒
 *
 * // ③ X 天后时间戳
 * const tsAfter3 = timeUtil.timestampAfterDays(3); // 3天后（基于当前）
 * const tsAfter5FromBase = timeUtil.timestampAfterDays(5, ts); // 基于 ts 的 5 天后
 *
 * // ④ 字符串转时间戳
 * const t1 = timeUtil.parseToTimestamp('2025-11-10');               // 当天 00:00:00
 * const t2 = timeUtil.parseToTimestamp('2025-11-10 08:30:00');
 * const t3 = timeUtil.parseToTimestamp('2025/11/10 08-30-00');
 * const t4 = timeUtil.parseToTimestamp('08:30:00');                 // 默认为今天 08:30:00
 */


