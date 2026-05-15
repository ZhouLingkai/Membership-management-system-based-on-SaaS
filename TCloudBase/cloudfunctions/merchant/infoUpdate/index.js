'use strict';

const { parseRequest } = require('/opt/nodejs/HttpUtils');
const { verifyToken } = require('/opt/nodejs/JWTUtils');
const { success, fail, error } = require('/opt/nodejs/ResponseUtils');
const ErrorCodes = require('/opt/nodejs/ErrorCodes');

const tcb = require('@cloudbase/node-sdk');
const app = tcb.init({ env: tcb.SYMBOL_CURRENT_ENV });
const db = app.database();

const ALLOWED_FIELDS = ['merchantName', 'avatar', 'contactEmail', 'merchantIntro'];

exports.main = async (event, context) => {
    try {
        const { headers, params } = parseRequest(event);

        // ── 层① 无状态校验 ─────────────────────────────────────────────
        const rawToken = headers['authorization'] || '';
        if (!rawToken) return fail(ErrorCodes.TOKEN_MISSING);
        let payload;
        try {
            payload = verifyToken(rawToken);
        } catch (e) {
            return fail(e.name === 'TokenExpiredError' ? ErrorCodes.TOKEN_EXPIRED : ErrorCodes.TOKEN_MISSING);
        }
        if (payload.role !== 'merchant') return fail(ErrorCodes.TOKEN_INSUFFICIENT_ROLE);
        if (payload.type !== 'access') return fail(ErrorCodes.TOKEN_TYPE_MISMATCH);

        // ── 层② 黑名单校验（写操作）────────────────────────────────────
        const { total } = await db.collection('token_blacklist')
            .where({ jti: payload.jti }).count();
        if (total > 0) return fail(ErrorCodes.TOKEN_BLACKLISTED);

        // ── 过滤允许修改的字段 ──────────────────────────────────────────
        const updateObj = {};
        for (const field of ALLOWED_FIELDS) {
            if (params[field] !== undefined) updateObj[field] = params[field];
        }
        if (Object.keys(updateObj).length === 0) return fail(ErrorCodes.PARAM_MISSING);
        updateObj.updateTime = new Date();

        await db.collection('merchants').doc(payload.merchantId).update(updateObj);

        return success({}, '商家信息修改成功');

    } catch (err) {
        return error(err);
    }
};
