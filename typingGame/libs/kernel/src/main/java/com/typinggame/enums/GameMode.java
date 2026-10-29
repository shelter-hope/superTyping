package com.typinggame.enums;

/**
 * 游戏模式（后端与数据库使用的标准枚举）
 *
 * - 枚举名用于数据库字段 `game_mode` 及相关配置表（如 game_mode_configs.mode_name）
 * - 前端/接口层使用的字符串（如 "endless" / "dictionary"）需要通过映射方法转换
 * - 生存挑战模式已从设计中移除，仅保留：无尽适应、词源界定、限时挑战（趣味）
 */
public enum GameMode {
    ENDLESS_ADAPT("无尽适应模式"),
    WORD_SOURCE("词源界定模式"),
    LIMIT_TIME("限时挑战模式");

    private final String desc;

    GameMode(String desc) {
        this.desc = desc;
    }

    public String getDesc() {
        return desc;
    }

    /**
     * 将前端/接口层传入的字符串映射为后端标准枚举
     * 约定：
     * - "endless"      -> ENDLESS_ADAPT
     * - "dictionary"   -> WORD_SOURCE
     * - "limit" 等      -> LIMIT_TIME
     * 其他或为空时默认返回 ENDLESS_ADAPT
     */
    public static GameMode fromClientMode(String mode) {
        if (mode == null) {
            return ENDLESS_ADAPT;
        }
        String m = mode.trim().toLowerCase();
        switch (m) {
            case "endless":
                return ENDLESS_ADAPT;
            case "dictionary":
                return WORD_SOURCE;
            case "limit":
            case "limit_time":
            case "limit-time":
                return LIMIT_TIME;
            case "survival":
            case "survival_challenge":
            case "survival-challenge":
                return ENDLESS_ADAPT;
            default:
                return ENDLESS_ADAPT;
        }
    }

    /**
     * 将后端标准枚举映射为前端/接口层使用的字符串
     * 主要用于建议模式、默认值等返回给前端的场景
     */
    public String toClientMode() {
        switch (this) {
            case ENDLESS_ADAPT:
                return "endless";
            case WORD_SOURCE:
                return "dictionary";
            case LIMIT_TIME:
                return "limit";
            default:
                return "endless";
        }
    }
}
