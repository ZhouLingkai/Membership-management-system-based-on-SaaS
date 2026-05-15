'use strict';

const { parseRequest } = require('/opt/nodejs/HttpUtils');
const { verifyToken } = require('/opt/nodejs/JWTUtils');
const { decryptAES } = require('/opt/nodejs/EncryptUtils');
const { validatePassword } = require('/opt/nodejs/ValidateUtils');
const { success, fail, error } = require('/opt/nodejs/ResponseUtils');
const ErrorCodes = require('/opt/nodejs/ErrorCodes');
const argon2 = require('@node-rs/argon2');

async function hashPassword(rawPassword) {
    return argon2.hash(rawPassword, {
        type: argon2.argon2id, timeCost: 2, memoryCost: 65536, parallelism: 1, hashLength: 32, saltLength: 16,
    });
}

const tcb = require('@cloudbase/node-sdk');
const app = tcb.init({ env: tcb.SYMBOL_CURRENT_ENV });
const db = app.database();

exports.main = async (event, context) => {
    try {
        const { headers, params } = parseRequest(event);

        // ── 层① 无状态校验（需已登录，携带商家访问令牌②）──────────────
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

        // ── 层② 黑名单校验 ─────────────────────────────────────────────
        const { total: blacklisted } = await db.collection('token_blacklist')
            .where({ jti: payload.jti }).count();
        if (blacklisted > 0) return fail(ErrorCodes.TOKEN_BLACKLISTED);

        // ── 参数校验 ────────────────────────────────────────────────────
        const { verifyCode, newSndPswd: newCipher } = params;
        if (!verifyCode || !newCipher) return fail(ErrorCodes.PARAM_MISSING);

        // ── 解密新二级密码 ───────────────────────────────────────────────
        let rawNewSndPswd;
        try { rawNewSndPswd = decryptAES(newCipher); } catch (e) { return fail(ErrorCodes.PARAM_INVALID); }
        if (!rawNewSndPswd) return fail(ErrorCodes.PARAM_MISSING);
        const pwdCheck = validatePassword(rawNewSndPswd);
        if (!pwdCheck.valid) return fail(ErrorCodes.PASSWORD_WEAK);

        // ── 查询商家（通过令牌 uid 获取手机号）─────────────────────────
        const merchantDoc = await db.collection('merchants').doc(payload.uid).get();
        const merchant = merchantDoc.data && (Array.isArray(merchantDoc.data) ? merchantDoc.data[0] : merchantDoc.data);
        if (!merchant) return fail(ErrorCodes.MERCHANT_NOT_FOUND);

        // ── 验证码校验（前端需提前用 type=2 调用 verifyCode 接口）───────
        const codeRes = await db.collection('verify_codes').where({ phone: merchant.phone }).get();
        const codeRecord = codeRes.data && codeRes.data[0];
        if (!codeRecord || codeRecord.code !== verifyCode ||
            new Date(codeRecord.codeExpireTime) < new Date() || codeRecord.used) {
            return fail(ErrorCodes.VERIFY_CODE_INVALID);
        }
        await db.collection('verify_codes').doc(codeRecord._id).update({ used: true });

        // ── 哈希并更新二级密码 ──────────────────────────────────────────
        const hashedNew = await hashPassword(rawNewSndPswd);
        await db.collection('merchants').doc(merchant._id).update({
            sndPswd: hashedNew,
            updateTime: new Date(),
        });

        return success({}, '二级密码修改成功');

    } catch (err) {
        return error(err);
    }
};
