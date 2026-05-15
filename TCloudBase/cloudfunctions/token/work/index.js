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

        // 校验令牌类型：必须是商家访问令牌
        if (payload.role !== 'merchant') return fail(ErrorCodes.TOKEN_INSUFFICIENT_ROLE);
        if (payload.type !== 'access') return fail(ErrorCodes.TOKEN_TYPE_MISMATCH);

        // ── 层② 黑名单校验 ─────────────────────────────────────────────
        const { total: blacklisted } = await db.collection('token_blacklist')
            .where({ jti: payload.jti }).count();
        if (blacklisted > 0) return fail(ErrorCodes.TOKEN_BLACKLISTED);

        // ── 参数校验 ────────────────────────────────────────────────────
        const { storeId } = params;
        if (!storeId) return fail(ErrorCodes.PARAM_MISSING);

        // ── 校验 storeId 归属 ───────────────────────────────────────────
        const storeRes = await db.collection('stores').doc(storeId).get();
        const store = storeRes.data;
        if (!store || (Array.isArray(store) ? store.length === 0 : !store._id)) {
            return fail(ErrorCodes.STORE_NOT_FOUND);
        }
        const storeDoc = Array.isArray(store) ? store[0] : store;
        if (storeDoc.merchantId !== payload.merchantId) return fail(ErrorCodes.STORE_NOT_BELONG);

        // ── 签发商家工作令牌③ ───────────────────────────────────────────
        const workToken = signToken({
            uid: payload.uid,
            role: 'merchant',
            type: 'work',
            merchantId: payload.merchantId,
            storeId,
            device_id: payload.device_id,
        }, '4h');

        return success({
            workToken: 'Bearer ' + workToken,
            workTokenExpireTime: Date.now() + 4 * 60 * 60 * 1000,
            storeId,
            merchantId: payload.merchantId,
        }, '工作令牌签发成功');

    } catch (err) {
        return error(err);
    }
};
