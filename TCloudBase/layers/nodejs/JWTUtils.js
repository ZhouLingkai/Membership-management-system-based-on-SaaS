'use strict';

const jwt = require('jsonwebtoken');
const { v4: uuidv4 } = require('uuid');

const JWT_SECRET = process.env.JWT_SECRET || 'your-jwt-secret-here';

/**
 * 签发 JWT，自动注入 jti（UUID v4）和 iat
 * @param {Object} payload - 业务字段，见JWT设计文档第三章
 * @param {string|number} expiresIn - 如 '7d'、'4h'、'30d'
 * @returns {string} JWT字符串
 */
function signToken(payload, expiresIn) {
    return jwt.sign(
        { ...payload, jti: uuidv4() },
        JWT_SECRET,
        { algorithm: 'HS256', expiresIn }
    );
}

/**
 * 验证签名合法性 + 是否过期，返回解码后的 payload
 * 只做JWT层校验，不做黑名单和tokenVersion校验（由业务层负责）
 * @param {string} token - JWT字符串（支持携带 Bearer 前缀）
 * @returns {Object} 解码后的 payload
 * @throws {TokenExpiredError | JsonWebTokenError}
 */
function verifyToken(token) {
    const raw = token && token.startsWith('Bearer ') ? token.slice(7).trim() : token;
    return jwt.verify(raw, JWT_SECRET, { algorithms: ['HS256'] });
}

/**
 * 不验证签名，直接解码 payload
 * 用于退出登录时提取 jti 写入黑名单等场景
 * @param {string} token - JWT字符串
 * @returns {Object|null} 解码后的 payload，解码失败返回 null
 */
function decodeToken(token) {
    const raw = token && token.startsWith('Bearer ') ? token.slice(7).trim() : token;
    return jwt.decode(raw);
}

/**
 * 返回令牌剩余有效秒数
 * 用于写入黑名单时设置 expireTime
 * @param {string} token - JWT字符串
 * @returns {number} 剩余秒数，已过期返回 0
 */
function getTokenRemainingSeconds(token) {
    try {
        const decoded = decodeToken(token);
        if (!decoded || !decoded.exp) return 0;
        const remaining = decoded.exp - Math.floor(Date.now() / 1000);
        return remaining > 0 ? remaining : 0;
    } catch {
        return 0;
    }
}

module.exports = { signToken, verifyToken, decodeToken, getTokenRemainingSeconds };
