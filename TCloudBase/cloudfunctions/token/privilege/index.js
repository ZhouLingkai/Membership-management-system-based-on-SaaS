'use strict';

const { parseRequest } = require('/opt/nodejs/HttpUtils');
const { verifyToken, signToken } = require('/opt/nodejs/JWTUtils');
const { decryptAES } = require('/opt/nodejs/EncryptUtils');
const { success, fail, error } = require('/opt/nodejs/ResponseUtils');
const ErrorCodes = require('/opt/nodejs/ErrorCodes');
const argon2 = require('@node-rs/argon2');

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

        // 校验令牌类型：必须是商家访问令牌②
        if (payload.role !== 'merchant') return fail(ErrorCodes.TOKEN_INSUFFICIENT_ROLE);
        if (payload.type !== 'access') return fail(ErrorCodes.TOKEN_TYPE_MISMATCH);

        // ── 层② 黑名单校验 ─────────────────────────────────────────────
        const { total: blacklisted } = await db.collection('token_blacklist')
            .where({ jti: payload.jti }).count();
        if (blacklisted > 0) return fail(ErrorCodes.TOKEN_BLACKLISTED);

        // ── 参数校验 ────────────────────────────────────────────────────
        const { sndPswd: sndPswdCipher } = params;
        if (!sndPswdCipher) return fail(ErrorCodes.PARAM_MISSING);

        // ── 解密二级密码 ────────────────────────────────────────────────
        let rawSndPswd;
        try { rawSndPswd = decryptAES(sndPswdCipher); } catch (e) { return fail(ErrorCodes.PARAM_INVALID); }
        if (!rawSndPswd) return fail(ErrorCodes.PARAM_MISSING);

        // ── 查询商家 ────────────────────────────────────────────────────
        const merchantDoc = await db.collection('merchants').doc(payload.uid).get();
        const merchant = merchantDoc.data && (Array.isArray(merchantDoc.data) ? merchantDoc.data[0] : merchantDoc.data);
        if (!merchant) return fail(ErrorCodes.MERCHANT_NOT_FOUND);

        // ── 校验二级密码 ────────────────────────────────────────────────
        const sndOk = await argon2.verify(merchant.sndPswd, rawSndPswd);
        if (!sndOk) return fail(ErrorCodes.MERCHANT_SND_PSWD_WRONG);

        // ── 签发商家特权令牌⑧（10分钟）────────────────────────────────
        const privilegeToken = signToken({
            uid: payload.uid,
            role: 'merchant',
            type: 'privilege',
            merchantId: payload.merchantId,
            device_id: payload.device_id,
        }, '10m');

        return success({
            privilegeToken: 'Bearer ' + privilegeToken,
            privilegeTokenExpireTime: Date.now() + 10 * 60 * 1000,
        }, '特权令牌签发成功');

    } catch (err) {
        return error(err);
    }
};
