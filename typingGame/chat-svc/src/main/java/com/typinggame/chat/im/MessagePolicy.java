package com.typinggame.chat.im;

public final class MessagePolicy {
    public static final int MAX_LENGTH = 500;

    private MessagePolicy() {
    }

    public static void requireChatType(String type) {
        if (type != null && "system".equalsIgnoreCase(type.trim())) {
            throw new IllegalArgumentException("不能发送系统消息");
        }
    }

    public static void requireContent(String content) {
        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException("消息不能为空");
        }
        if (content.length() > MAX_LENGTH) {
            throw new IllegalArgumentException("消息不能超过 " + MAX_LENGTH + " 字");
        }
    }

    public static boolean userEnabled(Boolean enabled) {
        return enabled == null || enabled;
    }
}
