package com.typinggame.chat.im;

import java.util.Set;

public final class MessagePolicy {
    public static final int MAX_LENGTH = 500;
    public static final int TYPE_TEXT = 1;
    public static final int TYPE_EMOJI = 2;
    public static final int TYPE_IMAGE = 5;
    public static final int TYPE_FILE = 6;
    /** 系统私信。2 按表注释留给表情，旧数据里发给自己的 2 仍按系统消息读。 */
    public static final int TYPE_SYSTEM = 9;
    /** 与 {@link com.typinggame.common.im.ImContracts#RECALL_MINUTES} 保持一致。 */
    public static final int RECALL_MINUTES = com.typinggame.common.im.ImContracts.RECALL_MINUTES;
    public static final Set<String> STICKERS = Set.of("smile", "wave", "gg", "fire", "think");

    private MessagePolicy() {
    }

    public static int resolveType(String type) {
        String value = type == null ? "text" : type.trim().toLowerCase();
        return switch (value) {
            case "", "text", "friend", "1" -> TYPE_TEXT;
            case "emoji", "sticker", "2" -> TYPE_EMOJI;
            case "image", "5" -> TYPE_IMAGE;
            case "file", "6" -> TYPE_FILE;
            case "system" -> throw new IllegalArgumentException("不能发送系统消息");
            default -> throw new IllegalArgumentException("不支持的消息类型");
        };
    }

    public static void requireChatType(String type) {
        resolveType(type);
    }

    public static void requireContent(String content) {
        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException("消息不能为空");
        }
        if (content.length() > MAX_LENGTH) {
            throw new IllegalArgumentException("消息不能超过 " + MAX_LENGTH + " 字");
        }
    }

    public static void requireEmoji(String stickerId) {
        if (stickerId == null || !STICKERS.contains(stickerId.trim())) {
            throw new IllegalArgumentException("未知表情");
        }
    }

    public static String typeName(int messageType, boolean selfAddressed) {
        if (messageType == TYPE_SYSTEM || (messageType == TYPE_EMOJI && selfAddressed)) {
            return "system";
        }
        return switch (messageType) {
            case TYPE_EMOJI -> "emoji";
            case TYPE_IMAGE -> "image";
            case TYPE_FILE -> "file";
            default -> "text";
        };
    }

    public static boolean userEnabled(Boolean enabled) {
        return enabled == null || enabled;
    }
}
