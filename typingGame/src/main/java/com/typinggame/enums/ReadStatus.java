package com.typinggame.enums;

import lombok.Getter;

/**
 * 消息阅读状态枚举
 */
@Getter
public enum ReadStatus {
    UNREAD(0, "未读"),
    READ(1, "已读");

    private final Integer code;
    private final String desc;

    ReadStatus(Integer code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    // 根据未读数获取状态
    public static ReadStatus getByUnreadCount(Integer unreadCount) {
        return (unreadCount == null || unreadCount == 0) ? READ : UNREAD;
    }
}


