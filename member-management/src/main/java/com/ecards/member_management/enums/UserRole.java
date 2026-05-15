package com.ecards.member_management.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 用户角色枚举
 */
@Getter
@AllArgsConstructor
public enum UserRole {

    /**
     * 普通用户
     */
    USER("USER", "普通用户"),

    /**
     * 商家用户
     */
    MERCHANT("MERCHANT", "商家用户"),

    /**
     * 员工用户（包括店长和店员）
     */
    STAFF("STAFF", "员工用户");

    /**
     * 角色代码
     */
    private final String code;

    /**
     * 角色名称
     */
    private final String name;

    /**
     * 根据代码获取用户角色
     *
     * @param code 角色代码
     * @return 用户角色枚举
     */
    public static UserRole fromCode(String code) {
        for (UserRole role : values()) {
            if (role.getCode().equals(code)) {
                return role;
            }
        }
        throw new IllegalArgumentException("无效的用户角色代码: " + code);
    }

    /**
     * 根据用户类型获取用户角色
     *
     * @param userType 用户类型（1-普通用户，2-商家，3-员工）
     * @return 用户角色枚举
     */
    public static UserRole fromUserType(Integer userType) {
        switch (userType) {
            case 1:
                return USER;
            case 2:
                return MERCHANT;
            case 3:
                return STAFF;
            default:
                throw new IllegalArgumentException("无效的用户类型: " + userType);
        }
    }
}

