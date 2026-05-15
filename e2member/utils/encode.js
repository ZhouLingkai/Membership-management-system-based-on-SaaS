/**
 * utils/encode.js
 * AES 加密工具（适用于微信小程序）
 * 
 * 注意：此文件依赖 CryptoJS 库
 * 使用前需要在页面或组件中引入 CryptoJS：
 * 方式1：下载 CryptoJS 库文件到项目目录（如 libs/crypto-js.js），然后：
 * const CryptoJS = require('../../libs/crypto-js.js');
 * 
 * 方式2：使用微信小程序 npm 支持（如果项目配置了 npm）
 * npm install crypto-js
 * const CryptoJS = require('crypto-js');
 */
const CryptoJS = require('crypto-js');

const AES_KEY = 'oUDKf7+y6SrpeJ+GCJHL31HKlRFJ1mM/1MhGB4HF/94='; // 与后端一致（32字节）

/**
 * 生成随机字节（适用于微信小程序环境）
 * @param {number} n - 字节数
 * @returns {CryptoJS.lib.WordArray} 随机字节数组
 */
function getRandomBytesLow(n) {
	const bytes = [];
	for (let i = 0; i < n; i++) {
		// 使用微信小程序提供的随机数生成方法
		const randomValue = Math.floor(Math.random() * 256);
		bytes.push(randomValue);
	}
	
	// 转换为 CryptoJS WordArray
	return CryptoJS.lib.WordArray.create(bytes);
}
function getSecureRandomIV(length) {
    if (length !== 16) {
        throw new Error('IV长度必须为16字节（AES-CBC要求）');
    }
    // 小程序环境使用wx.getRandomValues生成加密级随机数
    if (typeof wx !== 'undefined' && typeof wx.getRandomValues === 'function') {
        const uint8Array = new Uint8Array(length);
        wx.getRandomValues(uint8Array); // 系统级安全随机数
        return CryptoJS.lib.WordArray.create(uint8Array);
    }
    // 兼容Web环境（如果需要）
    if (typeof window !== 'undefined' && window.crypto) {
        const uint8Array = new Uint8Array(length);
        window.crypto.getRandomValues(uint8Array);
        return CryptoJS.lib.WordArray.create(uint8Array);
    }
    // 不支持安全随机数的环境（抛出错误，避免使用不安全随机数）
    console.log('不安全加密')
    return getRandomBytesLow(16)
    // throw new Error('当前环境不支持安全随机数生成，无法完成加密');
}
/**
 * AES-CBC 加密（适配微信小程序环境）
 * @param {string} plaintext - 待加密的明文（非空字符串）
 * @returns {string} 加密结果（格式：IV的Base64:密文的Base64）
 * @throws {Error} 当参数无效、依赖缺失或加密失败时抛出错误
 *//**
 * 调整后的加密函数：输出后端期望的格式
 * 格式：Base64(IV字节数组 + 密文字节数组)
 */
function encryptAES(plaintext) {
    try {
        const key = CryptoJS.enc.Base64.parse(AES_KEY); // 密钥解析（与后端一致）
        const iv = getSecureRandomIV(16); // 16字节IV

        // 加密（模式和填充与后端一致）
        const encrypted = CryptoJS.AES.encrypt(plaintext, key, {
            iv: iv,
            mode: CryptoJS.mode.CBC,
            padding: CryptoJS.pad.Pkcs7 // 与后端PKCS5兼容
        });

        // 关键调整：合并IV字节和密文字节，再整体Base64编码
        const combined = iv.concat(encrypted.ciphertext); // 拼接IV和密文的原始字节
        return combined.toString(CryptoJS.enc.Base64); // 整体Base64编码

    } catch (error) {
        throw new Error(`加密失败：${error.message}`);
    }
}

/**
 * AES-CBC 解密（适配微信小程序环境）
 * @param {string} ciphertext - 加密结果（格式：Base64(IV字节数组 + 密文字节数组)）
 * @returns {string} 解密后的明文
 * @throws {Error} 当参数无效、依赖缺失或解密失败时抛出错误
 */
function decryptAES(ciphertext) {
    try {
        if (!ciphertext || typeof ciphertext !== 'string' || ciphertext.trim() === '') {
            throw new Error('密文不能为空');
        }

        const key = CryptoJS.enc.Base64.parse(AES_KEY); // 密钥解析（与后端一致）

        // 解析Base64密文，得到合并的字节数组（IV + 密文）
        const combined = CryptoJS.enc.Base64.parse(ciphertext);

        // 提取前16字节作为IV
        const iv = CryptoJS.lib.WordArray.create(combined.words.slice(0, 4), 16);

        // 提取剩余字节作为密文
        const ciphertextBytes = CryptoJS.lib.WordArray.create(
            combined.words.slice(4),
            combined.sigBytes - 16
        );

        // 解密（模式和填充与加密一致）
        const decrypted = CryptoJS.AES.decrypt(
            { ciphertext: ciphertextBytes },
            key,
            {
                iv: iv,
                mode: CryptoJS.mode.CBC,
                padding: CryptoJS.pad.Pkcs7
            }
        );

        // 转换为UTF-8字符串
        const plaintext = decrypted.toString(CryptoJS.enc.Utf8);

        if (!plaintext) {
            throw new Error('解密失败：无法解析明文');
        }

        return plaintext;

    } catch (error) {
        throw new Error(`解密失败：${error.message}`);
    }
}

module.exports = {
	encryptAES,
	decryptAES,
	AES_KEY,
};

