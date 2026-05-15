/**
 * utils/storage-utils.js
 * 微信小程序本地存储工具（基于 wx.*Storage API 的轻封装）
 *
 * 功能特性：
 * - 异步：set/get/remove/clear（Promise，支持 async/await）
 * - 同步：setSync/getSync/removeSync/clearSync
 * - 自动序列化：存任意类型（string/number/boolean/object/Date...）自动 JSON.stringify
 * - 自动反序列化：读取自动 JSON.parse
 * - 过期时间：支持 expire（秒），读取时过期自动删除并返回 null；默认 30 天
 * - 校验：key（含前缀）长度 ≤ 16；value（序列化后）大小 ≤ 1MB
 * - 统一错误处理：wx.showToast 友好提示 + console.error 详细日志；失败返回 false/null
 *
 * 说明：
 * - 默认使用毫秒时间戳保存 expireMs。
 * - Date 类型会在 JSON 序列化后变为字符串（ISO 格式），读取时即为字符串；如需还原 Date，请在业务侧自行转换。
 * - 默认 key 前缀为 'app:'，可通过 setPrefix 修改。校验长度按 (prefix + key) 计算。
 */

const DEFAULT_PREFIX = 'ec:';
const MAX_KEY_LEN = 64; // 含前缀
const MAX_BYTES = 1024 * 1024; // 1MB
const DEFAULT_EXPIRE_SECONDS = 30 * 24 * 60 * 60; // 30 天

let _prefix = DEFAULT_PREFIX;

/**
 * 设置全局 key 前缀（影响后续调用）
 * @param {string} prefix
 */
function setPrefix(prefix) {
	if (typeof prefix !== 'string') return;
	_prefix = prefix;
}

/**
 * UTF-8 字节长度计算
 * @param {string} str
 * @returns {number}
 */
function byteLengthUTF8(str) {
	let bytes = 0;
	for (let i = 0; i < str.length; i++) {
		const codePoint = str.charCodeAt(i);
		if (codePoint <= 0x7f) {
			bytes += 1;
		} else if (codePoint <= 0x7ff) {
			bytes += 2;
		} else if (codePoint >= 0xd800 && codePoint <= 0xdbff) {
			// 代理对，高位代理 + 低位代理
			i++;
			bytes += 4;
		} else {
			bytes += 3;
		}
	}
	return bytes;
}

/**
 * 安全 JSON 序列化
 * @param {any} data
 * @returns {string}
 */
function safeStringify(data) {
	try {
		return JSON.stringify(data);
	} catch (err) {
		return '';
	}
}

/**
 * 反序列化（失败返回 null）
 * @param {string} str
 * @returns {any}
 */
function safeParse(str) {
	try {
		return JSON.parse(str);
	} catch (err) {
		return null;
	}
}

/**
 * 组装带前缀的 key
 * @param {string} key
 * @returns {string}
 */
function withPrefix(key) {
	return `${_prefix || ''}${key}`;
}

/**
 * 校验 key 长度（含前缀）
 * @param {string} key
 * @returns {true|string} 通过返回 true，否则返回错误消息
 */
function validateKey(key) {
	if (!key || typeof key !== 'string') return '无效的 key';
	const fullKey = withPrefix(key);
	if (fullKey.length > MAX_KEY_LEN) return `key 过长（含前缀≤${MAX_KEY_LEN}）`;
	return true;
}

/**
 * 校验序列化后大小（≤1MB）
 * @param {string} serialized
 * @returns {true|string}
 */
function validateSize(serialized) {
	if (typeof serialized !== 'string') return '序列化失败';
	const size = byteLengthUTF8(serialized);
	if (size > MAX_BYTES) return '数据过大（上限 1MB）';
	return true;
}

/**
 * 统一错误提示
 * @param {string} toastMsg
 * @param {any} err
 */
function handleError(toastMsg, err) {
	try {
		wx.showToast({ title: toastMsg || '操作失败，请稍后重试', icon: 'none' });
	} catch (_) {
		// 忽略环境下无 wx 的异常
	}
	// 详细日志
	/* eslint-disable no-console */
	console.error('[storage-utils]', toastMsg, err);
}

/**
 * 打包存储对象：{ value, expireMs }
 * @param {any} value
 * @param {number|undefined} expireSeconds
 */
function wrapValue(value, expireSeconds) {
	const expireSec = typeof expireSeconds === 'number' && expireSeconds > 0
		? expireSeconds
		: DEFAULT_EXPIRE_SECONDS;
	const expireMs = Date.now() + expireSec * 1000;
	return { value, expireMs };
}

/**
 * 检查是否过期；过期返回 true
 * @param {{ expireMs: number }} wrapped
 */
function isExpired(wrapped) {
	if (!wrapped || typeof wrapped.expireMs !== 'number') return false;
	return Date.now() > wrapped.expireMs;
}

/**
 * 异步 set
 * @param {string} key
 * @param {any} value
 * @param {{ expire?: number }} [options] expire 单位：秒；不传默认 30 天
 * @returns {Promise<boolean>} 成功 true，失败 false
 */
function set(key, value, options) {
	return new Promise((resolve) => {
		const keyCheck = validateKey(key);
		if (keyCheck !== true) {
			handleError(keyCheck, null);
			return resolve(false);
		}

		const wrapped = wrapValue(value, options && options.expire);
		const serialized = safeStringify(wrapped);
		const sizeCheck = validateSize(serialized);
		if (sizeCheck !== true) {
			handleError(sizeCheck, null);
			return resolve(false);
		}

		try {
			wx.setStorage({
				key: withPrefix(key),
				data: serialized,
				success: () => resolve(true),
				fail: (err) => {
					handleError('存储失败，请稍后重试', err);
					resolve(false);
				},
			});
		} catch (err) {
			handleError('存储失败，请稍后重试', err);
			resolve(false);
		}
	});
}

/**
 * 异步 get
 * @param {string} key
 * @returns {Promise<any|null>} 正常返回存储的原始值；过期/不存在时返回 null
 */
function get(key) {
	return new Promise((resolve) => {
		const keyCheck = validateKey(key);
		if (keyCheck !== true) {
			handleError(keyCheck, null);
			return resolve(null);
		}
		try {
			wx.getStorage({
				key: withPrefix(key),
				success: (res) => {
					const parsed = typeof res.data === 'string' ? safeParse(res.data) : res.data;
					if (!parsed || typeof parsed !== 'object') {
						// 非包装格式，直接返回（兼容历史数据）
						return resolve(parsed ?? null);
					}
					if (isExpired(parsed)) {
						// 过期即删除
						try { wx.removeStorage({ key: withPrefix(key) }); } catch (_) {}
						return resolve(null);
					}
					return resolve(parsed.value);
				},
				fail: () => resolve(null), // 不存在时也会走 fail
			});
		} catch (err) {
			handleError('读取失败，请稍后重试', err);
			resolve(null);
		}
	});
}

/**
 * 异步 remove
 * @param {string} key
 * @returns {Promise<boolean>}
 */
function remove(key) {
	return new Promise((resolve) => {
		const keyCheck = validateKey(key);
		if (keyCheck !== true) {
			handleError(keyCheck, null);
			return resolve(false);
		}
		try {
			wx.removeStorage({
				key: withPrefix(key),
				success: () => resolve(true),
				fail: (err) => {
					handleError('删除失败，请稍后重试', err);
					resolve(false);
				},
			});
		} catch (err) {
			handleError('删除失败，请稍后重试', err);
			resolve(false);
		}
	});
}

/**
 * 异步 clear（清空所有存储）
 * @returns {Promise<boolean>}
 */
function clear() {
	return new Promise((resolve) => {
		try {
			wx.clearStorage({
				success: () => resolve(true),
				fail: (err) => {
					handleError('清空失败，请稍后重试', err);
					resolve(false);
				},
			});
		} catch (err) {
			handleError('清空失败，请稍后重试', err);
			resolve(false);
		}
	});
}

/**
 * 同步 set
 * @param {string} key
 * @param {any} value
 * @param {{ expire?: number }} [options]
 * @returns {boolean}
 */
function setSync(key, value, options) {
	const keyCheck = validateKey(key);
	if (keyCheck !== true) {
		handleError(keyCheck, null);
		return false;
	}
	const wrapped = wrapValue(value, options && options.expire);
	const serialized = safeStringify(wrapped);
	const sizeCheck = validateSize(serialized);
	if (sizeCheck !== true) {
		handleError(sizeCheck, null);
		return false;
	}
	try {
		wx.setStorageSync(withPrefix(key), serialized);
		return true;
	} catch (err) {
		handleError('存储失败，请稍后重试', err);
		return false;
	}
}

/**
 * 同步 get
 * @param {string} key
 * @returns {any|null}
 */
function getSync(key) {
	const keyCheck = validateKey(key);
	if (keyCheck !== true) {
		handleError(keyCheck, null);
		return null;
	}
	try {
		const data = wx.getStorageSync(withPrefix(key));
		if (data == null || data === '') return null;
		const parsed = typeof data === 'string' ? safeParse(data) : data;
		if (!parsed || typeof parsed !== 'object') {
			return parsed ?? null;
		}
		if (isExpired(parsed)) {
			try { wx.removeStorageSync(withPrefix(key)); } catch (_) {}
			return null;
		}
		return parsed.value;
	} catch (err) {
		handleError('读取失败，请稍后重试', err);
		return null;
	}
}

/**
 * 同步 remove
 * @param {string} key
 * @returns {boolean}
 */
function removeSync(key) {
    console.log(key)
	const keyCheck = validateKey(key);
	if (keyCheck !== true) {
		handleError(keyCheck, null);
		return false;
	}
	try {
		wx.removeStorageSync(withPrefix(key));
		return true;
	} catch (err) {
		handleError('删除失败，请稍后重试', err);
		return false;
	}
}

/**
 * 同步 clear
 * @returns {boolean}
 */
function clearSync() {
	try {
		wx.clearStorageSync();
		return true;
	} catch (err) {
		handleError('清空失败，请稍后重试', err);
		return false;
	}
}

/**
 * 获取与设置当前前缀（可用于隔离不同业务域）
 */
function getPrefix() {
	return _prefix;
}

const storageUtils = {
	setPrefix,
	getPrefix,
	set,
	get,
	remove,
	clear,
	setSync,
	getSync,
	removeSync,
	clearSync,
	// 暴露工具仅供必要时使用
	__utils: {
		withPrefix,
		validateKey,
		validateSize,
	},
};

// 支持 ES6 export 和 CommonJS module.exports
module.exports = {
	storageUtils,
	// 也支持直接导出方法
	setPrefix,
	getPrefix,
	set,
	get,
	remove,
	clear,
	setSync,
	getSync,
	removeSync,
	clearSync,
};

// ES6 export（如果支持）
if (typeof exports !== 'undefined' && typeof module !== 'undefined' && module.exports) {
	// CommonJS 环境，已通过 module.exports 导出
}

/**
 * 使用示例：
 *
 * import { storageUtils } from '../../utils/storage-utils';
 *
 * // 可选：设置前缀，避免 key 冲突（注意总长度≤16）
 * storageUtils.setPrefix('ec:'); // 默认 'app:'
 *
 * // 异步存储（默认 30 天过期）
 * await storageUtils.set('token', 'abcdefg');
 *
 * // 异步存储（自定义过期：2小时）
 * await storageUtils.set('profile', { id: 1, name: 'Lee' }, { expire: 2 * 60 * 60 });
 *
 * // 异步读取
 * const token = await storageUtils.get('token'); // 过期/不存在 → null
 *
 * // 异步删除
 * await storageUtils.remove('token');
 *
 * // 异步清空
 * await storageUtils.clear();
 *
 * // 同步存储（有些场景如 onLaunch/init 对性能有要求且逻辑简单）
 * storageUtils.setSync('bootTime', Date.now(), { expire: 24 * 60 * 60 });
 * const bootTime = storageUtils.getSync('bootTime'); // number 或 null
 * storageUtils.removeSync('bootTime');
 * storageUtils.clearSync();
 *
 * // 注意事项：
 * // - value 序列化后需 ≤ 1MB，否则会失败并提示。
 * // - Date 存储后读取为字符串（ISO），如需 Date 类型请自行 new Date(value) 转换。
 * // - key（含前缀）长度 ≤ 16，超出会提示错误。
 */


