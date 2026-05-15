/**
 * utils/token.js
 * 微信小程序令牌管理工具（新版 - 适配云函数 JWT 体系）
 * 
 * 功能：
 * - 管理5种令牌：①商家登录 ②商家访问 ③商家工作(数组) ④员工登录 ⑤员工工作
 * - 令牌获取：封装与后端的交互（仅商家工作令牌③）
 * - 令牌存储：同时存储到内存(globalData)和本地存储(storage)
 * - 令牌读取：优先内存 → 本地存储 → 刷新/后端获取，过期前10分钟自动刷新
 * - 令牌有效性检查：自动验证令牌是否过期
 * - 获取所有缓存令牌：供退出登录时收集待注销令牌
 * 
 * 使用方式：
 * const tokenManager = require('../../utils/token.js');
 * const accessToken = await tokenManager.getMerchantAccessToken();
 */

const { parseToTimestamp } = require('./time-util');

// ==================== 常量定义 ====================

const TOKEN_KEYS = {
    MERCHANT_LOGIN: 'merchantLoginToken',     // ①
    MERCHANT_ACCESS: 'merchantAccessToken',   // ②
    MERCHANT_WORK: 'merchantWorkTokens',      // ③ 数组
    STAFF_LOGIN: 'staffLoginToken',           // ④
    STAFF_WORK: 'staffWorkToken',             // ⑤
};

const API_ENDPOINTS = {
    WORK: '/api/v1/tokens/work',
    REFRESH: '/api/v1/tokens/refresh',
    LOGIN: '/api/v1/merchant/login',
};

// 自动刷新阈值（毫秒）- 10分钟
const AUTO_REFRESH_THRESHOLD = 10 * 60 * 1000;

// ==================== 存储工具函数 ====================

/**
 * 存储数据到本地
 * @param {string} key 存储键
 * @param {any} value 存储值
 * @returns {boolean} 是否成功
 */
function setStorageSyncSafe(key, value) {
    try {
        wx.setStorageSync(key, value);
        return true;
    } catch (error) {
        console.error('[token.js] 存储失败:', error);
        return false;
    }
}

/**
 * 从本地获取数据（检查过期时间）
 * @param {string} key 存储键
 * @returns {any|null} 存储的值，不存在或过期返回null
 */
function getStorageSyncSafe(key) {
    try {
        const data = wx.getStorageSync(key);
        if (!data) return null;

        // 商家工作令牌数组：过滤已过期的
        if (key === TOKEN_KEYS.MERCHANT_WORK && Array.isArray(data)) {
            const valid = data.filter(t => t.expireTime && !isTokenExpired(t.expireTime));
            if (valid.length !== data.length) {
                setStorageSyncSafe(TOKEN_KEYS.MERCHANT_WORK, valid);
            }
            return valid;
        }

        // 单令牌：检查过期
        if (data.expireTime && isTokenExpired(data.expireTime)) {
            wx.removeStorageSync(key);
            return null;
        }

        return data;
    } catch (error) {
        console.error('[token.js] 读取失败:', error);
        return null;
    }
}

/**
 * 删除本地存储数据
 * @param {string} key 存储键
 * @returns {boolean} 是否成功
 */
function removeStorageSyncSafe(key) {
    try {
        wx.removeStorageSync(key);
        return true;
    } catch (error) {
        console.error('[token.js] 删除失败:', error);
        return false;
    }
}

// ==================== 工具函数 ====================

/**
 * 获取App实例和globalData
 */
function getAppInstance() {
    try {
        const app = getApp();
        if (!app || !app.globalData) {
            throw new Error('无法获取App实例或globalData');
        }
        return { app, globalData: app.globalData };
    } catch (error) {
        console.warn('[token.js] 无法获取App实例，使用临时globalData');
        return {
            app: null,
            globalData: {
                tokens: {
                    merchantLoginToken: null,
                    merchantAccessToken: null,
                    merchantWorkTokens: [],
                    staffLoginToken: null,
                    staffWorkToken: null,
                }
            }
        };
    }
}

/**
 * 检查令牌是否过期
 * @param {number} expireTimestamp 过期时间戳（毫秒）
 * @returns {boolean} true表示已过期
 */
function isTokenExpired(expireTimestamp) {
    if (!expireTimestamp) return true;
    return Date.now() > expireTimestamp;
}

/**
 * 检查令牌是否需要在10分钟内刷新
 * @param {number} expireTimestamp 过期时间戳（毫秒）
 * @returns {boolean} true表示需要刷新
 */
function shouldRefreshToken(expireTimestamp) {
    if (!expireTimestamp) return false;
    return (expireTimestamp - Date.now()) <= AUTO_REFRESH_THRESHOLD;
}

/**
 * 解析Bearer令牌，移除Bearer前缀
 * @param {string} bearerToken Bearer令牌
 * @returns {string} 纯令牌字符串
 */
function extractToken(bearerToken) {
    if (!bearerToken || typeof bearerToken !== 'string') return '';
    return bearerToken.startsWith('Bearer ') ? bearerToken.substring(7) : bearerToken;
}

/**
 * 确保令牌带有Bearer前缀
 */
function ensureBearerPrefix(token) {
    if (!token || typeof token !== 'string') return token;
    return token.startsWith('Bearer ') ? token : `Bearer ${token}`;
}

/**
 * 解析过期时间参数（支持多种格式）
 * @param {string|number|undefined} expireTime 服务端返回的过期时间（ms时间戳或格式化字符串）
 * @returns {number|null} 毫秒时间戳
 */
function resolveExpireTime(expireTime) {
    if (typeof expireTime === 'number') return expireTime;
    if (typeof expireTime === 'string' && expireTime.trim()) {
        return parseToTimestamp(expireTime);
    }
    return null;
}

/**
 * 验证令牌参数
 */
function validateTokenParam(token) {
    if (!token || typeof token !== 'string' || token.trim() === '') {
        throw new Error('令牌格式无效：令牌不能为空');
    }
}

/**
 * 验证店铺ID参数
 */
function validateStoreId(storeId) {
    if (!storeId || typeof storeId !== 'string' || storeId.trim() === '') {
        throw new Error('店铺ID格式无效：店铺ID不能为空');
    }
}

// ==================== 令牌存储函数 ====================

/**
 * 通用单令牌存储（内存 + Storage）
 * @param {string} memKey globalData.tokens中的键名
 * @param {string} storageKey Storage存储键
 * @param {string} token 令牌字符串
 * @param {string|number} [expireTime] 过期时间（可选，缺省从JWT提取）
 */
function _saveSingleToken(memKey, storageKey, token, expireTime) {
    try {
        validateTokenParam(token);
        const { globalData } = getAppInstance();
        const finalToken = ensureBearerPrefix(token);
        const expireTimestamp = resolveExpireTime(expireTime);

        const data = { token: finalToken, expireTime: expireTimestamp };
        globalData.tokens[memKey] = data;
        setStorageSyncSafe(storageKey, data);
        return true;
    } catch (error) {
        console.error(`[token.js] 保存${memKey}失败:`, error);
        return false;
    }
}

/** 存储商家登录令牌① */
function saveMerchantLoginToken(token, expireTime) {
    return _saveSingleToken('merchantLoginToken', TOKEN_KEYS.MERCHANT_LOGIN, token, expireTime);
}

/** 存储商家访问令牌② */
function saveMerchantAccessToken(token, expireTime) {
    return _saveSingleToken('merchantAccessToken', TOKEN_KEYS.MERCHANT_ACCESS, token, expireTime);
}

/** 存储员工登录令牌④ */
function saveStaffLoginToken(token, expireTime) {
    return _saveSingleToken('staffLoginToken', TOKEN_KEYS.STAFF_LOGIN, token, expireTime);
}

/** 存储员工工作令牌⑤ */
function saveStaffWorkToken(token, expireTime) {
    return _saveSingleToken('staffWorkToken', TOKEN_KEYS.STAFF_WORK, token, expireTime);
}

/**
 * 存储商家工作令牌③（数组，按storeId）
 * @param {string} storeId 店铺ID
 * @param {string} token 令牌字符串
 * @param {string|number} [expireTime] 过期时间
 */
function saveMerchantWorkToken(storeId, token, expireTime) {
    try {
        validateStoreId(storeId);
        validateTokenParam(token);
        const { globalData } = getAppInstance();
        const finalToken = ensureBearerPrefix(token);
        const expireTimestamp = resolveExpireTime(expireTime);

        if (!globalData.tokens.merchantWorkTokens) {
            globalData.tokens.merchantWorkTokens = [];
        }

        const idx = globalData.tokens.merchantWorkTokens.findIndex(item => item.storeId === storeId);
        const workItem = { storeId, token: finalToken, expireTime: expireTimestamp };

        if (idx >= 0) {
            globalData.tokens.merchantWorkTokens[idx] = workItem;
        } else {
            globalData.tokens.merchantWorkTokens.push(workItem);
        }

        setStorageSyncSafe(TOKEN_KEYS.MERCHANT_WORK, globalData.tokens.merchantWorkTokens);
        return true;
    } catch (error) {
        console.error('[token.js] 保存商家工作令牌失败:', error);
        return false;
    }
}

// ==================== 令牌读取函数 ====================

/**
 * 通用单令牌读取（内存 → Storage，含过期检查，不含自动刷新）
 * @returns {{ token, expireTime }|null}
 */
function _readSingleToken(memKey, storageKey) {
    const { globalData } = getAppInstance();

    // 优先从内存获取
    let tokenData = globalData.tokens[memKey];

    // 内存没有，从Storage获取
    if (!tokenData) {
        tokenData = getStorageSyncSafe(storageKey);
        if (tokenData) {
            globalData.tokens[memKey] = tokenData;
        }
    }

    if (!tokenData || !tokenData.token) return null;

    // 检查是否过期
    if (isTokenExpired(tokenData.expireTime)) {
        globalData.tokens[memKey] = null;
        removeStorageSyncSafe(storageKey);
        console.log(`[token.js] ${memKey}已过期，已自动清理`);
        return null;
    }

    return tokenData;
}

/**
 * 获取商家登录令牌①（不自动刷新，过期需重新登录）
 * @returns {Promise<string|null>} 令牌字符串
 */
async function getMerchantLoginToken() {
    try {
        const data = _readSingleToken('merchantLoginToken', TOKEN_KEYS.MERCHANT_LOGIN);
        return data ? data.token : null;
    } catch (error) {
        console.error('[token.js] 获取商家登录令牌失败:', error);
        return null;
    }
}

/**
 * 获取商家访问令牌②（自动刷新）
 * 临近过期 → 调用刷新接口 /tokens/refresh
 * 已过期/不存在 → 用①自动登录换取新②
 * @param {number} recursionDepth 递归深度，防止无限递归
 * @returns {Promise<string|null>} 令牌字符串
 */
async function getMerchantAccessToken(recursionDepth = 0) {
    try {
        const data = _readSingleToken('merchantAccessToken', TOKEN_KEYS.MERCHANT_ACCESS);

        // 不存在或已过期 → 用①自动登录
        if (!data || !data.token) {
            if (recursionDepth === 0) {
                const refreshed = await _autoLoginWithMerchantLogin();
                if (refreshed) return await getMerchantAccessToken(1);
            }
            return null;
        }

        // 临近过期 → 调用刷新接口
        if (shouldRefreshToken(data.expireTime) && recursionDepth === 0) {
            try {
                await _refreshToken(data.token, 'access');
                return await getMerchantAccessToken(1);
            } catch (e) {
                console.warn('[token.js] 刷新商家访问令牌失败，使用当前令牌:', e.message);
                return data.token;
            }
        }

        return data.token;
    } catch (error) {
        console.error('[token.js] 获取商家访问令牌失败:', error);
        return null;
    }
}

/**
 * 获取商家工作令牌③（自动刷新）
 * 临近过期 → 调用刷新接口 /tokens/refresh
 * 已过期/不存在 → 用②换取新③
 * @param {string} storeId 店铺ID
 * @param {number} recursionDepth 递归深度
 * @returns {Promise<string|null>} 令牌字符串
 */
async function getMerchantWorkToken(storeId, recursionDepth = 0) {
    try {
        validateStoreId(storeId);
        const { globalData } = getAppInstance();

        // 优先从内存获取
        let workTokens = globalData.tokens.merchantWorkTokens;

        // 内存没有，从Storage获取
        if (!workTokens || !Array.isArray(workTokens)) {
            workTokens = getStorageSyncSafe(TOKEN_KEYS.MERCHANT_WORK) || [];
            globalData.tokens.merchantWorkTokens = workTokens;
        }

        const tokenData = workTokens.find(item => item.storeId === storeId);

        // 不存在 → 用②获取
        if (!tokenData || !tokenData.token) {
            if (recursionDepth === 0) {
                try {
                    await fetchMerchantWorkToken(storeId);
                    return await getMerchantWorkToken(storeId, 1);
                } catch (e) {
                    console.error('[token.js] 获取新工作令牌失败:', e);
                }
            }
            return null;
        }

        // 已过期 → 清除后重新获取
        if (isTokenExpired(tokenData.expireTime)) {
            const updated = workTokens.filter(item => item.storeId !== storeId);
            globalData.tokens.merchantWorkTokens = updated;
            setStorageSyncSafe(TOKEN_KEYS.MERCHANT_WORK, updated);

            if (recursionDepth === 0) {
                try {
                    await fetchMerchantWorkToken(storeId);
                    return await getMerchantWorkToken(storeId, 1);
                } catch (e) {
                    console.error('[token.js] 获取新工作令牌失败:', e);
                }
            }
            return null;
        }

        // 临近过期 → 刷新
        if (shouldRefreshToken(tokenData.expireTime) && recursionDepth === 0) {
            try {
                await _refreshToken(tokenData.token, 'work');
                return await getMerchantWorkToken(storeId, 1);
            } catch (e) {
                console.warn('[token.js] 刷新商家工作令牌失败，使用当前令牌:', e.message);
                return tokenData.token;
            }
        }

        return tokenData.token;
    } catch (error) {
        console.error('[token.js] 获取商家工作令牌失败:', error);
        return null;
    }
}

/**
 * 获取员工登录令牌④（不自动刷新，过期需重新登录）
 * @returns {Promise<string|null>}
 */
async function getStaffLoginToken() {
    try {
        const data = _readSingleToken('staffLoginToken', TOKEN_KEYS.STAFF_LOGIN);
        return data ? data.token : null;
    } catch (error) {
        console.error('[token.js] 获取员工登录令牌失败:', error);
        return null;
    }
}

/**
 * 获取员工工作令牌⑤（自动刷新）
 * 临近过期/已过期 → 用④自动登录换取
 * @param {number} recursionDepth 递归深度
 * @returns {Promise<string|null>}
 */
async function getStaffWorkToken(recursionDepth = 0) {
    try {
        const data = _readSingleToken('staffWorkToken', TOKEN_KEYS.STAFF_WORK);

        if (!data || !data.token) {
            // TODO: 员工自动登录接口（携带④换取⑤）待实现
            return null;
        }

        if (shouldRefreshToken(data.expireTime) && recursionDepth === 0) {
            // TODO: 员工令牌刷新逻辑待实现
            return data.token;
        }

        return data.token;
    } catch (error) {
        console.error('[token.js] 获取员工工作令牌失败:', error);
        return null;
    }
}

// ==================== 令牌获取函数（从云函数） ====================

/**
 * 获取商家工作令牌③（从云函数）
 * 携带②访问令牌，指定storeId，调用 /api/v1/tokens/work
 * @param {string} storeId 店铺ID
 * @returns {Promise<object>} 后端返回的data字段
 */
async function fetchMerchantWorkToken(storeId) {
    try {
        validateStoreId(storeId);
        const accessToken = await getMerchantAccessToken();
        if (!accessToken) {
            throw new Error('商家访问令牌不存在或已过期');
        }

        const { request } = require('./request');
        const response = await request.post(API_ENDPOINTS.WORK, {
            'Content-Type': 'application/json',
            'Authorization': accessToken,
        }, { storeId });

        const data = response.data;
        if (data && data.workToken) {
            saveMerchantWorkToken(storeId, data.workToken, data.workTokenExpireTime);
        }

        return data;
    } catch (error) {
        console.error('[token.js] 获取商家工作令牌失败:', error);
        throw error;
    }
}

// ==================== 令牌刷新函数 ====================

/**
 * 用商家登录令牌①自动登录，换取新的访问令牌②
 * 调用 /api/v1/merchant/login (type=3)
 * @returns {Promise<boolean>} 是否成功
 */
async function _autoLoginWithMerchantLogin() {
    try {
        const loginToken = await getMerchantLoginToken();
        if (!loginToken) {
            console.log('[token.js] 商家登录令牌不存在或已过期，需重新登录');
            return false;
        }

        const deviceId = wx.getStorageSync('deviceId') || '';
        const { request } = require('./request');

        const response = await request.post(API_ENDPOINTS.LOGIN, {
            'Content-Type': 'application/json',
            'Authorization': loginToken,
        }, { type: 3, deviceId });

        const data = response.data;
        if (data && data.accessToken) {
            saveMerchantAccessToken(data.accessToken, data.accessTokenExpireTime);
            // 更新商家信息到全局
            if (data.merchantInfo) {
                const { globalData } = getAppInstance();
                globalData.userInfo = data.merchantInfo;
            }
            console.log('[token.js] 自动登录成功，已获取新访问令牌');
            return true;
        }
        return false;
    } catch (error) {
        console.error('[token.js] 自动登录失败:', error);
        return false;
    }
}

/**
 * 调用令牌刷新接口（通用）
 * 调用 /api/v1/tokens/refresh，旧令牌写入黑名单后签发同类型新令牌
 * @param {string} oldToken 待刷新的令牌（Bearer格式）
 * @param {string} tokenType 'access'|'work'
 * @returns {Promise<object>} 刷新结果
 */
async function _refreshToken(oldToken, tokenType) {
    try {
        const { request } = require('./request');

        const response = await request.post(API_ENDPOINTS.REFRESH, {
            'Content-Type': 'application/json',
            'Authorization': oldToken,
        }, {});

        const data = response.data;
        if (data && data.newToken) {
            if (tokenType === 'access') {
                saveMerchantAccessToken(data.newToken, data.newExpireTime);
                console.log('[token.js] 商家访问令牌刷新成功');
            } else if (tokenType === 'work') {
                if (data.storeId) {
                    saveMerchantWorkToken(data.storeId, data.newToken, data.newExpireTime);
                    console.log('[token.js] 商家工作令牌刷新成功, storeId:', data.storeId);
                }
            }
            return data;
        }
        throw new Error(data?.message || '刷新失败');
    } catch (error) {
        console.error(`[token.js] 刷新${tokenType}令牌失败:`, error);
        throw error;
    }
}

// ==================== 获取所有缓存令牌 / 清除 ====================

/**
 * 获取缓存中所有令牌字符串（供退出登录时收集待注销令牌）
 * 返回纯JWT字符串（不含Bearer前缀），直接传给 /api/v1/tokens/logout
 * @returns {string[]} 令牌字符串数组
 */
function getAllCachedTokens() {
    const { globalData } = getAppInstance();
    const tokens = [];

    // 单令牌类型
    const singleKeys = [
        'merchantLoginToken', 'merchantAccessToken',
        'staffLoginToken', 'staffWorkToken',
    ];

    singleKeys.forEach(key => {
        const data = globalData.tokens[key];
        if (data && data.token) {
            tokens.push(extractToken(data.token));
        }
    });

    // 商家工作令牌数组
    const workTokens = globalData.tokens.merchantWorkTokens;
    if (Array.isArray(workTokens)) {
        workTokens.forEach(item => {
            if (item && item.token) {
                tokens.push(extractToken(item.token));
            }
        });
    }

    return tokens;
}

/**
 * 清除所有缓存令牌（退出登录后调用）
 */
function clearAllTokens() {
    const { globalData } = getAppInstance();
    globalData.tokens = {
        merchantLoginToken: null,
        merchantAccessToken: null,
        merchantWorkTokens: [],
        staffLoginToken: null,
        staffWorkToken: null,
    };
    Object.values(TOKEN_KEYS).forEach(key => removeStorageSyncSafe(key));
}

// ==================== 导出接口 ====================

module.exports = {
    // 令牌存储
    saveMerchantLoginToken,
    saveMerchantAccessToken,
    saveMerchantWorkToken,
    saveStaffLoginToken,
    saveStaffWorkToken,

    // 令牌读取（自动刷新）
    getMerchantLoginToken,
    getMerchantAccessToken,
    getMerchantWorkToken,
    getStaffLoginToken,
    getStaffWorkToken,

    // 令牌获取（从云函数）
    fetchMerchantWorkToken,

    // 获取所有缓存令牌（退出登录用）
    getAllCachedTokens,
    clearAllTokens,

    // 常量
    TOKEN_KEYS,
};
