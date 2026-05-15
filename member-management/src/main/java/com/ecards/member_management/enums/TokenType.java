package com.ecards.member_management.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 令牌类型枚举
 */
@Getter
@AllArgsConstructor
public enum TokenType {

    /**
     * 普通令牌（2小时有效期）
     */
    NORMAL(1, "普通令牌", 7200000L),

    /**
     * 特权令牌（5分钟有效期，单次使用）
     */
    PRIVILEGE(2, "特权令牌", 300000L),

    /**
     * 工作令牌（1小时有效期）
     */
    WORK(3, "工作令牌", 3600000L),

    /**
     * 管理令牌（5分钟有效期，最多使用5次）
     */
    MANAGER(4, "管理令牌", 300000L),

    /**
     * 自动登录令牌（7天有效期）
     */
    AUTO_LOGIN(5, "自动登录令牌", 604800000L);

    /**
     * 令牌类型代码
     */
    private final Integer code;

    /**
     * 令牌类型名称
     */
    private final String name;

    /**
     * 默认过期时间（毫秒）
     */
    private final Long defaultExpiration;

    /**
     * 根据代码获取令牌类型
     *
     * @param code 令牌类型代码
     * @return 令牌类型枚举
     */
    public static TokenType fromCode(Integer code) {
        for (TokenType type : values()) {
            if (type.getCode().equals(code)) {
                return type;
            }
        }
        throw new IllegalArgumentException("无效的令牌类型代码: " + code);
    }
}

