'use strict';

const { parseRequest } = require('/opt/nodejs/HttpUtils');
const { verifyToken, decodeToken } = require('/opt/nodejs/JWTUtils');
const { success, fail, error } = require('/opt/nodejs/ResponseUtils');
const ErrorCodes = require('/opt/nodejs/ErrorCodes');
const { generateUUID } = require('/opt/nodejs/EncryptUtils');

const tcb = require('@cloudbase/node-sdk');
const app = tcb.init({ env: tcb.SYMBOL_CURRENT_ENV });
const db = app.database();

exports.main = async (event, context) => {
    try {
        const { headers, params } = parseRequest(event);

        // ── 层① 验证主令牌（用于身份确认）──────────────────────────────
        const rawToken = headers['authorization'] || '';
        if (!rawToken) return fail(ErrorCodes.TOKEN_MISSING);
        try {
            verifyToken(rawToken);
        } catch (e) {
            return fail(e.name === 'TokenExpiredError' ? ErrorCodes.TOKEN_EXPIRED : ErrorCodes.TOKEN_MISSING);
        }

        // ── 参数校验 ────────────────────────────────────────────────────
        const { tokens } = params;
        if (!tokens || !Array.isArray(tokens) || tokens.length === 0) {
            return fail(ErrorCodes.PARAM_MISSING);
        }
        if (tokens.length > 10) return fail(ErrorCodes.PARAM_INVALID);

        // ── 批量写入黑名单（已存在的 jti 幂等跳过）─────────────────────
        let revokedCount = 0;
        for (const t of tokens) {
            const decoded = decodeToken(t);
            if (!decoded || !decoded.jti) continue;

            // 幂等：jti 已在黑名单则跳过
            const { total } = await db.collection('token_blacklist')
                .where({ jti: decoded.jti }).count();
            if (total > 0) continue;

            const expireTime = decoded.exp ? new Date(decoded.exp * 1000) : new Date();
            await db.collection('token_blacklist').add({
                _id: generateUUID(),
                jti: decoded.jti,
                expireTime,
            });
            revokedCount++;
        }

        return success({ revokedCount }, '退出登录成功');

    } catch (err) {
        return error(err);
    }
};
