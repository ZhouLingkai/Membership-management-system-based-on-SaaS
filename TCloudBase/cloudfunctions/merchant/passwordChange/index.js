'use strict';

const { parseRequest } = require('/opt/nodejs/HttpUtils');
const { verifyToken } = require('/opt/nodejs/JWTUtils');
const { decryptAES, generateUUID } = require('/opt/nodejs/EncryptUtils');
const argon2 = require('@node-rs/argon2');

async function hashPassword(rawPassword) {
    if (!rawPassword) throw new Error('密码不能为空');
    return argon2.hash(rawPassword, {
        type: argon2.argon2id, timeCost: 2, memoryCost: 65536, parallelism: 1, hashLength: 32, saltLength: 16,
    });
}
async function verifyPassword(rawPassword, hashedPassword) {
    if (!rawPassword || !hashedPassword) return false;
    return argon2.verify(hashedPassword, rawPassword);
}
const { validatePassword } = require('/opt/nodejs/ValidateUtils');
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
        if (payload.role !== 'merchant') return fail(ErrorCodes.TOKEN_INSUFFICIENT_ROLE);
        if (payload.type !== 'access') return fail(ErrorCodes.TOKEN_TYPE_MISMATCH);

        // ── 层② 黑名单校验（改密为敏感写操作）─────────────────────────
        const { total: blacklisted } = await db.collection('token_blacklist')
            .where({ jti: payload.jti }).count();
        if (blacklisted > 0) return fail(ErrorCodes.TOKEN_BLACKLISTED);

        // ── 参数校验 ────────────────────────────────────────────────────
        const { oldPassword: oldCipher, newPassword: newCipher } = params;
        if (!oldCipher || !newCipher) return fail(ErrorCodes.PARAM_MISSING);

        let oldPassword, newPassword;
        try { oldPassword = decryptAES(oldCipher); } catch (e) { return fail(ErrorCodes.PARAM_INVALID); }
        try { newPassword = decryptAES(newCipher); } catch (e) { return fail(ErrorCodes.PARAM_INVALID); }
        if (!oldPassword || !newPassword) return fail(ErrorCodes.PARAM_MISSING);

        const pwdCheck = validatePassword(newPassword);
        if (!pwdCheck.valid) return fail(ErrorCodes.PASSWORD_WEAK);

        // ── 查询商家，验证旧密码 ────────────────────────────────────────
        const res = await db.collection('merchants').doc(payload.uid).get();
        const merchant = res.data && (Array.isArray(res.data) ? res.data[0] : res.data);
        if (!merchant) return fail(ErrorCodes.MERCHANT_NOT_FOUND);

        const pwdOk = await verifyPassword(oldPassword, merchant.password);
        if (!pwdOk) return fail(ErrorCodes.MERCHANT_PASSWORD_WRONG);

        // ── 更新密码和 tokenVersion ─────────────────────────────────────
        const hashedNew = await hashPassword(newPassword);
        await db.collection('merchants').doc(merchant._id).update({
            password: hashedNew,
            tokenVersion: merchant.tokenVersion + 1,
            updateTime: new Date(),
        });

        // ── 将当前 access 令牌写入黑名单 ───────────────────────────────
        await db.collection('token_blacklist').add({
            _id: generateUUID(),
            jti: payload.jti,
            expireTime: new Date(payload.exp * 1000),
        });

        return success({}, '密码修改成功，请重新登录');

    } catch (err) {
        return error(err);
    }
};
