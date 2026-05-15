'use strict';

const crypto = require('crypto');
const { v4: uuidv4 } = require('uuid');
// const argon2 = require('@node-rs/argon2'); // 仅用到的云函数调用

const AES_SECRET_KEY = process.env.AES_SECRET_KEY || 'your-aes-256-cbc-secret-key-base64-here';
const CBC_IV_LENGTH = 16;

/**
 * AES-256-CBC 加密
 * 输出格式：Base64(IV + cipherText)，与旧Java版本完全兼容
 * @param {string} plainText - 明文
 * @returns {string|null} Base64密文，输入为空返回 null
 */
function encryptAES(plainText) {
    if (!plainText) return null;
    const keyBytes = Buffer.from(AES_SECRET_KEY, 'base64');
    const iv = crypto.randomBytes(CBC_IV_LENGTH);
    const cipher = crypto.createCipheriv('aes-256-cbc', keyBytes, iv);
    const encrypted = Buffer.concat([cipher.update(plainText, 'utf8'), cipher.final()]);
    return Buffer.concat([iv, encrypted]).toString('base64');
}

/**
 * AES-256-CBC 解密
 * @param {string} encryptedText - Base64密文（包含IV）
 * @returns {string|null} 明文，输入为空返回 null
 */
function decryptAES(encryptedText) {
    if (!encryptedText) return null;
    const combined = Buffer.from(encryptedText, 'base64');
    if (combined.length <= CBC_IV_LENGTH) throw new Error('密文长度非法，缺少IV或数据内容');
    const iv = combined.slice(0, CBC_IV_LENGTH);
    const cipherText = combined.slice(CBC_IV_LENGTH);
    const keyBytes = Buffer.from(AES_SECRET_KEY, 'base64');
    const decipher = crypto.createDecipheriv('aes-256-cbc', keyBytes, iv);
    return Buffer.concat([decipher.update(cipherText), decipher.final()]).toString('utf8');
}

/**
 * 生成 UUID v4 字符串
 * 用于 jti、集合 _id 生成等场景
 * @returns {string}
 */
function generateUUID() {
    return uuidv4();
}

/**
 * 手机号解密（智能判断）
 * 若输入已是11位纯数字明文则直接返回，否则走 AES 解密
 * @param {string} phoneOrCipher - 手机号明文或 AES 密文
 * @returns {string|null} 11位手机号明文，输入为空返回 null
 */
function decryptPhone(phoneOrCipher) {
    if (!phoneOrCipher) return null;
    if (/^\d{11}$/.test(phoneOrCipher)) return phoneOrCipher;
    return decryptAES(phoneOrCipher);
}

module.exports = { encryptAES, decryptAES, decryptPhone, generateUUID };
