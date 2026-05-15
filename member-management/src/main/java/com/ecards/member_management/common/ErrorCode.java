package com.ecards.member_management.common;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 错误码枚举类
 * 定义系统中所有的错误码和对应的错误信息
 */
@Getter
@AllArgsConstructor
public enum ErrorCode {

    // ==================== 令牌相关错误码 (10xxx) ====================
    TOKEN_NOT_EXIST(10001, "令牌不存在"),
    TOKEN_EXPIRED(10002, "令牌已过期"),
    TOKEN_BLACKLISTED(10003, "令牌已被加入黑名单"),
    TOKEN_PERMISSION_DENIED(10004, "令牌权限不足"),
    TOKEN_INVALID(10005, "令牌无效"),
    DEVICE_NOT_MATCH(10006, "设备ID不匹配"),
    WORK_TOKEN_VERSION_EXPIRED(10007, "工作令牌版本过期"),
    PRIVILEGE_TOKEN_INVALID(10012, "特权令牌无效或已过期"),
    PRIVILEGE_TOKEN_PERMISSION_DENIED(10013, "特权令牌权限不足（非办卡权限）"),

    // ==================== 用户相关错误码 (20xxx) ====================
    PHONE_ALREADY_REGISTERED(20001, "手机号已注册"),
    VERIFICATION_CODE_INVALID(20002, "验证码无效或已过期"),
    VERIFY_CODE_INVALID(20002, "验证码无效或已过期"),
    USER_NOT_EXIST(20003, "用户信息不存在"),
    PASSWORD_ERROR(20004, "密码错误"),
    PERMISSION_DENIED(20005, "权限不足"),

    // ==================== 商家相关错误码 (30xxx) ====================
    NOT_NORMAL_USER(30001, "非普通用户，无法申请商家"),
    MERCHANT_NOT_EXIST(30002, "商家信息不存在"),
    CDK_INVALID(30003, "CDK无效或已使用"),

    // ==================== 店铺相关错误码 (40xxx) ====================
    NOT_MERCHANT_USER(40001, "非商家用户，无店铺操作权限"),
    STORE_NOT_EXIST(40002, "店铺不存在"),
    SECONDARY_PASSWORD_ERROR(40003, "二级密码错误"),
    STORE_NUMBER_HIT_LIMIT(40004, "店铺数量达到上限"),

    // ==================== 员工相关错误码 (50xxx) ====================
    STAFF_ALREADY_RELATED(50001, "员工已关联该店铺"),
    STAFF_NOT_RELATED(50002, "员工未关联该店铺"),

    // ==================== 会员卡种相关错误码 (60xxx) ====================
    CARD_TYPE_NAME_DUPLICATE(60001, "该店铺下已存在同名卡种"),
    CARD_TYPE_NOT_EXIST(60002, "卡种不存在"),
    CARD_TYPE_STORE_MISMATCH(60003, "卡种与店铺不匹配"),
    CARD_TYPE_HAS_ACTIVE_CARDS(60004, "卡种下存在未注销的会员卡，无法删除"),
    CARD_TYPE_DELETE_CONFIRM_ERROR(60005, "删除确认参数错误（confirm必须为true）"),
    CARD_TYPE_LIMIT_EXCEEDED(60006, "普通商家最多创建3个卡种，请升级VIP"),

    // ==================== 会员卡管理模块错误码 (70xxx) ====================
    MEMBER_CARD_PHONE_DUPLICATE(70001, "该手机号已办理该卡种"),
    MEMBER_CARD_LIMIT_EXCEEDED(70002, "商家会员数量已达上限（普通商家最多200个会员，请开通特权）"),
    MEMBER_CARD_USER_DUPLICATE(70003, "该用户已办理该卡种/您已领取该会员卡"),
    MEMBER_CARD_NOT_EXIST(70004, "会员卡不存在"),
    MEMBER_CARD_PERMISSION_DENIED(70005, "无权查询/操作该会员卡"),
    MEMBER_CARD_NOT_UNACTIVATED(70006, "该会员卡不是未激活状态，无法激活"),
    MEMBER_CARD_PHONE_MISMATCH(70007, "手机号不匹配，无法激活该会员卡"),
    MEMBER_CARD_CANNOT_FREEZE(70008, "该会员卡状态无法冻结"),
    MEMBER_CARD_NOT_FROZEN(70009, "该会员卡不是冻结状态，无法解冻"),
    MEMBER_CARD_CANCELLED(70010, "该会员卡已注销，无法操作"),
    PRIVILEGE_TOKEN_USED(70011, "该二维码已使用，请重新生成"),
    MEMBER_CARD_STATUS_INVALID(70012, "会员卡状态异常，无法操作"),
    POINTS_INSUFFICIENT(70013, "积分不足，无法扣减"),
    POINTS_OVERFLOW(70014, "积分变动超过限制"),
    MULTIPLE_CARDS_FOUND(70015, "该手机号有多张会员卡，请使用会员卡ID查询"),

    // ==================== 交易管理模块错误码 (80xxx) ====================
    BALANCE_INSUFFICIENT(80001, "余额不足，无法消费"),
    TIMES_INSUFFICIENT(80002, "次数不足，无法消费"),
    RECHARGE_AMOUNT_EXCEEDED(80003, "充值金额超过限制（单笔不超过10000元）"),
    CONSUME_AMOUNT_EXCEEDED(80004, "消费金额超过限制（单笔不超过10000元）"),
    CARD_TYPE_NOT_SUPPORT_OPERATION(80005, "该卡种不支持此操作"),
    NOT_TIME_CARD(80006, "该会员卡非时效卡，不支持延期操作"),
    RECHARGE_TYPE_MISMATCH(80007, "充值类型与卡种不匹配"),
    CONSUME_TYPE_MISMATCH(80008, "消费类型与卡种不匹配"),
    MERCHANT_PERMISSION_DENIED(80009, "非商家用户，无权查询商家统计数据"),

    // ==================== 参数相关错误码 (90xxx) ====================
    PARAM_ERROR(90001, "参数错误"),
    
    // ==================== 幂等性相关错误码 (11xxx) ====================
    REQUEST_ID_MISSING(11001, "请求头缺少X-Request-ID"),
    DUPLICATE_REQUEST(11002, "重复请求，请勿重复提交"),
    
    // ==================== 商家认证错误码 ====================
    MERCHANT_NOT_AUTHENTICATED(30004, "商家未认证，无法操作"),
    MERCHANT_NOT_FOUND(30005, "商家信息不存在"),
    CARD_TYPE_NOT_FOUND(60007, "卡种不存在"),
    MEMBER_CARD_NOT_FOUND(70016, "会员卡不存在"),
    STORE_NOT_FOUND(40003, "店铺不存在"),
    TOKEN_PERMISSION_INSUFFICIENT(10014, "令牌权限不足"),

    // ==================== 预约系统错误码 (100xxx) ====================
    RESERVATION_RESOURCE_NOT_FOUND(100001, "预约资源不存在"),
    RESERVATION_TEMPLATE_NOT_FOUND(100012, "预约模板不存在"),
    RESERVATION_TIMESLOT_FORMAT_ERROR(100013, "时间段格式错误"),
    RESERVATION_TIMESLOT_OVERLAP(100014, "时间段存在交叉"),
    RESERVATION_TIMESLOT_ORDER_ERROR(100015, "时间段顺序错误"),
    RESERVATION_RESOURCE_PENDING(100016, "资源存在未完成的预约记录，无法删除"),
    RESERVATION_RESOURCE_NOT_DISABLED_LONG(100017, "资源停用未满7天，无法删除"),
    RESERVATION_TEMPLATE_OVERLAP(100021, "模板时间段重叠"),
    RESERVATION_RESOURCE_NAME_DUPLICATE(100022, "资源名称重复"),
    RESERVATION_BATCH_LIMIT_EXCEEDED(100023, "批量创建数量超过限制"),
    RESERVATION_TEMPLATE_ALREADY_EXISTS(100024, "该店铺已存在模板，无法通过创建接口再次创建"),
    RESERVATION_TIMESLOT_OCCUPIED(100009, "时间段已被占用"),
    RESERVATION_BUSY(100010, "预约繁忙，请稍后重试"),
    RESERVATION_LOCK_ERROR(100011, "预约锁获取失败"),
    RESERVATION_NOT_FOUND(100018, "预约不存在或已取消"),
    RESERVATION_EXPIRED(100019, "预约已开始，不可直接取消，请联系商家处理"),
    RESERVATION_DATE_FORBIDDEN(100020, "该日期不可预约"),

    // ==================== 系统异常 (99999) ====================
    SYSTEM_ERROR(99999, "系统异常");

    /**
     * 错误码
     */
    private final Integer code;

    /**
     * 错误信息
     */
    private final String message;
}

