/**
 * utils/token.js
 * 微信小程序令牌管理工具
 * 
 * 功能：
 * - 管理多种令牌类型：普通令牌、特权令牌、工作令牌、管理令牌、自动登录令牌
 * - 令牌获取：封装与后端的交互，自动填充必要的认证信息
 * - 令牌存储：同时存储到内存(globalData)和本地存储(storage)
 * - 令牌读取：优先从内存读取，过期前10分钟自动刷新
 * - 令牌有效性检查：自动验证令牌是否过期
 * 
 * 使用方式：
 * const tokenManager = require('../../utils/token.js');
 * // 获取令牌
 * const normalToken = await tokenManager.getNormalToken();
 * // 存储令牌
 * await tokenManager.saveNormalToken(token, expireTime);
 */

// ==================== 依赖引入 ====================
const { request } = require('./request');
const { parseToTimestamp } = require('./time-util');

// ==================== 存储工具函数 ====================

/**
 * 存储数据到本地（带过期时间）
 * @param {string} key 存储键
 * @param {any} value 存储值
 * @returns {boolean} 是否成功
 */
function setStorageSyncWithExpire(key, value) {
    try {
        // 直接存储传入的值，过期时间检查由value内部的expireTime字段决定
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
 * @returns {any|null} 存储的值，如果不存在或过期返回null
 */
function getStorageSyncWithExpire(key) {
    try {
        const data = wx.getStorageSync(key);
        if (!data) {
            return null;
        }
        
        // 对于工作令牌数组，需要特殊处理
        if (key === TOKEN_KEYS.WORK && Array.isArray(data)) {
            // 过滤掉已过期的工作令牌
            const validTokens = data.filter(token => {
                if (!token.expireTime) return false; // 没有过期时间的令牌视为无效
                return !isTokenExpired(token.expireTime); // 保留未过期的令牌
            });
            
            // 如果有令牌被过滤掉（即有过期令牌），更新存储
            if (validTokens.length !== data.length) {
                setStorageSyncWithExpire(TOKEN_KEYS.WORK, validTokens);
            }
            
            return validTokens;
        }
        
        // 对于其他类型的令牌，检查其expireTime字段
        if (data.expireTime && Date.now() > data.expireTime) {
            // 过期，删除并返回null
            wx.removeStorageSync(key);
            return null;
        }
        
        // 直接返回数据，因为现在存储的就是原始数据
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
function removeStorageSync(key) {
    try {
        wx.removeStorageSync(key);
        return true;
    } catch (error) {
        console.error('[token.js] 删除失败:', error);
        return false;
    }
}

// ==================== 常量定义 ====================
const TOKEN_KEYS = {
    NORMAL: 'normalToken',
    PRIVILEGE: 'privilegeToken',
    WORK: 'workTokens',
    MANAGER: 'managerToken',
    AUTO_LOGIN: 'autoLoginToken'
};

const TOKEN_TYPES = {
    NORMAL: 1,
    WORK: 2
};

const TOKEN_STATUS = {
    VALID: 'valid',
    EXPIRED: 'expired',
    NOT_FOUND: 'not_found',
    ERROR: 'error'
};

const API_ENDPOINTS = {
    NORMAL: '/v1/tokens/normal',
    PRIVILEGE: '/v1/tokens/privilege',
    WORK: '/v1/tokens/work',
    MANAGER: '/v1/tokens/manager',
    REFRESH: '/v1/tokens/refresh',
    LOGOUT: '/v1/tokens'
};

// 自动刷新阈值（毫秒）- 10分钟
const AUTO_REFRESH_THRESHOLD = 10 * 60 * 1000;

// ==================== 工具函数 ====================

/**
 * 获取App实例和globalData
 * @returns {object} App实例和globalData
 */
function getAppInstance() {
    try {
        const app = getApp();
        if (!app || !app.globalData) {
            throw new Error('无法获取App实例或globalData');
        }
        return { app, globalData: app.globalData };
    } catch (error) {
        // 在onLaunch阶段可能获取不到App实例，返回一个临时的globalData结构
        console.warn('[token.js] 无法获取App实例，使用临时globalData');
        return { 
            app: null, 
            globalData: {
                tokens: {
                    normalToken: null,
                    workTokens: [],
                    privilegeToken: null,
                    managerToken: null,
                    autoLoginToken: null
                }
            }
        };
    }
}

/**
 * 获取设备ID
 * @returns {string} 设备ID
 */
function getDeviceId() {
    const { globalData } = getAppInstance();
    if (!globalData.deviceId) {
        throw new Error('设备ID不存在，请确保App已正确初始化');
    }
    return globalData.deviceId;
}

/**
 * 检查令牌是否过期
 * @param {number} expireTimestamp 过期时间戳（毫秒）
 * @returns {boolean} true表示已过期，false表示未过期
 */
function isTokenExpired(expireTimestamp) {
    if (!expireTimestamp) return true;
    
    return Date.now() > expireTimestamp;
}

/**
 * 检查令牌是否需要在10分钟内刷新
 * @param {number} expireTimestamp 过期时间戳（毫秒）
 * @returns {boolean} true表示需要刷新，false表示不需要
 */
function shouldRefreshToken(expireTimestamp) {
    if (!expireTimestamp) return false;
    
    return (expireTimestamp - Date.now()) <= AUTO_REFRESH_THRESHOLD;
}

/**
 * 验证令牌参数
 * @param {string} token 令牌字符串
 * @param {string} expireTime 过期时间字符串
 * @throws {Error} 参数无效时抛出错误
 */
function validateTokenParams(token, expireTime) {
    if (!token || typeof token !== 'string' || token.trim() === '') {
        throw new Error('令牌格式无效：令牌不能为空');
    }
    if (!expireTime || typeof expireTime !== 'string' || expireTime.trim() === '') {
        throw new Error('过期时间格式无效：过期时间不能为空');
    }
}

/**
 * 验证店铺ID参数
 * @param {string} storeId 店铺ID
 * @throws {Error} 参数无效时抛出错误
 */
function validateStoreId(storeId) {
    if (!storeId || typeof storeId !== 'string' || storeId.trim() === '') {
        throw new Error('店铺ID格式无效：店铺ID不能为空');
    }
}

/**
 * 解析Bearer令牌，移除Bearer前缀
 * @param {string} bearerToken Bearer令牌
 * @returns {string} 纯令牌字符串
 */
function extractToken(bearerToken) {
    if (!bearerToken || typeof bearerToken !== 'string') return '';
    
    if (bearerToken.startsWith('Bearer ')) {
        return bearerToken.substring(7);
    }
    
    return bearerToken;
}

// ==================== 令牌存储函数 ====================

/**
 * 存储普通令牌
 * @param {string} token 令牌字符串
 * @param {string} expireTime 过期时间字符串（yyyy-MM-dd HH:mm:ss）
 * @returns {boolean} 存储是否成功
 */
function saveNormalToken(token, expireTime) {
    try {
        // 参数验证
        validateTokenParams(token, expireTime);
        
        const { globalData } = getAppInstance();
        
        // 将过期时间字符串转换为时间戳
        const expireTimestamp = parseToTimestamp(expireTime);
        
        // 确保令牌格式正确（避免重复Bearer前缀）
        const finalToken = token.startsWith('Bearer ') ? token : `Bearer ${token}`;
        
        // 存储到内存
        globalData.tokens.normalToken = {
            token: finalToken,
            expireTime: expireTimestamp
        };
        
        // 存储到本地
        setStorageSyncWithExpire(TOKEN_KEYS.NORMAL, {
            token: finalToken,
            expireTime: expireTimestamp
        });
        
        return true;
    } catch (error) {
        console.error('[token.js] 保存普通令牌失败:', error);
        return false;
    }
}

/**
 * 存储特权令牌
 * @param {string} token 令牌字符串
 * @param {string} expireTime 过期时间字符串（yyyy-MM-dd HH:mm:ss）
 * @returns {boolean} 存储是否成功
 */
function savePrivilegeToken(token, expireTime) {
    try {
        // 参数验证
        validateTokenParams(token, expireTime);
        
        const { globalData } = getAppInstance();
        
        // 将过期时间字符串转换为时间戳
        const expireTimestamp = parseToTimestamp(expireTime);
        
        // 确保令牌格式正确（避免重复Bearer前缀）
        const finalToken = token.startsWith('Bearer ') ? token : `Bearer ${token}`;
        
        // 存储到内存
        globalData.tokens.privilegeToken = {
            token: finalToken,
            expireTime: expireTimestamp
        };
        
        // 存储到本地
        setStorageSyncWithExpire(TOKEN_KEYS.PRIVILEGE, {
            token: finalToken,
            expireTime: expireTimestamp
        });
        
        return true;
    } catch (error) {
        console.error('[token.js] 保存特权令牌失败:', error);
        return false;
    }
}

/**
 * 存储工作令牌
 * @param {string} storeId 店铺ID
 * @param {string} token 令牌字符串
 * @param {string} expireTime 过期时间字符串（yyyy-MM-dd HH:mm:ss）
 * @returns {boolean} 存储是否成功
 */
function saveWorkToken(storeId, token, expireTime) {
    try {
        // 参数验证
        validateStoreId(storeId);
        validateTokenParams(token, expireTime);
        
        const { globalData } = getAppInstance();
        
        // 将过期时间字符串转换为时间戳
        const expireTimestamp = parseToTimestamp(expireTime);
        
        // 初始化工作令牌数组
        if (!globalData.tokens.workTokens) {
            globalData.tokens.workTokens = [];
        }
        
        // 查找是否已存在该店铺的工作令牌
        const existingIndex = globalData.tokens.workTokens.findIndex(item => item.storeId === storeId);
        // 确保令牌格式正确（避免重复Bearer前缀）
        const finalToken = token.startsWith('Bearer ') ? token : `Bearer ${token}`;
        
        const workTokenItem = {
            storeId,
            token: finalToken,
            expireTime: expireTimestamp
        };
        
        if (existingIndex >= 0) {
            // 更新现有令牌
            globalData.tokens.workTokens[existingIndex] = workTokenItem;
        } else {
            // 添加新令牌
            globalData.tokens.workTokens.push(workTokenItem);
        }
        
        // 存储到本地
        setStorageSyncWithExpire(TOKEN_KEYS.WORK, globalData.tokens.workTokens);
        
        return true;
    } catch (error) {
        console.error('[token.js] 保存工作令牌失败:', error);
        return false;
    }
}

/**
 * 存储管理令牌
 * @param {string} token 令牌字符串
 * @param {string} expireTime 过期时间字符串（yyyy-MM-dd HH:mm:ss）
 * @returns {boolean} 存储是否成功
 */
function saveManagerToken(token, expireTime) {
    try {
        // 参数验证
        validateTokenParams(token, expireTime);
        
        const { globalData } = getAppInstance();
        
        // 将过期时间字符串转换为时间戳
        const expireTimestamp = parseToTimestamp(expireTime);
        
        // 确保令牌格式正确（避免重复Bearer前缀）
        const finalToken = token.startsWith('Bearer ') ? token : `Bearer ${token}`;
        
        // 存储到内存
        globalData.tokens.managerToken = {
            token: finalToken,
            expireTime: expireTimestamp
        };
        
        // 存储到本地
        setStorageSyncWithExpire(TOKEN_KEYS.MANAGER, {
            token: finalToken,
            expireTime: expireTimestamp
        });
        
        return true;
    } catch (error) {
        console.error('[token.js] 保存管理令牌失败:', error);
        return false;
    }
}

/**
 * 存储自动登录令牌
 * @param {string} token 令牌字符串
 * @param {string} expireTime 过期时间字符串（yyyy-MM-dd HH:mm:ss）
 * @returns {boolean} 存储是否成功
 */
function saveAutoLoginToken(token, expireTime) {
    try {
        // 参数验证
        validateTokenParams(token, expireTime);
        
        const { globalData } = getAppInstance();
        
        // 将过期时间字符串转换为时间戳
        const expireTimestamp = parseToTimestamp(expireTime);
        
        // 确保令牌格式正确（避免重复Bearer前缀）
        const finalToken = token.startsWith('Bearer ') ? token : `Bearer ${token}`;
        
        // 存储到内存
        globalData.tokens.autoLoginToken = {
            token: finalToken,
            expireTime: expireTimestamp
        };
        
        // 存储到本地
        setStorageSyncWithExpire(TOKEN_KEYS.AUTO_LOGIN, {
            token: finalToken,
            expireTime: expireTimestamp
        });
        
        return true;
    } catch (error) {
        console.error('[token.js] 保存自动登录令牌失败:', error);
        return false;
    }
}

// ==================== 令牌获取函数 ====================

/**
 * 获取普通令牌（通过自动登录令牌）
 * @returns {Promise<object>} 后端返回的响应体
 */
async function fetchNormalTokenByAutoLogin() {
    try {
        // 获取自动登录令牌
        const autoLoginToken = await getAutoLoginToken();
        if (!autoLoginToken || autoLoginToken == '已过期') {
            throw new Error('自动登录令牌不存在或已过期，需要重新登录');
        }
        
        // 调用新的自动登录接口
        const deviceId = getDeviceId();
        
        const response = await request.post('/v1/users/autologin-wx', {
            'Content-Type': 'application/json',
            'Authorization': autoLoginToken
        });
        
        console.log('[token.js] 收到后端响应:', response);
        if(response.code != 200)  {
          throw new Error('自动登录错误，需要重新登录');
        }
        // 提取实际数据（后端返回的是Result结构，实际数据在data字段中）
        const data = response?.data || response;
        
        // 处理响应并保存令牌
        if (data && data.normalToken && data.normalExpireTime) {
            // 保存普通令牌
            saveNormalToken(data.normalToken, data.normalExpireTime);
            
            // 如果有新的自动登录令牌（令牌轮换），保存它
            if (data.tokenRotated && data.newAutoLoginToken && data.newAutoExpireTime) {
                saveAutoLoginToken(data.newAutoLoginToken, data.newAutoExpireTime);
            }
            
            // 保存用户信息到globalData
            if (data.userInfo) {
                const { globalData } = getAppInstance();
                globalData.userInfo = data.userInfo;
            }
        }
        
        return response;
    } catch (error) {
        console.error('[token.js] 获取普通令牌失败:', error);
        throw error;
    }
}
/*
// 调用者代码示例
const tokenManager = require('../../utils/token.js');
async function someBusinessLogic() {
  try {
    // 尝试通过自动登录令牌获取普通令牌
    const normalTokenResp = await tokenManager.fetchNormalTokenByAutoLogin();
    // 成功获取，继续业务逻辑
    console.log('普通令牌获取成功', normalTokenResp);
  } catch (error) {
    // 关键：判断错误是否为“自动登录令牌无效/过期”
    if (error.message === '自动登录令牌不存在或已过期，需要重新登录') {
      // 触发重新登录逻辑（如跳转登录页）
      console.log('触发重新登录...');
      wx.navigateTo({ url: '/pages/login/login' }); // 示例：跳转登录页
    } else {
      // 其他错误（如接口异常、设备ID缺失等），做其他处理
      console.error('其他错误:', error);
    }
  }
}
*/

/**
 * 获取特权令牌
 * @param {Array<string>} targetOperate 目标操作列表
 * @returns {Promise<object>} 后端返回的响应体
 */
async function fetchPrivilegeToken(targetOperate) {
    try {
        // 获取普通令牌
        const normalToken = await getNormalToken();
        if (!normalToken) {
            throw new Error('普通令牌不存在或已过期');
        }
        
        // 调用后端接口
        const deviceId = getDeviceId();
        const response = await request.post(API_ENDPOINTS.PRIVILEGE, {
            'Content-Type': 'application/json',
            'Authorization': normalToken,
            'X-Device-ID': deviceId
        }, {
            targetOperate
        });
        
        // 保存令牌
        if (response && response.token && response.expireTime) {
            savePrivilegeToken(response.token, response.expireTime);
        }
        
        return response;
    } catch (error) {
        console.error('[token.js] 获取特权令牌失败:', error);
        throw error;
    }
}

/**
 * 获取工作令牌
 * @param {string} storeId 店铺ID
 * @returns {Promise<object>} 后端返回的响应体
 */
async function fetchWorkToken(storeId) {
    try {
        const normalToken = await getNormalToken();
        if (!normalToken) {
            throw new Error('普通令牌不存在或已过期');
        }
        
        const deviceId = getDeviceId();
        const response = await request.post(API_ENDPOINTS.WORK, {
            'Content-Type': 'application/json',
            'Authorization': normalToken,
            'X-Device-ID': deviceId
        }, {
            storeId
        });
        
        // 后端返回格式: {code: 200, data: {token, expireTime}, ...}
        const tokenData = response?.data || response;
        
        if (tokenData && tokenData.token && tokenData.expireTime) {
            saveWorkToken(storeId, tokenData.token, tokenData.expireTime);
        }
        
        return tokenData;
    } catch (error) {
        console.error('[token.js] 获取工作令牌失败:', error);
        throw error;
    }
}

/**
 * 获取管理令牌
 * @param {string} sndPswd 二级密码
 * @param {string} storeId 目标操作店铺ID（必选）
 * @returns {Promise<object>} 后端返回的响应体
 */
async function fetchManagerToken(sndPswd, storeId) {
    try {
        const { globalData } = getAppInstance();
        
        // 检查用户类型
        if (!globalData.userInfo || globalData.userInfo.userType !== 2) {
            throw new Error('用户类型不是商家，无法获取管理令牌');
        }
        
        // 获取商家ID
        if (!globalData.userInfo.merchantInfo || !globalData.userInfo.merchantInfo.merchantId) {
            throw new Error('商家信息不存在，无法获取管理令牌');
        }
        
        const merchantId = globalData.userInfo.merchantInfo.merchantId;
        
        // 获取普通令牌
        const normalToken = await getNormalToken();
        if (!normalToken) {
            throw new Error('普通令牌不存在或已过期');
        }
        
        // 调用后端接口
        const deviceId = getDeviceId();
        const requestData = {
            merchantId,
            sndPswd
        };
        
        if (storeId) {
            requestData.storeId = storeId;
        }
        
        const response = await request.post(API_ENDPOINTS.MANAGER, {
            'Content-Type': 'application/json',
            'Authorization': normalToken,
            'X-Device-ID': deviceId
        }, requestData);
        
        // 保存令牌
        if (response && response.token && response.expireTime) {
            saveManagerToken(response.token, response.expireTime);
        }
        
        return response;
    } catch (error) {
        console.error('[token.js] 获取管理令牌失败:', error);
        throw error;
    }
}

// ==================== 令牌刷新函数 ====================

/**
 * 刷新普通令牌
 * @param {boolean} skipAutoLogin 是否跳过自动登录令牌获取（防止无限递归）
 * @returns {Promise<object>} 后端返回的响应体
 */
async function refreshNormalToken(skipAutoLogin = false) {
    try {
        const { globalData } = getAppInstance();
        const currentToken = globalData.tokens.normalToken;
        
        // 优先尝试使用有效的普通令牌刷新
        if (currentToken && currentToken.token && !isTokenExpired(currentToken.expireTime)) {
            console.log('[token.js] 使用现有普通令牌进行刷新');
            
            const deviceId = getDeviceId();
            const response = await request.put(API_ENDPOINTS.REFRESH, {
                'Content-Type': 'application/json',
                'Authorization': currentToken.token,
                'X-Device-ID': deviceId
            }, {
                tokenType: TOKEN_TYPES.NORMAL
            });
            
            // 保存新令牌
            if (response && response.newToken && response.newExpireTime) {
                saveNormalToken(response.newToken, response.newExpireTime);
            }
            console.log('[token.js] 普通令牌刷新成功');
            return response;
        }
        
        // 如果没有有效的普通令牌，且未跳过自动登录，尝试使用自动登录令牌
        if (!skipAutoLogin) {
            const autoLoginToken = await getAutoLoginToken();
            if (autoLoginToken && autoLoginToken !== '已过期') {
                console.log('[token.js] 普通令牌无效，使用自动登录令牌获取新的普通令牌');
                return await fetchNormalTokenByAutoLogin();
            }
        }
        
        // 都没有有效令牌
        throw new Error('无有效的普通令牌或自动登录令牌，无法刷新');
        
    } catch (error) {
        console.error('[token.js] 刷新普通令牌失败:', error);
        
        // 如果是刷新接口调用失败，且有当前令牌，返回当前令牌避免无限递归
        const { globalData } = getAppInstance();
        const currentToken = globalData.tokens.normalToken;
        
        if (currentToken && currentToken.token && !skipAutoLogin) {
            console.warn('[token.js] 刷新失败，返回当前令牌');
            return {
                success: false,
                token: currentToken.token,
                expireTime: currentToken.expireTime,
                error: error.message
            };
        } else {
            // 如果连当前令牌都没有，或者已经跳过自动登录，才抛出错误
            throw error;
        }
    }
}

/**
 * 刷新工作令牌
 * @param {string} storeId 店铺ID
 * @returns {Promise<object>} 后端返回的响应体
 */
async function refreshWorkToken(storeId) {
    try {
        // 获取当前工作令牌
        const { globalData } = getAppInstance();
        const workTokens = globalData.tokens.workTokens || [];
        const currentToken = workTokens.find(item => item.storeId === storeId);
        
        if (!currentToken || !currentToken.token) {
            throw new Error(`店铺 ${storeId} 的工作令牌不存在，无法刷新`);
        }
        
        // 调用后端接口
        const deviceId = getDeviceId();
        const response = await request.put(API_ENDPOINTS.REFRESH, {
            'Content-Type': 'application/json',
            'Authorization': currentToken.token,
            'X-Device-ID': deviceId
        }, {
            tokenType: TOKEN_TYPES.WORK
        });
        
        // 保存新令牌
        if (response && response.newToken && response.newExpireTime) {
            saveWorkToken(storeId, response.newToken, response.newExpireTime);
        }
        
        return response;
    } catch (error) {
        console.error('[token.js] 刷新工作令牌失败:', error);
        // 不再抛出错误，而是返回一个包含当前令牌的对象
        const { globalData } = getAppInstance();
        const workTokens = globalData.tokens.workTokens || [];
        const currentToken = workTokens.find(item => item.storeId === storeId);
        
        if (currentToken && currentToken.token) {
            // 返回当前令牌，避免无限递归
            return {
                success: false,
                token: currentToken.token,
                expireTime: currentToken.expireTime,
                error: error.message
            };
        } else {
            // 如果连当前令牌都没有，才抛出错误
            throw error;
        }
    }
}

// ==================== 令牌读取函数 ====================

/**
 * 获取普通令牌
 * @param {number} recursionDepth 递归深度，防止无限递归
 * @returns {Promise<string|null>} 令牌字符串，如果不存在或过期返回null
 */
async function getNormalToken(recursionDepth = 0) {
    try {
        const { globalData } = getAppInstance();
        
        // 优先从内存获取
        let tokenData = globalData.tokens.normalToken;
        
        // 如果内存中没有，从本地存储获取
        if (!tokenData) {
            tokenData = getStorageSyncWithExpire(TOKEN_KEYS.NORMAL);
            if (tokenData) {
                // 同步到内存
                globalData.tokens.normalToken = tokenData;
            }
        }
        
        // 检查令牌是否存在
        if (!tokenData || !tokenData.token) {
            return null;
        }
        
        // 检查令牌是否过期
        if (isTokenExpired(tokenData.expireTime)) {
            // 过期令牌自动清理
            globalData.tokens.normalToken = null;
            removeStorageSync(TOKEN_KEYS.NORMAL);
            console.log('[token.js] 普通令牌已过期，已自动清理');
            return null;
        }
        
        // 检查是否需要自动刷新（防止无限递归）
        if (shouldRefreshToken(tokenData.expireTime) && recursionDepth === 0) {
            try {
                const refreshResult = await refreshNormalToken(true); // 跳过自动登录避免递归
                
                // 检查刷新是否成功
                if (refreshResult && refreshResult.success === false) {
                    // 刷新失败，返回当前令牌（可能即将过期）
                    console.warn('[token.js] 自动刷新普通令牌失败，使用当前令牌:', refreshResult.error);
                    return tokenData.token;
                }
                
                // 刷新成功后，重新获取令牌（递归深度+1）
                return await getNormalToken(recursionDepth + 1);
            } catch (error) {
                console.error('[token.js] 自动刷新普通令牌失败:', error);
                // 刷新失败，返回当前令牌（可能即将过期）
                return tokenData.token;
            }
        }
        return tokenData.token;
    } catch (error) {
        console.error('[token.js] 获取普通令牌失败:', error);
        return null;
    }
}

/**
 * 获取特权令牌
 * @returns {Promise<string|null>} 令牌字符串，如果不存在返回null，如果过期返回"已过期"
 */
async function getPrivilegeToken() {
    try {
        const { globalData } = getAppInstance();
        
        // 优先从内存获取
        let tokenData = globalData.tokens.privilegeToken;
        
        // 如果内存中没有，从本地存储获取
        if (!tokenData) {
            tokenData = getStorageSyncWithExpire(TOKEN_KEYS.PRIVILEGE);
            if (tokenData) {
                // 同步到内存
                globalData.tokens.privilegeToken = tokenData;
            }
        }
        
        // 检查令牌是否存在
        if (!tokenData || !tokenData.token) {
            return null;
        }
        
        // 检查令牌是否过期
        if (isTokenExpired(tokenData.expireTime)) {
            // 过期令牌自动清理
            globalData.tokens.privilegeToken = null;
            removeStorageSync(TOKEN_KEYS.PRIVILEGE);
            console.log('[token.js] 特权令牌已过期，已自动清理');
            return null;
        }
        
        return tokenData.token;
    } catch (error) {
        console.error('[token.js] 获取特权令牌失败:', error);
        return null;
    }
}

/**
 * 获取工作令牌
 * @param {string} storeId 店铺ID
 * @param {number} recursionDepth 递归深度，防止无限递归
 * @returns {Promise<string|null>} 令牌字符串，如果不存在返回null，如果过期返回"已过期"
 */
async function getWorkToken(storeId, recursionDepth = 0) {
    try {
        const { globalData } = getAppInstance();
        
        // 优先从内存获取
        let workTokens = globalData.tokens.workTokens;
        
        // 如果内存中没有，从本地存储获取
        if (!workTokens) {
            workTokens = getStorageSyncWithExpire(TOKEN_KEYS.WORK);
            if (workTokens) {
                globalData.tokens.workTokens = workTokens;
            }
        }
        
        // 检查令牌数组是否存在
        if (!workTokens || !Array.isArray(workTokens)) {
            globalData.tokens.workTokens = [];
            setStorageSyncWithExpire(TOKEN_KEYS.WORK, []);
            return null;
        }
        
        // 查找指定店铺的令牌
        const tokenData = workTokens.find(item => item.storeId === storeId);
        
        if (!tokenData || !tokenData.token) {
            return null;
        }
        
        // 检查令牌是否过期
        if (isTokenExpired(tokenData.expireTime)) {
            const updatedTokens = workTokens.filter(item => item.storeId !== storeId);
            globalData.tokens.workTokens = updatedTokens;
            setStorageSyncWithExpire(TOKEN_KEYS.WORK, updatedTokens);
            return null;
        }
        
        // 检查是否需要自动刷新（防止无限递归）
        if (shouldRefreshToken(tokenData.expireTime) && recursionDepth === 0) {
            try {
                const refreshResult = await refreshWorkToken(storeId);
                if (refreshResult && refreshResult.success === false) {
                    return tokenData.token;
                }
                return await getWorkToken(storeId, recursionDepth + 1);
            } catch (error) {
                return tokenData.token;
            }
        }
        
        return tokenData.token;
    } catch (error) {
        console.error('[token.js] 获取工作令牌失败:', error);
        return null;
    }
}

/**
 * 获取管理令牌
 * @returns {Promise<string|null>} 令牌字符串，如果不存在返回null，如果过期返回"已过期"
 */
async function getManagerToken() {
    try {
        const { globalData } = getAppInstance();
        
        // 优先从内存获取
        let tokenData = globalData.tokens.managerToken;
        
        // 如果内存中没有，从本地存储获取
        if (!tokenData) {
            tokenData = getStorageSyncWithExpire(TOKEN_KEYS.MANAGER);
            if (tokenData) {
                // 同步到内存
                globalData.tokens.managerToken = tokenData;
            }
        }
        
        // 检查令牌是否存在
        if (!tokenData || !tokenData.token) {
            return null;
        }
        
        // 检查令牌是否过期
        if (isTokenExpired(tokenData.expireTime)) {
            // 过期令牌自动清理
            globalData.tokens.managerToken = null;
            removeStorageSync(TOKEN_KEYS.MANAGER);
            console.log('[token.js] 管理令牌已过期，已自动清理');
            return null;
        }
        
        return tokenData.token;
    } catch (error) {
        console.error('[token.js] 获取管理令牌失败:', error);
        return null;
    }
}

/**
 * 获取自动登录令牌|已过期
 * @returns {Promise<string|null>} 令牌字符串，如果不存在返回null，如果过期返回"已过期"
 */
async function getAutoLoginToken() {
    try {
        const { globalData } = getAppInstance();
        
        // 优先从内存获取
        let tokenData = globalData.tokens.autoLoginToken;
        
        // 如果内存中没有，从本地存储获取
        if (!tokenData) {
            tokenData = getStorageSyncWithExpire(TOKEN_KEYS.AUTO_LOGIN);
            if (tokenData) {
                // 同步到内存
                globalData.tokens.autoLoginToken = tokenData;
            }
        }
        
        // 检查令牌是否存在
        if (!tokenData || !tokenData.token) {
            return null;
        }
        
        // 检查令牌是否过期
        if (isTokenExpired(tokenData.expireTime)) {
            // 过期令牌自动清理
            globalData.tokens.autoLoginToken = null;
            removeStorageSync(TOKEN_KEYS.AUTO_LOGIN);
            console.log('[token.js] 自动登录令牌已过期，已自动清理');
            return null;
        }
        
        return tokenData.token;
    } catch (error) {
        console.error('[token.js] 获取自动登录令牌失败:', error);
        return null;
    }
}

// ==================== 令牌注销函数 ====================

/**
 * 注销当前令牌
 * @param {string} token 要注销的令牌
 * @returns {Promise<boolean>} 注销是否成功
 */
async function logoutToken(token) {
    try {
        const deviceId = getDeviceId();
        await request.delete(API_ENDPOINTS.LOGOUT, {
            'Authorization': token,
            'X-Device-ID': deviceId
        });
        
        return true;
    } catch (error) {
        console.error('[token.js] 注销令牌失败:', error);
        return false;
    }
}

// ==================== 导出接口 ====================

module.exports = {
    // 令牌获取
    fetchNormalTokenByAutoLogin,
    fetchPrivilegeToken,
    fetchWorkToken,
    fetchManagerToken,
    
    // 令牌存储
    saveNormalToken,
    savePrivilegeToken,
    saveWorkToken,
    saveManagerToken,
    saveAutoLoginToken,
    
    // 令牌读取
    getNormalToken,
    getPrivilegeToken,
    getWorkToken,
    getManagerToken,
    getAutoLoginToken,
    
    // 令牌刷新
    refreshNormalToken,
    refreshWorkToken,
    
    // 令牌注销
    logoutToken
};