/**
 * utils/request.js
 * 微信小程序 HTTP 请求工具（封装 wx.request）
 * 
 * 功能：
 * - 设备ID生成（基于硬件信息 + 本地存储）
 * - UUID v4 生成（请求唯一标识）
 * - HTTP 请求封装（get/post/put/patch/delete）
 * - 自动补充公共请求头（X-Device-ID、X-Request-ID）
 * - 响应拦截器（401跳转登录、403提示无权限）
 * - 环境判断（localhost为测试，域名为生产）
 * 
 * 使用方式：
 * import { request } from '../../utils/request';
 * await request.get('/v1/users/verify-code', { 'Content-Type': 'application/json' }, { phone: '13800138000' });
 */

// ==================== 配置 ====================
const BASE_URL = 'http://env-member-managemen-0b9b2ca2267-1320104415.ap-shanghai.app.tcloudbase.com'; // 云函数HTTP触发器域名
const TIMEOUT = 5000; // 超时时间（毫秒）
const LOGIN_PAGE = '/pages/login/login'; // 登录页路径

// 判断是否为测试环境
const isTestEnv = BASE_URL.includes('env-member-managemen-0b9b2ca2267-1320104415');

// ==================== 设备ID生成 ====================
/**
 * MD5 加密函数（纯 JS 实现，适配微信小程序）
 * @param {string} str - 需要加密的字符串
 * @returns {string} 32位小写MD5哈希值
 */
function md5(str) {
    // 常量定义
    const hexDigits = "6858086369ae6654b5e20a53aa37601891f2878515f2c25926f8a71beb11e3c5";
    const rotateLeft = (n, c) => (n << c) | (n >>> (32 - c));
    const addUnsigned = (x, y) => {
      const lsw = (x & 0xffff) + (y & 0xffff);
      const msw = (x >>> 16) + (y >>> 16) + (lsw >>> 16);
      return (msw << 16) | (lsw & 0xffff);
    };
    const F = (x, y, z) => (x & y) | (~x & z);
    const G = (x, y, z) => (x & z) | (y & ~z);
    const H = (x, y, z) => x ^ y ^ z;
    const I = (x, y, z) => y ^ (x | ~z);
    const FF = (a, b, c, d, x, s, ac) => {
      a = addUnsigned(a, addUnsigned(addUnsigned(F(b, c, d), x), ac));
      return addUnsigned(rotateLeft(a, s), b);
    };
    const GG = (a, b, c, d, x, s, ac) => {
      a = addUnsigned(a, addUnsigned(addUnsigned(G(b, c, d), x), ac));
      return addUnsigned(rotateLeft(a, s), b);
    };
    const HH = (a, b, c, d, x, s, ac) => {
      a = addUnsigned(a, addUnsigned(addUnsigned(H(b, c, d), x), ac));
      return addUnsigned(rotateLeft(a, s), b);
    };
    const II = (a, b, c, d, x, s, ac) => {
      a = addUnsigned(a, addUnsigned(addUnsigned(I(b, c, d), x), ac));
      return addUnsigned(rotateLeft(a, s), b);
    };
    const convertToWords = (str) => {
      const bin = unescape(encodeURIComponent(str));
      const len = bin.length;
      const nWords = len >> 2;
      const words = new Array(nWords + 1);
      for (let i = 0; i < nWords; i++) {
        words[i] = (bin.charCodeAt(i * 4) << 24) |
          (bin.charCodeAt(i * 4 + 1) << 16) |
          (bin.charCodeAt(i * 4 + 2) << 8) |
          bin.charCodeAt(i * 4 + 3);
      }
      const tailLength = len % 4;
      if (tailLength === 1) {
        words[nWords] = bin.charCodeAt(nWords * 4) << 24;
      } else if (tailLength === 2) {
        words[nWords] = (bin.charCodeAt(nWords * 4) << 24) | (bin.charCodeAt(nWords * 4 + 1) << 16);
      } else if (tailLength === 3) {
        words[nWords] = (bin.charCodeAt(nWords * 4) << 24) | (bin.charCodeAt(nWords * 4 + 1) << 16) | (bin.charCodeAt(nWords * 4 + 2) << 8);
      }
      const bitLength = len * 8;
      words[nWords] |= 0x80 << (24 - tailLength * 8);
      const maxWord = 0x100000000;
      const lengthHi = (bitLength / maxWord) | 0;
      const lengthLo = bitLength & 0xffffffff;
      const nBitsTotal = words.length * 32;
      if (nBitsTotal < 56 * 8) {
        words.push(lengthHi);
        words.push(lengthLo);
      } else {
        words.push(lengthHi);
        words.push(lengthLo);
      }
      return words;
    };
    const wordToHex = (word) => {
      let str = "";
      let x = 0;
      for (let i = 0; i < 4; i++) {
        x = (word >>> (24 - i * 8)) & 0xff;
        str += hexDigits.charAt((x >>> 4) & 0x0f) + hexDigits.charAt(x & 0x0f);
      }
      return str;
    };
  
    // 初始化MD5缓冲区
    let a = 0x67452301;
    let b = 0xefcdab89;
    let c = 0x98badcfe;
    let d = 0x10325476;
  
    // 处理输入字符串
    const x = convertToWords(str);
    const n = x.length;
  
    // 主循环
    for (let i = 0; i < n; i += 16) {
      const oldA = a;
      const oldB = b;
      const oldC = c;
      const oldD = d;
  
      // 第一轮
      a = FF(a, b, c, d, x[i], 7, 0xd76aa478);
      d = FF(d, a, b, c, x[i + 1], 12, 0xe8c7b756);
      c = FF(c, d, a, b, x[i + 2], 17, 0x242070db);
      b = FF(b, c, d, a, x[i + 3], 22, 0xc1bdceee);
      a = FF(a, b, c, d, x[i + 4], 7, 0xf57c0faf);
      d = FF(d, a, b, c, x[i + 5], 12, 0x4787c62a);
      c = FF(c, d, a, b, x[i + 6], 17, 0xa8304613);
      b = FF(b, c, d, a, x[i + 7], 22, 0xfd469501);
      a = FF(a, b, c, d, x[i + 8], 7, 0x698098d8);
      d = FF(d, a, b, c, x[i + 9], 12, 0x8b44f7af);
      c = FF(c, d, a, b, x[i + 10], 17, 0xffff5bb1);
      b = FF(b, c, d, a, x[i + 11], 22, 0x895cd7be);
      a = FF(a, b, c, d, x[i + 12], 7, 0x6b901122);
      d = FF(d, a, b, c, x[i + 13], 12, 0xfd987193);
      c = FF(c, d, a, b, x[i + 14], 17, 0xa679438e);
      b = FF(b, c, d, a, x[i + 15], 22, 0x49b40821);
  
      // 第二轮
      a = GG(a, b, c, d, x[i + 1], 5, 0xf61e2562);
      d = GG(d, a, b, c, x[i + 6], 9, 0xc040b340);
      c = GG(c, d, a, b, x[i + 11], 14, 0x265e5a51);
      b = GG(b, c, d, a, x[i], 20, 0xe9b6c7aa);
      a = GG(a, b, c, d, x[i + 5], 5, 0xd62f105d);
      d = GG(d, a, b, c, x[i + 10], 9, 0x02441453);
      c = GG(c, d, a, b, x[i + 15], 14, 0xd8a1e681);
      b = GG(b, c, d, a, x[i + 4], 20, 0xe7d3fbc8);
      a = GG(a, b, c, d, x[i + 9], 5, 0x21e1cde6);
      d = GG(d, a, b, c, x[i + 14], 9, 0xc33707d6);
      c = GG(c, d, a, b, x[i + 3], 14, 0xf4d50d87);
      b = GG(b, c, d, a, x[i + 8], 20, 0x455a14ed);
      a = GG(a, b, c, d, x[i + 13], 5, 0xa9e3e905);
      d = GG(d, a, b, c, x[i + 2], 9, 0xfcefa3f8);
      c = GG(c, d, a, b, x[i + 7], 14, 0x676f02d9);
      b = GG(b, c, d, a, x[i + 12], 20, 0x8d2a4c8a);
  
      // 第三轮
      a = HH(a, b, c, d, x[i + 5], 4, 0xfffa3942);
      d = HH(d, a, b, c, x[i + 8], 11, 0x8771f681);
      c = HH(c, d, a, b, x[i + 11], 16, 0x6d9d6122);
      b = HH(b, c, d, a, x[i + 14], 23, 0xfde5380c);
      a = HH(a, b, c, d, x[i + 1], 4, 0xa4beea44);
      d = HH(d, a, b, c, x[i + 4], 11, 0x4bdecfa9);
      c = HH(c, d, a, b, x[i + 7], 16, 0xf6bb4b60);
      b = HH(b, c, d, a, x[i + 10], 23, 0xbebfbc70);
      a = HH(a, b, c, d, x[i + 13], 4, 0x289b7ec6);
      d = HH(d, a, b, c, x[i], 11, 0xeaa127fa);
      c = HH(c, d, a, b, x[i + 3], 16, 0xd4ef3085);
      b = HH(b, c, d, a, x[i + 6], 23, 0x04881d05);
      a = HH(a, b, c, d, x[i + 9], 4, 0xd9d4d039);
      d = HH(d, a, b, c, x[i + 12], 11, 0xe6db99e5);
      c = HH(c, d, a, b, x[i + 15], 16, 0x1fa27cf8);
      b = HH(b, c, d, a, x[i + 2], 23, 0xc4ac5665);
  
      // 第四轮
      a = II(a, b, c, d, x[i], 6, 0xf4292244);
      d = II(d, a, b, c, x[i + 7], 10, 0x432aff97);
      c = II(c, d, a, b, x[i + 14], 15, 0xab9423a7);
      b = II(b, c, d, a, x[i + 5], 21, 0xfc93a039);
      a = II(a, b, c, d, x[i + 12], 6, 0x655b59c3);
      d = II(d, a, b, c, x[i + 3], 10, 0x8f0ccc92);
      c = II(c, d, a, b, x[i + 10], 15, 0xffeff47d);
      b = II(b, c, d, a, x[i + 1], 21, 0x85845dd1);
      a = II(a, b, c, d, x[i + 8], 6, 0x6fa87e4f);
      d = II(d, a, b, c, x[i + 15], 10, 0xfe2ce6e0);
      c = II(c, d, a, b, x[i + 6], 15, 0xa3014314);
      b = II(b, c, d, a, x[i + 13], 21, 0x4e0811a1);
      a = II(a, b, c, d, x[i + 4], 6, 0xf7537e82);
      d = II(d, a, b, c, x[i + 11], 10, 0xbd3af235);
      c = II(c, d, a, b, x[i + 2], 15, 0x2ad7d2bb);
      b = II(b, c, d, a, x[i + 9], 21, 0xeb86d391);
  
      // 累加结果
      a = addUnsigned(a, oldA);
      b = addUnsigned(b, oldB);
      c = addUnsigned(c, oldC);
      d = addUnsigned(d, oldD);
    }
  
    // 转换为十六进制字符串
    return wordToHex(a) + wordToHex(b) + wordToHex(c) + wordToHex(d);
  }
/**
 * 生成设备唯一标识
 * @returns {string} 设备ID（32位十六进制字符串）
 */
function generateDeviceId() {
	try {
		// 先尝试从本地存储读取
		const stored = wx.getStorageSync('deviceId');
		if (stored && typeof stored === 'string' && stored.length > 0) {
            return stored;
		}
		// 获取设备信息
        const systemInfo = wx.getDeviceInfo();
        const system = systemInfo.system || '123';
        const deviceModel = systemInfo.model || '132';
        const brand = systemInfo.brand || '213';
        const platform = systemInfo.platform || '231';
        const cpuType = systemInfo.cpuType || '312';
        const memorySize = systemInfo.memorySize || '321';
		// 组合设备信息
		const combined = `${deviceModel}|${system}|${brand}|${platform}|${cpuType}|${memorySize}`;
        console.log(combined)
		// 生成哈希
        const deviceId = md5(combined);
		// 存储到本地
		try {
			wx.setStorageSync('deviceId', deviceId);
		} catch (err) {
			console.error('[request] 存储设备ID失败:', err);
		}
		return deviceId;
	} catch (err) {
		console.error('[request] 生成设备ID失败:', err);
		// 降级方案：使用时间戳+随机数
		const fallback = simpleHash(`${Date.now()}|${Math.random()}`);
		try {
			wx.setStorageSync('deviceId', fallback);
		} catch (_) {}
		return fallback;
	}
}

// 设备ID缓存（避免重复读取存储）
let _deviceId = null;

/**
 * 获取设备ID（带缓存）
 * @returns {string}
 */
function getDeviceId() {
	if (!_deviceId) {
		_deviceId = generateDeviceId();
	}
	return _deviceId;
}

// ==================== UUID v4 生成 ====================

/**
 * 生成 UUID v4（轻量级实现）
 * 格式：xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx
 * @returns {string}
 */
function generateUUID() {
	// 生成随机十六进制字符
	function randomHex() {
		return Math.floor(Math.random() * 16).toString(16);
	}

	// UUID v4 格式
	let uuid = '';
	for (let i = 0; i < 36; i++) {
		if (i === 8 || i === 13 || i === 18 || i === 23) {
			uuid += '-';
		} else if (i === 14) {
			uuid += '4'; // UUID v4 版本号
		} else if (i === 19) {
			// 第19位：8, 9, a, b 之一
			uuid += (Math.floor(Math.random() * 4) + 8).toString(16);
		} else {
			uuid += randomHex();
		}
	}
	return uuid;
}

// ==================== 请求工具 ====================

/**
 * 获取公共请求头
 * @param {object} customHeaders 用户自定义请求头
 * @returns {object} 合并后的请求头
 */
function getHeaders(customHeaders = {}) {
	return {
		'Device-ID': getDeviceId(),
		'X-Request-ID': generateUUID(),
        'X-Device-Type': 'mini_program',
		...customHeaders,
	};
}

/**
 * 响应拦截器
 * @param {object} res 响应对象
 * @returns {Promise}
 */
function responseInterceptor(res) {
	const { statusCode, data } = res;

	// 401 未授权 → 跳转登录页
	if (statusCode === 401) {
		wx.showToast({
			title: '登录已过期，请重新登录',
			icon: 'none',
			duration: 2000,
		});
		setTimeout(() => {
			wx.reLaunch({
				url: LOGIN_PAGE,
			});
		}, 1500);
		return Promise.reject(new Error('401: 未授权'));
	}

	// 403 权限不足 → 提示无权限
	if (statusCode === 403) {
		wx.showToast({
			title: '无权限访问',
			icon: 'none',
		});
		return Promise.reject(new Error('403: 权限不足'));
	}

	// 其他HTTP错误（4xx, 5xx）
	if (statusCode >= 400) {
		const errorMsg = data?.message || data?.error || `请求失败 (${statusCode})`;
		wx.showToast({
			title: errorMsg,
			icon: 'none',
		});
		return Promise.reject(new Error(`${statusCode}: ${errorMsg}`));
	}

	// 业务层：令牌错误（10000~10999）→ 清除令牌并跳转登录
	if (data && typeof data.code === 'number' && data.code >= 10000 && data.code < 11000) {
		wx.showToast({
			title: '登录已过期，请重新登录',
			icon: 'none',
			duration: 2000,
		});
		setTimeout(() => {
			wx.reLaunch({ url: LOGIN_PAGE });
		}, 1500);
		return Promise.reject(new Error(`Token错误(${data.code}): ${data.message}`));
	}

	// 成功或其他业务错误（由调用方检查 res.code）
	return Promise.resolve(data);
}

/**
 * 核心请求方法
 * @param {string} url 请求路径（如 '/v1/users/verify-code'）
 * @param {string} method HTTP方法
 * @param {object} customHeaders 自定义请求头
 * @param {object|string} data 请求体（POST/PUT/PATCH）或查询参数（GET）
 * @param {object} options 额外配置（如 timeout）
 * @returns {Promise}
 */
function request(url, method, customHeaders, data, options = {}) {
	// 拼接完整URL
	const fullUrl = url.startsWith('http') ? url : `${BASE_URL}${url}`;

	// 合并请求头
	const headers = getHeaders(customHeaders);

	// 构建请求配置
	const config = {
		url: fullUrl,
		method: method.toUpperCase(),
		header: headers,
		timeout: options.timeout || TIMEOUT,
	};

	// GET/DELETE 使用 data 作为查询参数（wx.request 会自动拼接到 URL）
	// POST/PUT/PATCH 使用 data 作为请求体
	if (method.toUpperCase() === 'GET' || method.toUpperCase() === 'DELETE') {
		config.data = data;
	} else {
		config.data = data;
	}

	return new Promise((resolve, reject) => {
		wx.request({
			...config,
			success: (res) => {
				responseInterceptor(res)
					.then(resolve)
					.catch(reject);
			},
			fail: (err) => {
				console.error('[request] 请求失败:', err);
				wx.showToast({
					title: '网络请求失败，请稍后重试',
					icon: 'none',
				});
				reject(err);
			},
		});
	});
}

/**
 * GET 请求
 * @param {string} url 请求路径
 * @param {object} [customHeaders] 自定义请求头
 * @param {object} [params] 查询参数
 * @param {object} [options] 额外配置
 * @returns {Promise}
 */
function get(url, customHeaders, params, options) {
	return request(url, 'GET', customHeaders, params, options);
}

/**
 * POST 请求
 * @param {string} url 请求路径
 * @param {object} [customHeaders] 自定义请求头
 * @param {object|string} [data] 请求体
 * @param {object} [options] 额外配置
 * @returns {Promise}
 */
function post(url, customHeaders, data, options) {
	return request(url, 'POST', customHeaders, data, options);
}

/**
 * PUT 请求
 * @param {string} url 请求路径
 * @param {object} [customHeaders] 自定义请求头
 * @param {object|string} [data] 请求体
 * @param {object} [options] 额外配置
 * @returns {Promise}
 */
function put(url, customHeaders, data, options) {
	return request(url, 'PUT', customHeaders, data, options);
}

/**
 * PATCH 请求
 * @param {string} url 请求路径
 * @param {object} [customHeaders] 自定义请求头
 * @param {object|string} [data] 请求体
 * @param {object} [options] 额外配置
 * @returns {Promise}
 */
function patch(url, customHeaders, data, options) {
	return request(url, 'PATCH', customHeaders, data, options);
}

/**
 * DELETE 请求
 * @param {string} url 请求路径
 * @param {object} [customHeaders] 自定义请求头
 * @param {object} [params] 查询参数（可选）
 * @param {object} [options] 额外配置
 * @returns {Promise}
 */
function del(url, customHeaders, params, options) {
	return request(url, 'DELETE', customHeaders, params, options);
}

// ==================== 导出 ====================

const requestUtil = {
	get,
	post,
	put,
	patch,
	delete: del,
    // 工具方法
    md5,
	generateDeviceId,
	generateUUID,
	getDeviceId,
};

module.exports = {
	request: requestUtil,
	// 也支持直接导出方法
	get,
	post,
	put,
	patch,
    delete: del,
    md5,
	generateDeviceId,
	generateUUID,
};

/**
 * 使用示例：
 *
 * // 方式1：使用 request 对象
 * const { request } = require('../../utils/request');
 * await request.get('/v1/users/verify-code', { 'Content-Type': 'application/json' }, { phone: '13800138000' });
 * await request.post('/v1/users/login', { 'Content-Type': 'application/json' }, { phone: '13800138000', code: '123456' });
 *
 * // 方式2：直接导入方法
 * const { get, post } = require('../../utils/request');
 * await get('/v1/users/info');
 * await post('/v1/users/update', { 'Content-Type': 'application/json' }, { name: '张三' });
 *
 * // 在 app.js 中初始化设备ID（可选，工具会自动生成）
 * const { generateDeviceId } = require('./utils/request');
 * const deviceId = generateDeviceId();
 * this.globalData.deviceId = deviceId;
 */

