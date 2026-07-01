package com.typinggame.enums;

/**
 * 系统参数类型枚举
 */
public enum ParamType {
    number("数值"),
    string("字符串"),
    _boolean("布尔值"), // boolean is a keyword
    _enum("枚举值"),   // enum is a keyword
    ratio("比例"),
    integer("整数");

    private final String desc;

    ParamType(String desc) {
        this.desc = desc;
    }

    public String getDesc() {
        return desc;
    }
    
    @Override
    public String toString() {
        // Handle special cases to match DB string
        if (this == _boolean) return "boolean";
        if (this == _enum) return "enum";
        return super.toString();
    }
}
