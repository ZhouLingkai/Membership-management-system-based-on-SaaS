'use strict';

const { parseRequest } = require('/opt/nodejs/HttpUtils');
const { decryptPhone, decryptAES } = require('/opt/nodejs/EncryptUtils');
const argon2 = require('@node-rs/argon2');

async function hashPassword(rawPassword) {
    if (!rawPassword) throw new Error('密码不能为空');
    return argon2.hash(rawPassword, {
        type: argon2.argon2id, timeCost: 2, memoryCost: 65536, parallelism: 1, hashLength: 32, saltLength: 16,
    });
}
const { validatePhone, validatePassword } = require('/opt/nodejs/ValidateUtils');
const { success, fail, error } = require('/opt/nodejs/ResponseUtils');
const ErrorCodes = require('/opt/nodejs/ErrorCodes');

const tcb = require('@cloudbase/node-sdk');
const app = tcb.init({ env: tcb.SYMBOL_CURRENT_ENV });
const db = app.database();

exports.main = async (event, context) => {
    try {
        const { params } = parseRequest(event);
        const { phone: phoneCipher, verifyCode, newPassword: newCipher } = params;

        // ── 参数校验 ────────────────────────────────────────────────────
        if (!phoneCipher || !verifyCode || !newCipher) return fail(ErrorCodes.PARAM_MISSING);

        let phone;
        try { phone = decryptPhone(phoneCipher); } catch (e) { return fail(ErrorCodes.PHONE_FORMAT_INVALID); }
        if (!phone || !validatePhone(phone)) return fail(ErrorCodes.PHONE_FORMAT_INVALID);

        let newPassword;
        try { newPassword = decryptAES(newCipher); } catch (e) { return fail(ErrorCodes.PARAM_INVALID); }
        if (!newPassword) return fail(ErrorCodes.PARAM_MISSING);
        const pwdCheck = validatePassword(newPassword);
        if (!pwdCheck.valid) return fail(ErrorCodes.PASSWORD_WEAK);

        // ── 查询商家 ────────────────────────────────────────────────────
        const merchantRes = await db.collection('merchants').where({ phone }).get();
        const merchant = merchantRes.data && merchantRes.data[0];
        if (!merchant) return fail(ErrorCodes.MERCHANT_NOT_FOUND);

        // ── 验证码校验 ──────────────────────────────────────────────────
        const codeRes = await db.collection('verify_codes').where({ phone }).get();
        const codeRecord = codeRes.data && codeRes.data[0];
        if (!codeRecord || codeRecord.code !== verifyCode ||
            new Date(codeRecord.codeExpireTime) < new Date() || codeRecord.used) {
            return fail(ErrorCodes.VERIFY_CODE_INVALID);
        }
        await db.collection('verify_codes').doc(codeRecord._id).update({ used: true });

        // ── 更新密码和 tokenVersion（全局失效所有登录令牌）────────────
        const hashedNew = await hashPassword(newPassword);
        await db.collection('merchants').doc(merchant._id).update({
            password: hashedNew,
            tokenVersion: merchant.tokenVersion + 1,
            updateTime: new Date(),
        });

        return success({}, '密码重置成功，请重新登录');

    } catch (err) {
        return error(err);
    }
};
