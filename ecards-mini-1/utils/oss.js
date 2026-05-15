/**
 * 阿里云OSS工具函数
 * 用于微信小程序端的OSS上传和签名URL生成
 * 
 * 说明：通过后端获取STS临时凭证，前端使用凭证计算签名后直传OSS
 * 
 * pathType 支持：
 * - merchant: 商户图片 (默认)
 * - user: 用户头像
 * - member: 会员相关
 * - card: 会员卡
 * - employee: 员工相关
 * - resource: 预约资源
 * 
 * 缓存策略：内存 + Storage 双层缓存
 * - 优先使用内存缓存（快速）
 * - 内存没有时从 Storage 恢复（离线重启后可用）
 * - 凭证有效期 30 分钟，提前 5 分钟刷新
 */

const { request } = require('./request');
const tokenManager = require('./token');
const CryptoJS = require('crypto-js');
const { set: storageSet, get: storageGet, remove: storageRemove } = require('./storage-utils');

// 允许的 pathType 值
const VALID_PATH_TYPES = ['merchant', 'user', 'member', 'card', 'employee', 'resource'];

// Storage key 前缀
const STS_STORAGE_KEY_PREFIX = 'sts_';

// 内存缓存STS凭证（按 pathType 分别缓存）
const stsCredentialsCache = {};

// 内存缓存签名URL（objectName → { url, expireTime }）
const signedUrlCache = {};

// 签名URL缓存配置
const SIGNED_URL_CACHE_CONFIG = {
  defaultExpires: 1800,      // 默认有效期30分钟
  refreshBuffer: 5 * 60      // 提前5分钟刷新
};

/**
 * 校验 pathType 是否合法
 * @param {string} pathType - 路径类型
 * @returns {boolean} 是否合法
 */
function isValidPathType(pathType) {
  return VALID_PATH_TYPES.includes(pathType);
}

/**
 * 检查凭证是否有效（未过期，且剩余有效期 > 5分钟）
 * @param {Object} credentials - STS凭证
 * @returns {boolean}
 */
function isCredentialsValid(credentials) {
  if (!credentials || !credentials.expiration) {
    return false;
  }
  const now = new Date().getTime();
  const expTime = new Date(credentials.expiration).getTime();
  return expTime - now > 5 * 60 * 1000; // 提前5分钟刷新
}

/**
 * 计算凭证剩余有效期（秒）
 * @param {Object} credentials - STS凭证
 * @returns {number} 剩余秒数
 */
function getCredentialsRemainingSeconds(credentials) {
  if (!credentials || !credentials.expiration) {
    return 0;
  }
  const now = new Date().getTime();
  const expTime = new Date(credentials.expiration).getTime();
  return Math.max(0, Math.floor((expTime - now) / 1000));
}

/**
 * 获取STS临时凭证（内存 + Storage 双层缓存，提前5分钟刷新）
 * @param {string} pathType - 路径类型，默认 'merchant'
 * @returns {Promise<Object>} STS凭证对象
 */
async function getStsCredentials(pathType = 'merchant') {
  // 校验 pathType，无效时直接拒绝
  if (!isValidPathType(pathType)) {
    const errorMsg = `无效的 pathType: "${pathType}"，允许值: ${VALID_PATH_TYPES.join(', ')}`;
    console.error(`[OSS] ${errorMsg}`);
    throw new Error(errorMsg);
  }
  
  const cacheKey = pathType;
  const storageKey = STS_STORAGE_KEY_PREFIX + cacheKey;
  
  // 第一层：检查内存缓存
  const memoryCached = stsCredentialsCache[cacheKey];
  if (isCredentialsValid(memoryCached)) {
    return memoryCached;
  }
  
  // 第二层：检查 Storage 缓存
  try {
    const storageCached = await storageGet(storageKey);
    if (storageCached && isCredentialsValid(storageCached)) {
      // 恢复到内存缓存
      stsCredentialsCache[cacheKey] = storageCached;
      return storageCached;
    }
  } catch (e) {
    // 读取失败时静默忽略
  }

  // 第三层：从服务器获取新凭证
  const normalToken = await tokenManager.getNormalToken();
  if (!normalToken) {
    throw new Error('令牌已过期，请重新登录');
  }

  // 构建请求URL
  const url = pathType ? `/v1/sts/credentials?pathType=${pathType}` : '/v1/sts/credentials';

  // 获取STS凭证
  const result = await request.get(
    url,
    {
      'Content-Type': 'application/json',
      'Authorization': normalToken
    }
  );

  if (result.code === 200 && result.data) {
    const credentials = result.data;
    
    // 存入内存缓存
    stsCredentialsCache[cacheKey] = credentials;
    
    // 存入 Storage 缓存（过期时间设为凭证剩余有效期）
    const remainingSeconds = getCredentialsRemainingSeconds(credentials);
    if (remainingSeconds > 0) {
      try {
        await storageSet(storageKey, credentials, { expire: remainingSeconds });
      } catch (e) {
        // 存入失败时静默忽略
      }
    }
    
    return credentials;
  } else {
    throw new Error('获取STS凭证失败: ' + (result.message || '未知错误'));
  }
}

/**
 * 生成上传Policy（Base64编码）
 * @param {Object} credentials - STS凭证
 * @returns {string} Base64编码的policy
 */
function generatePolicy(credentials) {
  // Policy有效期：凭证过期时间或30分钟后
  const expiration = credentials.expiration || new Date(Date.now() + 30 * 60 * 1000).toISOString();
  
  const policy = {
    expiration: expiration,
    conditions: [
      { bucket: credentials.bucket },
      ['starts-with', '$key', credentials.pathPrefix || ''],
      ['content-length-range', 0, 10 * 1024 * 1024] // 最大10MB
    ]
  };
  
  const policyStr = JSON.stringify(policy);
  // 使用CryptoJS进行Base64编码
  const wordArray = CryptoJS.enc.Utf8.parse(policyStr);
  return CryptoJS.enc.Base64.stringify(wordArray);
}

/**
 * 生成签名
 * @param {string} accessKeySecret - STS AccessKeySecret
 * @param {string} policyBase64 - Base64编码的policy
 * @returns {string} 签名
 */
function generateSignature(accessKeySecret, policyBase64) {
  const signature = CryptoJS.HmacSHA1(policyBase64, accessKeySecret);
  return CryptoJS.enc.Base64.stringify(signature);
}

/**
 * 清除凭证缓存（退出登录时调用）
 * 同时清除内存和 Storage 中的缓存
 * @param {string} pathType - 可选，指定清除哪个类型的缓存，不传则清除全部
 */
async function clearStsCredentials(pathType) {
  if (pathType) {
    // 清除指定类型的内存缓存
    delete stsCredentialsCache[pathType];
    
    // 清除指定类型的 Storage 缓存
    const storageKey = STS_STORAGE_KEY_PREFIX + pathType;
    try {
      await storageRemove(storageKey);
    } catch (e) {
      // 读取失败时静默忽略
    }
    
  } else {
    // 清除所有内存缓存
    const keys = Object.keys(stsCredentialsCache);
    keys.forEach(key => {
      delete stsCredentialsCache[key];
    });
    
    // 清除所有类型的 Storage 缓存
    for (const type of VALID_PATH_TYPES) {
      const storageKey = STS_STORAGE_KEY_PREFIX + type;
      try {
        await storageRemove(storageKey);
      } catch (e) {
        // 忽略
      }
    }
    
  }
}

/**
 * 生成文件名
 * @param {string} prefix - 文件名前缀（如 'avatar', 'member_avatar'）
 * @param {string} ext - 文件扩展名（如 '.jpg', '.png'）
 * @returns {string} 生成的文件名
 */
function generateFileName(prefix, ext) {
  const timestamp = Date.now();
  const random = Math.random().toString(36).substring(2, 8);
  return `${prefix}_${timestamp}_${random}${ext}`;
}

/**
 * 生成卡种专用文件名
 * 命名格式: {prefix}_{hourTimestamp}_{randomCode}.{ext}
 * @param {string} prefix - 文件名前缀（'bkgd' 或 'mask'）
 * @param {string} randomCode - 6位随机码（0-9, a-z, A-Z）
 * @param {string} ext - 文件扩展名（不带点，如 'jpg', 'png'）
 * @returns {string} 生成的文件名
 */
function generateCardFileName(prefix, randomCode, ext) {
  const timestamp = Date.now();
  const hourTimestamp = Math.floor(timestamp / 3600000); // 整除3600000（小时）
  return `${prefix}_${hourTimestamp}_${randomCode}.${ext}`;
}

/**
 * 生成随机码（6位，由数字和字母组成）
 * @returns {string} 6位随机码
 */
function generateRandomCode() {
  const chars = '0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ';
  let code = '';
  for (let i = 0; i < 6; i++) {
    code += chars.charAt(Math.floor(Math.random() * chars.length));
  }
  return code;
}

/**
 * 上传卡种图片到OSS（背景/蒙版）
 * @param {string} filePath - 本地文件路径
 * @param {string} prefix - 文件名前缀（'bkgd' 或 'mask'）
 * @param {string} randomCode - 6位随机码
 * @param {Object} options - 选项
 * @param {Function} options.onProgress - 上传进度回调
 * @returns {Promise<string>} 返回Object路径
 */
async function uploadCardImage(filePath, prefix, randomCode, options = {}) {
  const { onProgress } = options;
  
  try {
    // 1. 获取文件扩展名
    const ext = filePath.substring(filePath.lastIndexOf('.') + 1).toLowerCase();
    
    // 2. 生成文件名
    const fileName = generateCardFileName(prefix, randomCode, ext);
    
    // 3. 上传到OSS（pathType='card'）
    const objectName = await uploadToOss(filePath, fileName, {
      pathType: 'card',
      onProgress
    });
    
    return objectName;
  } catch (error) {
    console.error('[卡种图片上传] 失败:', error);
    throw error;
  }
}

/**
 * 上传文件到OSS（微信小程序版）
 * 使用 PostObject 方式上传
 * @param {string} filePath - 本地文件路径
 * @param {string} fileName - 目标文件名
 * @param {Object} options - 选项
 * @param {string} options.pathType - 路径类型，默认 'merchant'
 * @param {Function} options.onProgress - 上传进度回调 (percent) => {}
 * @returns {Promise<string>} 返回Object路径（用于数据库存储）
 */
async function uploadToOss(filePath, fileName, options = {}) {
  // 兼容旧版调用方式：uploadToOss(filePath, fileName, onProgress)
  if (typeof options === 'function') {
    options = { onProgress: options };
  }
  
  const { pathType = 'merchant', onProgress } = options;
  
  try {
    // 1. 获取STS凭证（传入 pathType）
    const credentials = await getStsCredentials(pathType);
    
    // 2. 构建上传参数
    const objectName = (credentials.pathPrefix || '') + fileName;
    
    // 3. 构建上传URL
    // PostObject 需要使用 bucket 域名: https://{bucket}.{endpoint} 或 https://{bucket}.oss-{region}.aliyuncs.com
    let host = '';
    if (credentials.endpoint) {
      // endpoint 可能是 "oss-cn-hangzhou.aliyuncs.com" 或 "https://oss-cn-hangzhou.aliyuncs.com"
      let endpoint = credentials.endpoint.replace(/^https?:\/\//, ''); // 移除协议前缀
      host = `https://${credentials.bucket}.${endpoint}`;
    } else if (credentials.region) {
      // 使用 region 构建
      host = `https://${credentials.bucket}.oss-${credentials.region}.aliyuncs.com`;
    } else {
      throw new Error('缺少 endpoint 或 region 信息');
    }
    
    // 4. 生成policy和signature
    const policyBase64 = generatePolicy(credentials);
    const signature = generateSignature(credentials.accessKeySecret, policyBase64);
    
    // 5. 使用wx.uploadFile上传
    return new Promise((resolve, reject) => {
      const uploadTask = wx.uploadFile({
        url: host,
        filePath: filePath,
        name: 'file',
        formData: {
          'key': objectName,
          'OSSAccessKeyId': credentials.accessKeyId,
          'policy': policyBase64,
          'signature': signature,
          'x-oss-security-token': credentials.securityToken || '',
          'success_action_status': '200'
        },
        success: (res) => {
          if (res.statusCode === 200 || res.statusCode === 204) {
            resolve(objectName);
          } else {
            console.error('[OSS] 上传失败:', res.statusCode, res.data);
            reject(new Error('上传失败: HTTP ' + res.statusCode + ' - ' + (res.data || '')));
          }
        },
        fail: (err) => {
          console.error('[OSS] 上传失败:', err.errMsg);
          reject(new Error('上传失败: ' + err.errMsg));
        }
      });
      
      // 监听上传进度
      if (onProgress) {
        uploadTask.onProgressUpdate((res) => {
          onProgress(res.progress);
        });
      }
    });
  } catch (error) {
    console.error('[OSS] 上传异常:', error);
    throw error;
  }
}

/**
 * 根据objectName推断pathType
 * @param {string} objectName - OSS对象路径
 * @returns {string} pathType
 */
function inferPathTypeFromObjectName(objectName) {
  if (!objectName) return 'merchant';
  
  // 检查路径前缀，推断pathType
  const pathPrefixes = {
    'card/': 'card',
    'user/': 'user',
    'member/': 'member',
    'employee/': 'employee',
    'resource/': 'resource',
    'merchant/': 'merchant'
  };
  
  for (const [prefix, pathType] of Object.entries(pathPrefixes)) {
    if (objectName.startsWith(prefix)) {
      return pathType;
    }
  }
  
  return 'merchant'; // 默认
}

/**
 * 检查签名URL缓存是否有效
 * @param {string} objectName - OSS对象路径
 * @returns {string|null} 有效的签名URL或null
 */
function getCachedSignedUrl(objectName) {
  const cached = signedUrlCache[objectName];
  if (!cached) return null;
  
  const now = Math.floor(Date.now() / 1000);
  // 检查是否在有效期内（提前refreshBuffer秒刷新）
  if (cached.expireTime - now > SIGNED_URL_CACHE_CONFIG.refreshBuffer) {
    return cached.url;
  }
  
  return null;
}

/**
 * 缓存签名URL
 * @param {string} objectName - OSS对象路径
 * @param {string} url - 签名URL
 * @param {number} expireTime - 过期时间戳（秒）
 */
function cacheSignedUrl(objectName, url, expireTime) {
  signedUrlCache[objectName] = { url, expireTime };
}

/**
 * 清除签名URL缓存
 * @param {string} objectName - 可选，指定清除哪个objectName的缓存，不传则清除全部
 */
function clearSignedUrlCache(objectName) {
  if (objectName) {
    delete signedUrlCache[objectName];
  } else {
    const keys = Object.keys(signedUrlCache);
    keys.forEach(key => delete signedUrlCache[key]);
  }
}

/**
 * 生成签名URL（用于图片预览）
 * 使用STS凭证在前端计算签名，带缓存机制
 * @param {string} objectName - OSS对象路径
 * @param {number} expires - 有效期（秒），默认1800（30分钟）
 * @returns {Promise<string>} 签名URL
 */
async function generateSignedUrl(objectName, expires = SIGNED_URL_CACHE_CONFIG.defaultExpires) {
  if (!objectName) {
    return '';
  }
  
  // 1. 先检查缓存
  const cachedUrl = getCachedSignedUrl(objectName);
  if (cachedUrl) {
    console.log(`[OSS] 使用缓存的签名URL: ${objectName}`);
    return cachedUrl;
  }
  
  try {
    // 2. 缓存未命中，重新生成
    const pathType = inferPathTypeFromObjectName(objectName);
    const credentials = await getStsCredentials(pathType);
    
    console.log(`[OSS] 生成新的签名URL: objectName=${objectName}, pathType=${pathType}`);
    
    // 计算过期时间戳
    const expireTime = Math.floor(Date.now() / 1000) + expires;
    
    // 构建待签名字符串
    // 格式：HTTP-Verb + "\n" + Content-MD5 + "\n" + Content-Type + "\n" + Expires + "\n" + CanonicalizedOSSHeaders + CanonicalizedResource
    // 重要：当 security-token 作为 URL 参数传递时，必须包含在 CanonicalizedResource 中
    const bucket = credentials.bucket;
    
    // CanonicalizedResource 格式：/{bucket}/{object}?security-token={token}
    let resource = `/${bucket}/${objectName}`;
    if (credentials.securityToken) {
      resource += `?security-token=${credentials.securityToken}`;
    }
    
    // 对于GET请求的签名字符串
    const stringToSign = `GET\n\n\n${expireTime}\n${resource}`;
    
    // 计算签名
    const signature = CryptoJS.HmacSHA1(stringToSign, credentials.accessKeySecret);
    const signatureBase64 = CryptoJS.enc.Base64.stringify(signature);
    
    // 构建签名URL - 使用 bucket + endpoint 或 bucket + oss-region
    // 访问文件需要用 https://{bucket}.{endpoint} 或 https://{bucket}.oss-{region}.aliyuncs.com
    let baseUrl = '';
    if (credentials.endpoint) {
      let endpoint = credentials.endpoint.replace(/^https?:\/\//, '');
      baseUrl = `https://${bucket}.${endpoint}`;
    } else if (credentials.region) {
      baseUrl = `https://${bucket}.oss-${credentials.region}.aliyuncs.com`;
    } else {
      throw new Error('缺少 endpoint 或 region 信息');
    }
    
    // 构建签名URL
    let signedUrl = `${baseUrl}/${objectName}?OSSAccessKeyId=${encodeURIComponent(credentials.accessKeyId)}&Expires=${expireTime}&Signature=${encodeURIComponent(signatureBase64)}`;
    
    // 添加安全令牌参数（STS Token 必须作为 URL 参数传递）
    if (credentials.securityToken) {
      signedUrl += `&security-token=${encodeURIComponent(credentials.securityToken)}`;
    }
    
    // 3. 缓存签名URL
    cacheSignedUrl(objectName, signedUrl, expireTime);
    
    return signedUrl;
    
  } catch (error) {
    console.error('[OSS] 生成签名URL失败:', error);
    // 失败时返回空字符串
    return '';
  }
}

/**
 * 删除OSS文件
 * 使用STS凭证发送DELETE请求到OSS
 * @param {string} objectName - OSS对象路径
 * @returns {Promise<boolean>} 是否成功
 */
async function deleteFromOss(objectName) {
  if (!objectName) {
    return true;
  }
  
  try {
    // 获取STS凭证
    const credentials = await getStsCredentials();
    
    // 计算签名
    const date = new Date().toUTCString();
    const bucket = credentials.bucket;
    const resource = `/${bucket}/${objectName}`;
    
    // 构建待签名字符串
    let stringToSign = `DELETE\n\n\n${date}\n`;
    if (credentials.securityToken) {
      stringToSign += `x-oss-security-token:${credentials.securityToken}\n`;
    }
    stringToSign += resource;
    
    // 计算签名
    const signature = CryptoJS.HmacSHA1(stringToSign, credentials.accessKeySecret);
    const signatureBase64 = CryptoJS.enc.Base64.stringify(signature);
    
    // 构建URL
    const endpoint = credentials.endpoint || `https://${bucket}.${credentials.region}.aliyuncs.com`;
    const baseUrl = endpoint.replace(/\/$/, '');
    const url = `${baseUrl}/${objectName}`;
    
    // 构建请求头
    const headers = {
      'Date': date,
      'Authorization': `OSS ${credentials.accessKeyId}:${signatureBase64}`
    };
    if (credentials.securityToken) {
      headers['x-oss-security-token'] = credentials.securityToken;
    }
    
    // 发送DELETE请求
    return new Promise((resolve) => {
      wx.request({
        url: url,
        method: 'DELETE',
        header: headers,
        success: (res) => {
          if (res.statusCode === 204 || res.statusCode === 200) {
            resolve(true);
          } else {
            resolve(false);
          }
        },
        fail: (err) => {
          resolve(false);
        }
      });
    });
  } catch (error) {
    console.error('[OSS] 文件删除失败:', error);
    return false;
  }
}

/**
 * 选择并上传图片
 * @param {Object} options - 选项
 * @param {string} options.prefix - 文件名前缀
 * @param {string} options.pathType - 路径类型，默认 'merchant'
 * @param {number} options.maxSize - 最大文件大小（MB），默认5
 * @param {Function} options.onProgress - 上传进度回调
 * @returns {Promise<string>} 返回Object路径
 */
async function chooseAndUploadImage(options = {}) {
  const { prefix = 'image', pathType = 'merchant', maxSize = 5, onProgress } = options;
  
  return new Promise((resolve, reject) => {
    wx.chooseMedia({
      count: 1,
      mediaType: ['image'],
      sourceType: ['album', 'camera'],
      sizeType: ['compressed'],
      success: async (res) => {
        const tempFile = res.tempFiles[0];
        
        // 检查文件大小
        if (tempFile.size > maxSize * 1024 * 1024) {
          reject(new Error(`图片大小不能超过${maxSize}MB`));
          return;
        }
        
        // 获取文件扩展名
        const filePath = tempFile.tempFilePath;
        const ext = filePath.substring(filePath.lastIndexOf('.')) || '.jpg';
        
        // 生成文件名
        const fileName = generateFileName(prefix, ext);
        
        try {
          // 上传到OSS（传入 pathType）
          const objectName = await uploadToOss(filePath, fileName, { pathType, onProgress });
          resolve(objectName);
        } catch (error) {
          reject(error);
        }
      },
      fail: (err) => {
        if (err.errMsg.includes('cancel')) {
          reject(new Error('用户取消选择'));
        } else {
          reject(new Error('选择图片失败: ' + err.errMsg));
        }
      }
    });
  });
}

module.exports = {
  // 常量
  VALID_PATH_TYPES,
  
  // 函数
  isValidPathType,
  getStsCredentials,
  clearStsCredentials,
  generateFileName,
  uploadToOss,
  generateSignedUrl,
  deleteFromOss,
  chooseAndUploadImage,
  inferPathTypeFromObjectName,
  clearSignedUrlCache,
  
  // 卡种专用函数
  generateCardFileName,
  generateRandomCode,
  uploadCardImage
};
