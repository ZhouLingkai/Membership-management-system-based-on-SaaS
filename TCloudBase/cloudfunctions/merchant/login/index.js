'use strict';

const { parseRequest } = require('/opt/nodejs/HttpUtils');
const { decryptPhone, decryptAES, generateUUID } = require('/opt/nodejs/EncryptUtils');
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
const { validatePhone, validatePassword } = require('/opt/nodejs/ValidateUtils');
const { verifyToken, signToken } = require('/opt/nodejs/JWTUtils');
const { success, fail, error } = require('/opt/nodejs/ResponseUtils');
const ErrorCodes = require('/opt/nodejs/ErrorCodes');

const tcb = require('@cloudbase/node-sdk');
const app = tcb.init({ env: tcb.SYMBOL_CURRENT_ENV });
const db = app.database();

// ── 内部工具：从 merchant 文档组装返回给前端的基础信息 ─────────────
function buildMerchantInfo(doc) {
    return {
        merchantId: doc._id,
        merchantName: doc.merchantName,
        avatar: doc.avatar || null,
        merchantLevel: doc.merchantLevel,
    };
}

// ── 内部工具：按梯度计算锁定截止时间 ──────────────────────────────
function calcLockUntil(failCount) {
    const now = Date.now();
    if (failCount >= 15) return new Date(now + 24 * 60 * 60 * 1000);
    if (failCount >= 10) return new Date(now + 60 * 60 * 1000);
    if (failCount >= 5)  return new Date(now + 15 * 60 * 1000);
    return null;
}

exports.main = async (event, context) => {
    try {
        const { headers, params } = parseRequest(event);
        const { phone: phoneCipher, password: passwordCipher, verifyCode, deviceId } = params;
        const type = Number(params.type);

        if (!deviceId) return fail(ErrorCodes.PARAM_MISSING);
        if (type !== 1 && type !== 2 && type !== 3) return fail(ErrorCodes.PARAM_INVALID);

        // ════════════════════════════════════════════════════════════════
        // type=1：注册
        // ════════════════════════════════════════════════════════════════
        if (type === 1) {
            if (!phoneCipher || !passwordCipher || !verifyCode) return fail(ErrorCodes.PARAM_MISSING);

            let phone;
            try { phone = decryptPhone(phoneCipher); } catch (e) { return fail(ErrorCodes.PHONE_FORMAT_INVALID); }
            if (!phone || !validatePhone(phone)) return fail(ErrorCodes.PHONE_FORMAT_INVALID);

            let rawPassword;
            try { rawPassword = decryptAES(passwordCipher); } catch (e) { return fail(ErrorCodes.PARAM_INVALID); }
            if (!rawPassword) return fail(ErrorCodes.PARAM_MISSING);
            const pwdCheck = validatePassword(rawPassword);
            if (!pwdCheck.valid) return fail(ErrorCodes.PASSWORD_WEAK);

            const existRes = await db.collection('merchants').where({ phone }).count();
            if (existRes.total > 0) return fail(ErrorCodes.MERCHANT_PHONE_EXISTS);

            // 验证码校验
            const codeRes = await db.collection('verify_codes').where({ phone }).get();
            const codeRecord = codeRes.data && codeRes.data[0];
            if (!codeRecord || codeRecord.code !== verifyCode ||
                new Date(codeRecord.codeExpireTime) < new Date() || codeRecord.used) {
                return fail(ErrorCodes.VERIFY_CODE_INVALID);
            }
            await db.collection('verify_codes').doc(codeRecord._id).update({ used: true });

            // 写入 merchants
            const hashedPwd = await hashPassword(rawPassword);
            const merchantId = generateUUID();
            const now = new Date();
            await db.collection('merchants').add({
                _id: merchantId,
                phone,
                password: hashedPwd,
                merchantName: phone.slice(0, 3) + '****' + phone.slice(7),
                avatar: null,
                contactEmail: null,
                merchantIntro: null,
                merchantLevel: 1,
                sndPswd: hashedPwd,
                privilegeExpireTime: null,
                additionalStores: 0,
                haveClient: false,
                tokenVersion: 0,
                createTime: now,
                updateTime: now,
                extJson: {},
            });

            const loginToken = signToken({ uid: merchantId, role: 'merchant', type: 'login', v: 0, device_id: deviceId }, '7d');
            const accessToken = signToken({ uid: merchantId, role: 'merchant', type: 'access', merchantId, device_id: deviceId }, '4h');

            const maskedPhone = phone.slice(0, 3) + '****' + phone.slice(7);
            const now1 = Date.now();
            return success({
                loginToken: 'Bearer ' + loginToken,
                accessToken: 'Bearer ' + accessToken,
                loginTokenExpireTime: now1 + 7 * 24 * 60 * 60 * 1000,
                accessTokenExpireTime: now1 + 4 * 60 * 60 * 1000,
                merchantInfo: { merchantId, merchantName: maskedPhone, avatar: null, merchantLevel: 1 },
            }, '注册成功');
        }

        // ════════════════════════════════════════════════════════════════
        // type=2：账密登录
        // ════════════════════════════════════════════════════════════════
        if (type === 2) {
            if (!phoneCipher || !passwordCipher) return fail(ErrorCodes.PARAM_MISSING);

            let phone;
            try { phone = decryptPhone(phoneCipher); } catch (e) { return fail(ErrorCodes.PHONE_FORMAT_INVALID); }
            if (!phone || !validatePhone(phone)) return fail(ErrorCodes.PHONE_FORMAT_INVALID);

            let rawPassword;
            try { rawPassword = decryptAES(passwordCipher); } catch (e) { return fail(ErrorCodes.PARAM_INVALID); }
            if (!rawPassword) return fail(ErrorCodes.PARAM_MISSING);

            // 锁定检查
            const lockRes = await db.collection('login_fail_records')
                .where({ identifier: phone, role: 'merchant' }).get();
            const lockRecord = lockRes.data && lockRes.data[0];
            if (lockRecord && lockRecord.lockUntil && new Date(lockRecord.lockUntil) > new Date()) {
                return fail(ErrorCodes.MERCHANT_ACCOUNT_LOCKED, { lockUntil: lockRecord.lockUntil });
            }

            // 查商家
            const merchantRes = await db.collection('merchants').where({ phone }).get();
            const merchant = merchantRes.data && merchantRes.data[0];
            if (!merchant) return fail(ErrorCodes.MERCHANT_NOT_FOUND);

            // 验证密码
            const pwdOk = await verifyPassword(rawPassword, merchant.password);
            if (!pwdOk) {
                const newFailCount = lockRecord ? lockRecord.failCount + 1 : 1;
                const lockUntil = calcLockUntil(newFailCount);
                if (lockRecord) {
                    await db.collection('login_fail_records').doc(lockRecord._id).update({
                        failCount: newFailCount,
                        lockUntil,
                        lastFailTime: new Date(),
                        updateTime: new Date(),
                    });
                } else {
                    await db.collection('login_fail_records').add({
                        _id: generateUUID(),
                        role: 'merchant',
                        identifier: phone,
                        failCount: newFailCount,
                        lockUntil,
                        lastFailTime: new Date(),
                        updateTime: new Date(),
                    });
                }
                const remainCount = lockUntil ? 0 : Math.max(0, 5 - newFailCount);
                return fail(ErrorCodes.MERCHANT_PASSWORD_WRONG, { remainCount });
            }

            // 登录成功：清除失败记录
            if (lockRecord) {
                await db.collection('login_fail_records').doc(lockRecord._id).remove();
            }

            const loginToken = signToken({ uid: merchant._id, role: 'merchant', type: 'login', v: merchant.tokenVersion, device_id: deviceId }, '7d');
            const accessToken = signToken({ uid: merchant._id, role: 'merchant', type: 'access', merchantId: merchant._id, device_id: deviceId }, '4h');

            const now2 = Date.now();
            return success({
                loginToken: 'Bearer ' + loginToken,
                accessToken: 'Bearer ' + accessToken,
                loginTokenExpireTime: now2 + 7 * 24 * 60 * 60 * 1000,
                accessTokenExpireTime: now2 + 4 * 60 * 60 * 1000,
                merchantInfo: buildMerchantInfo(merchant),
            }, '登录成功');
        }

        // ════════════════════════════════════════════════════════════════
        // type=3：自动登录（登录令牌换访问令牌）
        // ════════════════════════════════════════════════════════════════
        if (type === 3) {
            const rawToken = headers['authorization'] || '';
            if (!rawToken) return fail(ErrorCodes.TOKEN_MISSING);

            let payload;
            try {
                payload = verifyToken(rawToken);
            } catch (e) {
                return fail(e.name === 'TokenExpiredError' ? ErrorCodes.TOKEN_EXPIRED : ErrorCodes.TOKEN_MISSING);
            }

            if (payload.role !== 'merchant' || payload.type !== 'login') {
                return fail(ErrorCodes.TOKEN_TYPE_MISMATCH);
            }

            // 层② 黑名单校验
            const { total: blacklisted } = await db.collection('token_blacklist')
                .where({ jti: payload.jti }).count();
            if (blacklisted > 0) return fail(ErrorCodes.TOKEN_BLACKLISTED);

            // 层③ tokenVersion 校验
            const merchantDoc = await db.collection('merchants').doc(payload.uid).get();
            const merchant = merchantDoc.data && (Array.isArray(merchantDoc.data) ? merchantDoc.data[0] : merchantDoc.data);
            if (!merchant) return fail(ErrorCodes.MERCHANT_NOT_FOUND);
            if (merchant.tokenVersion !== payload.v) return fail(ErrorCodes.TOKEN_VERSION_MISMATCH);

            const accessToken = signToken({
                uid: merchant._id,
                role: 'merchant',
                type: 'access',
                merchantId: merchant._id,
                device_id: payload.device_id,
            }, '4h');

            return success({
                accessToken: 'Bearer ' + accessToken,
                accessTokenExpireTime: Date.now() + 4 * 60 * 60 * 1000,
                merchantInfo: buildMerchantInfo(merchant),
            }, '自动登录成功');
        }

    } catch (err) {
        return error(err);
    }
};
