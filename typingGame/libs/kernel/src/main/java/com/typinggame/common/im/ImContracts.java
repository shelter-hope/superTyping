package com.typinggame.common.im;

/**
 * 跨服务即时通信契约。chat-svc 与 user-svc 共用频道名和事件 type，
 * 避免字面量漂移导致推送静默丢失。
 */
public final class ImContracts {
    public static final String CHANNEL = "im.events";

    public static final String TYPE_MESSAGE = "MESSAGE";
    public static final String TYPE_READ = "READ";
    public static final String TYPE_RECALL = "RECALL";
    public static final String TYPE_UNREAD = "UNREAD";
    public static final String TYPE_TYPING = "TYPING";
    public static final String TYPE_PRESENCE = "PRESENCE";
    public static final String TYPE_FRIEND_REQUEST = "FRIEND_REQUEST";
    public static final String TYPE_ACK = "ACK";
    public static final String TYPE_ERROR = "ERROR";
    public static final String TYPE_PING = "PING";
    public static final String TYPE_PONG = "PONG";

    public static final String ACTION_CHAT = "chat";
    public static final String ACTION_TYPING = "typing";
    public static final String ACTION_ACK = "ack";

    /** 发送方撤回窗口，分钟。前端应使用同一数值。 */
    public static final int RECALL_MINUTES = 2;

    private ImContracts() {
    }
}
