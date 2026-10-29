package com.typinggame.enums;

/**
 * 字符类型枚举
 */
public enum CharType {

    lower("小写字母", "a-z"),
    upper("大写字母", "A-Z"),
    number("数字", "0-9"),
    punct("标点符号", ",.!?\"'等"),
    operator("运算符号", "+-*/=<>"),
    chinese("中文字符", "简体中文"),
    special("特殊符号", "@#$%^*等");

    private final String name;
    private final String desc;

    CharType(String name, String desc) {
        this.name = name;
        this.desc = desc;
    }

    public String getName() {
        return name;
    }

    public String getDesc() {
        return desc;
    }
}
