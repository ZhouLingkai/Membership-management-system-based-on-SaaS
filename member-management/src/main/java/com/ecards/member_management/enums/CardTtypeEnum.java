package com.ecards.member_management.enums;

/**
 * 卡种类型枚举
 */
public enum CardTtypeEnum {
    BALANCE(1, "余额卡"),
    TIMES(2, "次数卡"),
    DURATION(3, "时效卡"),
    POINTS(4, "积分卡");

    private final int code;
    private final String name;

    CardTtypeEnum(int code, String name) {
        this.code = code;
        this.name = name;
    }

    public int getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    /**
     * 根据code获取名称
     */
    public static String getNameByCode(int code) {
        for (CardTtypeEnum type : values()) {
            if (type.code == code) {
                return type.name;
            }
        }
        return "未知类型";
    }

    /**
     * 验证code是否有效
     */
    public static boolean isValid(int code) {
        for (CardTtypeEnum type : values()) {
            if (type.code == code) {
                return true;
            }
        }
        return false;
    }
}

