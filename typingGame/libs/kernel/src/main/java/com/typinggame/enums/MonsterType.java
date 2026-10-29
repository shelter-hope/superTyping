package com.typinggame.enums;

/**
 * 怪物类型枚举
 */
public enum MonsterType {
    NORMAL("普通型", "无特殊效果，匀速向玩家前进", 1),
    ATTACKER("攻击型", "生成后每5秒新增1个普通小怪，每15秒新增8个单字符普通小怪", 3),
    EVASIVE("回避型", "30%概率规避玩家单次输入的正确字符", 2),
    SMART("智慧型", "下落过程中周围1个普通小怪会向其聚拢", 3);

    public final String name;
    public final String desc;
    public final Integer score;

    MonsterType(String name, String desc, Integer score) {
        this.name = name;
        this.desc = desc;
        this.score = score;
    }
}
