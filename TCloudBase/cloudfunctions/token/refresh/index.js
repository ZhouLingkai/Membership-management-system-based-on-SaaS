'use strict';

const { parseRequest } = require('/opt/nodejs/HttpUtils');
const { verifyToken, decodeToken, signToken, getTokenRemainingSeconds } = require('/opt/nodejs/JWTUtils');
const { success, fail, error } = require('/opt/nodejs/ResponseUtils');
const ErrorCodes = require('/opt/nodejs/ErrorCodes');
const { generateUUID } = require('/opt/nodejs/EncryptUtils');

const tcb = require('@cloudbase/node-sdk');
const app = tcb.init({ env: tcb.SYMBOL_CURRENT_ENV });
const db = app.database();

exports.main = async (event, context) => {
    try {
        const { headers } = parseRequest(event);

        // ── 层① 无状态校验（已过期令牌不允许刷新）─────────────────────
        const rawToken = headers['authorization'] || '';
        if (!rawToken) return fail(ErrorCodes.TOKEN_MISSING);
        let payload;
        try {
            payload = verifyToken(rawToken);
        } catch (e) {
            return fail(e.name === 'TokenExpiredError' ? ErrorCodes.TOKEN_EXPIRED : ErrorCodes.TOKEN_MISSING);
        }

        // login 令牌不支持刷新
        if (payload.type === 'login') return fail(ErrorCodes.TOKEN_LOGIN_NO_REFRESH);
        if (payload.type !== 'access' && payload.type !== 'work') {
            return fail(ErrorCodes.TOKEN_TYPE_MISMATCH);
        }

        // ── 剩余有效期校验（> 10 分钟则拒绝）──────────────────────────
        const remainSeconds = getTokenRemainingSeconds(rawToken);
        if (remainSeconds > 600) {
            return fail(ErrorCodes.TOKEN_REFRESH_TOO_EARLY, { remainSeconds });
        }

        // ── 将旧令牌写入黑名单 ─────────────────────────────────────────
        const decodedOld = decodeToken(rawToken);
        const oldJti = decodedOld.jti;
        const expireTime = new Date(decodedOld.exp * 1000);
        await db.collection('token_blacklist').add({
            _id: generateUUID(),
            jti: oldJti,
            expireTime,
        });

        // ── 按原令牌类型重新签发 ────────────────────────────────────────
        let newPayload;
        if (payload.type === 'access') {
            newPayload = {
                uid: payload.uid,
                role: payload.role,
                type: 'access',
                merchantId: payload.merchantId,
                device_id: payload.device_id,
            };
        } else {
            // work 令牌：保留 storeId；员工 work 令牌还保留 permissions
            newPayload = {
                uid: payload.uid,
                role: payload.role,
                type: 'work',
                merchantId: payload.merchantId,
                storeId: payload.storeId,
                device_id: payload.device_id,
            };
            if (payload.permissions) newPayload.permissions = payload.permissions;
        }

        const expiresIn = '4h';
        const newToken = signToken(newPayload, expiresIn);
        const newExpireTime = Date.now() + 4 * 60 * 60 * 1000;

        const responseData = {
            newToken: 'Bearer ' + newToken,
            newExpireTime,
            oldJti,
        };
        if (newPayload.type === 'work' && newPayload.storeId) {
            responseData.storeId = newPayload.storeId;
        }

        return success(responseData, '令牌刷新成功');

    } catch (err) {
        return error(err);
    }
};
