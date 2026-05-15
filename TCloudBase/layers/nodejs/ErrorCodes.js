'use strict';

/**
 * 统一错误码定义
 *
 * 编码规则：
 *   1xxxx  令牌相关
 *   2xxxx  验证码相关
 *   3xxxx  商家账号相关
 *   4xxxx  店铺相关
 *   5xxxx  员工相关
 *   6xxxx  会员相关
 *   7xxxx  会员卡/资产相关
 *   8xxxx  预约相关
 *   9xxxx  通用参数 / 系统
 *
 * HTTP 触发器始终返回 HTTP 200，业务错误通过响应体 code 字段区分。
 */
const ErrorCodes = {

    // ─── 1xxxx 令牌相关 ───────────────────────────────────────────────
    TOKEN_MISSING:            { code: 10001, message: '令牌不存在，请重新登录' },
    TOKEN_EXPIRED:            { code: 10002, message: '令牌已过期，请重新登录' },
    TOKEN_BLACKLISTED:        { code: 10003, message: '令牌已注销，请重新登录' },
    TOKEN_INSUFFICIENT_ROLE:  { code: 10004, message: '令牌角色权限不足' },
    TOKEN_TYPE_MISMATCH:      { code: 10005, message: '令牌类型不符，请使用正确令牌' },
    TOKEN_REFRESH_TOO_EARLY:  { code: 10006, message: '令牌剩余有效期超过10分钟，暂不允许刷新' },
    TOKEN_LOGIN_NO_REFRESH:   { code: 10007, message: '登录令牌不支持刷新' },
    TOKEN_VERSION_MISMATCH:   { code: 10008, message: '登录令牌已失效，请重新登录（账号已在其他设备修改密码或退出）' },
    TOKEN_DEVICE_MISMATCH:    { code: 10009, message: '设备不匹配，请在原设备上操作' },

    // ─── 2xxxx 验证码相关 ─────────────────────────────────────────────
    VERIFY_CODE_INVALID:      { code: 20001, message: '验证码错误或已过期' },
    VERIFY_CODE_SEND_TOO_FAST:{ code: 20002, message: '操作太频繁，请1分钟后再试' },
    VERIFY_CODE_DAILY_LIMIT:  { code: 20003, message: '今日发送次数已达上限（5次）' },
    VERIFY_CODE_WRONG_TYPE:   { code: 20004, message: '验证码类型错误（手机号注册状态与type不匹配）' },

    // ─── 3xxxx 商家账号相关 ───────────────────────────────────────────
    MERCHANT_PHONE_EXISTS:    { code: 30001, message: '手机号已注册，请直接登录' },
    MERCHANT_NOT_FOUND:       { code: 30002, message: '商家账号不存在' },
    MERCHANT_PASSWORD_WRONG:  { code: 30003, message: '密码错误' },
    MERCHANT_ACCOUNT_LOCKED:  { code: 30004, message: '账号已被锁定，请稍后再试' },
    MERCHANT_SND_PSWD_WRONG:  { code: 30005, message: '二级密码错误' },

    // ─── 4xxxx 店铺相关 ───────────────────────────────────────────────
    STORE_NOT_FOUND:          { code: 40001, message: '店铺不存在' },
    STORE_NOT_BELONG:         { code: 40002, message: '店铺不归属于该商家' },
    STORE_LIMIT_EXCEEDED:     { code: 40003, message: '已达店铺数量上限' },

    // ─── 5xxxx 员工相关 ───────────────────────────────────────────────
    STAFF_NOT_FOUND:          { code: 50001, message: '员工账号不存在' },
    STAFF_ACCOUNT_EXISTS:     { code: 50002, message: '员工账号名已存在' },
    STAFF_DISABLED:           { code: 50003, message: '员工账号已被禁用' },
    STAFF_PASSWORD_WRONG:     { code: 50004, message: '员工密码错误' },

    // ─── 6xxxx 会员相关 ───────────────────────────────────────────────
    MEMBER_NOT_FOUND:         { code: 60001, message: '会员不存在' },
    MEMBER_PHONE_EXISTS:      { code: 60002, message: '该手机号在本商家下已注册为会员' },
    MEMBER_STATUS_INVALID:    { code: 60003, message: '会员状态异常，无法执行此操作' },

    // ─── 7xxxx 会员卡/资产相关 ────────────────────────────────────────
    ASSET_INSUFFICIENT:       { code: 70001, message: '余额或时长不足' },
    ASSET_POINTS_INSUFFICIENT:{ code: 70002, message: '积分不足' },

    // ─── 8xxxx 预约相关 ───────────────────────────────────────────────
    RESERVATION_SLOT_FULL:    { code: 80001, message: '该时间段预约已满' },
    RESERVATION_NOT_FOUND:    { code: 80002, message: '预约记录不存在' },
    RESERVATION_CANCEL_DENIED:{ code: 80003, message: '不符合取消规则，无法取消' },

    // ─── 9xxxx 通用参数 / 系统 ────────────────────────────────────────
    PARAM_INVALID:            { code: 90001, message: '请求参数错误' },
    PARAM_MISSING:            { code: 90002, message: '缺少必要参数' },
    PHONE_FORMAT_INVALID:     { code: 90003, message: '手机号格式错误' },
    PASSWORD_WEAK:            { code: 90004, message: '密码强度不足' },
    SYSTEM_ERROR:             { code: 99999, message: '系统异常，请联系技术支持' },
};

module.exports = ErrorCodes;
