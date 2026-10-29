package com.typinggame.chat.support.im;

/** 在线状态端口。WebSocket 写入，好友列表读取。 */
public interface PresenceStore {
    void markOnline(Integer userId);

    void markOffline(Integer userId);
}
