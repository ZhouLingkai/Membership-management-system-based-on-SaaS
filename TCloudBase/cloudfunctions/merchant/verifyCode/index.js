'use strict';

const { parseRequest } = require('/opt/nodejs/HttpUtils');
const { decryptPhone } = require('/opt/nodejs/EncryptUtils');
const { validatePhone } = require('/opt/nodejs/ValidateUtils');
const { tomorrowMidnight } = require('/opt/nodejs/DateUtils');
const { success, fail, error } = require('/opt/nodejs/ResponseUtils');
const ErrorCodes = require('/opt/nodejs/ErrorCodes');

const tcb = require('@cloudbase/node-sdk');
const app = tcb.init({ env: tcb.SYMBOL_CURRENT_ENV });
const db = app.database();

exports.main = async (event, context) => {
    try {
        const { params } = parseRequest(event);
        const { phone: phoneCipher } = params;
        const type = Number(params.type);

        // ── 参数校验 ────────────────────────────────────────────────────
        if (!phoneCipher || !params.type) return fail(ErrorCodes.PARAM_MISSING);
        if (type !== 1 && type !== 2) return fail(ErrorCodes.PARAM_INVALID);

        let phone;
        try {
            phone = decryptPhone(phoneCipher);
        } catch (e) {
            return fail(ErrorCodes.PHONE_FORMAT_INVALID);
        }
        if (!phone || !validatePhone(phone)) return fail(ErrorCodes.PHONE_FORMAT_INVALID);

        // ── 校验手机号注册状态 ──────────────────────────────────────────
        const merchantQuery = await db.collection('merchants')
            .where({ phone }).count();
        const exists = merchantQuery.total > 0;
        if (type === 1 && exists) return fail(ErrorCodes.VERIFY_CODE_WRONG_TYPE);
        if (type === 2 && !exists) return fail(ErrorCodes.VERIFY_CODE_WRONG_TYPE);

        // ── 频率与次数校验 ──────────────────────────────────────────────
        const now = Date.now();
        const codeRes = await db.collection('verify_codes').where({ phone }).get();
        const existingRecord = codeRes.data && codeRes.data[0];

        if (existingRecord) {
            // 1 分钟频率限制
            const lastSendTime = existingRecord.lastSendTime
                ? new Date(existingRecord.lastSendTime).getTime() : 0;
            if (now < lastSendTime + 60 * 1000) {
                const remainSeconds = Math.ceil((lastSendTime + 60 * 1000 - now) / 1000);
                return fail(ErrorCodes.VERIFY_CODE_SEND_TOO_FAST, { remainSeconds });
            }
            // 每日 5 次上限
            if (existingRecord.dailyCount >= 5) {
                return fail(ErrorCodes.VERIFY_CODE_DAILY_LIMIT);
            }
        }

        // ── 生成验证码（YYMMDD 格式）────────────────────────────────────
        const d = new Date();
        const yy = String(d.getFullYear()).slice(2);
        const mm = String(d.getMonth() + 1).padStart(2, '0');
        const dd = String(d.getDate()).padStart(2, '0');
        const mockCode = `${yy}${mm}${dd}`;

        const codeExpireTime = new Date(now + 5 * 60 * 1000);
        const lastSendTime = new Date(now);

        if (!existingRecord) {
            // 今日首次：新增
            await db.collection('verify_codes').add({
                phone,
                code: mockCode,
                codeExpireTime,
                used: false,
                dailyCount: 1,
                lastSendTime,
                expireTime: tomorrowMidnight(),
            });
        } else {
            // 非首次：更新
            await db.collection('verify_codes')
                .doc(existingRecord._id)
                .update({
                    code: mockCode,
                    codeExpireTime,
                    used: false,
                    lastSendTime,
                    dailyCount: existingRecord.dailyCount + 1,
                });
        }

        // TODO: 生产环境将此处改为调用真实短信 SDK，发送 mockCode
        return success({}, '验证码已发送');

    } catch (err) {
        return error(err);
    }
};
