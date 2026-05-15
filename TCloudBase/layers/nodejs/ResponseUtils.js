'use strict';

const ErrorCodes = require('/opt/nodejs/ErrorCodes');

/**
 * 统一响应工具
 *
 * 云函数 HTTP 触发器始终返回 HTTP 200，业务错误通过响应体 code 字段区分。
 * 所有云函数通过此工具构造最终响应，不允许直接拼裸 JSON 返回。
 *
 * 使用示例：
 *   const { success, fail, failCode } = require('ResponseUtils');
 *
 *   return success({ token: '...' }, '登录成功');
 *   return fail(ErrorCodes.MERCHANT_NOT_FOUND);
 *   return failCode(ErrorCodes.MERCHANT_ACCOUNT_LOCKED, { lockUntil: '...' });
 */

/**
 * 成功响应
 * @param {Object} data    - 业务数据（可为 {} 空对象）
 * @param {string} message - 响应消息，默认 '操作成功'
 * @returns {{ code: 200, data: Object, message: string }}
 */
function success(data = {}, message = '操作成功') {
    return { code: 200, data, message };
}

/**
 * 业务失败响应（使用 ErrorCodes 中预定义的错误项）
 * @param {{ code: number, message: string }} errorItem - ErrorCodes 中的错误项
 * @param {Object} [extra={}] - 附加数据（如剩余秒数、锁定时间等）
 * @returns {{ code: number, data: Object, message: string }}
 */
function fail(errorItem, extra = {}) {
    return { code: errorItem.code, data: extra, message: errorItem.message };
}

/**
 * 系统异常响应（兜底，catch 块使用）
 * @param {Error|string} [err] - 错误对象或消息（仅用于服务端日志，不暴露给前端）
 * @returns {{ code: 99999, data: {}, message: string }}
 */
function error(err) {
    if (err) console.error('[SystemError]', err);
    return { code: ErrorCodes.SYSTEM_ERROR.code, data: {}, message: ErrorCodes.SYSTEM_ERROR.message };
}

module.exports = { success, fail, error };
