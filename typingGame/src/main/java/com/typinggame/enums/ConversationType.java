package com.typinggame.enums;

import lombok.Getter;

/**
 * 会话类型枚举
 */
@Getter
public enum ConversationType {

    /**
     * 好友一对一单聊
     */
    FRIEND_CHAT(1, "好友会话"),
    /**
     * 管理员一对一单聊
     */
    ADMIN_CHAT(2, "管理员会话"),
    /**
     * 系统通知
     */
    SYSTEM_NOTICE(3, "系统通知");

    private final Integer code;
    private final String desc;

    ConversationType(Integer code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    // 工具方法：根据编码获取枚举
    public static ConversationType getByCode(Integer code) {
        if (code == null) {
            return null;
        }
        for (ConversationType type : values()) {
            if (type.code.equals(code)) {
                return type;
            }
        }
        return null;
    }

}
