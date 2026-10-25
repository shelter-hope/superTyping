package com.typinggame.common.im;

import java.time.Duration;

/** 即时连接在线标记。chat-svc 写入，user-svc 在好友列表里读取。 */
public final class PresenceKeys {
    public static final Duration TTL = Duration.ofSeconds(90);

    private PresenceKeys() {
    }

    public static String online(Integer userId) {
        return "im:online:" + userId;
    }
}
