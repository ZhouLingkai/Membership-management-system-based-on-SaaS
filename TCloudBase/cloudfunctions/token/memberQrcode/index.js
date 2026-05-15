'use strict';

const { parseRequest } = require('/opt/nodejs/HttpUtils');
const { verifyToken, signToken } = require('/opt/nodejs/JWTUtils');
const { success, fail, error } = require('/opt/nodejs/ResponseUtils');
const ErrorCodes = require('/opt/nodejs/ErrorCodes');

const tcb = require('@cloudbase/node-sdk');
const app = tcb.init({ env: tcb.SYMBOL_CURRENT_ENV });
const db = app.database();

exports.main = async (event, context) => {
    try {
        const { headers } = parseRequest(event);

        // ── 层① 无状态校验 ─────────────────────────────────────────────
        const rawToken = headers['authorization'] || '';
        if (!rawToken) return fail(ErrorCodes.TOKEN_MISSING);
        let payload;
        try {
            payload = verifyToken(rawToken);
        } catch (e) {
            return fail(e.name === 'TokenExpiredError' ? ErrorCodes.TOKEN_EXPIRED : ErrorCodes.TOKEN_MISSING);
        }

        // 校验令牌类型：必须是会员业务令牌⑦
        if (payload.role !== 'member') return fail(ErrorCodes.TOKEN_INSUFFICIENT_ROLE);
        if (payload.type !== 'access') return fail(ErrorCodes.TOKEN_TYPE_MISMATCH);

        // ── 层② 黑名单校验 ─────────────────────────────────────────────
        const { total: blacklisted } = await db.collection('token_blacklist')
            .where({ jti: payload.jti }).count();
        if (blacklisted > 0) return fail(ErrorCodes.TOKEN_BLACKLISTED);

        // ── 签发会员二维码令牌⑨（5分钟，单次有效）──────────────────────
        const qrcodeToken = signToken({
            uid: payload.uid,
            role: 'member',
            type: 'qrcode',
            device_id: payload.device_id,
        }, '5m');

        const expireTime = Date.now() + 5 * 60 * 1000;

        return success({
            qrcodeToken: 'Bearer ' + qrcodeToken,
            qrcodeTokenExpireTime: expireTime,
        }, '二维码令牌签发成功');

    } catch (err) {
        return error(err);
    }
};
