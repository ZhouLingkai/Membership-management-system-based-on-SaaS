'use strict';

const { parseRequest } = require('/opt/nodejs/HttpUtils');
const { verifyToken } = require('/opt/nodejs/JWTUtils');
const { success, fail, error } = require('/opt/nodejs/ResponseUtils');
const ErrorCodes = require('/opt/nodejs/ErrorCodes');

const tcb = require('@cloudbase/node-sdk');
const app = tcb.init({ env: tcb.SYMBOL_CURRENT_ENV });
const db = app.database();

function maskPhone(phone) {
    if (!phone || phone.length < 11) return phone;
    return phone.slice(0, 3) + '****' + phone.slice(7);
}

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
        if (payload.role !== 'merchant') return fail(ErrorCodes.TOKEN_INSUFFICIENT_ROLE);
        if (payload.type !== 'access') return fail(ErrorCodes.TOKEN_TYPE_MISMATCH);

        // 查询商家信息（读操作，不查黑名单）
        const res = await db.collection('merchants').doc(payload.merchantId).get();
        const doc = res.data && (Array.isArray(res.data) ? res.data[0] : res.data);
        if (!doc) return fail(ErrorCodes.MERCHANT_NOT_FOUND);

        return success({
            merchantId: doc._id,
            phone: maskPhone(doc.phone),
            merchantName: doc.merchantName,
            avatar: doc.avatar || null,
            contactEmail: doc.contactEmail || null,
            merchantIntro: doc.merchantIntro || null,
            merchantLevel: doc.merchantLevel,
            additionalStores: doc.additionalStores ?? 0,
            privilegeExpireTime: doc.privilegeExpireTime || null,
            haveClient: doc.haveClient ?? false,
            createTime: doc.createTime || null,
        }, '查询成功');

    } catch (err) {
        return error(err);
    }
};
