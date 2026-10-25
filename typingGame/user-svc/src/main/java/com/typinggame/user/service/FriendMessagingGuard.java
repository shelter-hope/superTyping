package com.typinggame.user.service;

/** 发消息前好友鉴权。归属 user-svc。 */
public interface FriendMessagingGuard {
    /** 非好友或拉黑时抛 IllegalArgumentException。 */
    void assertCanMessage(Integer senderId, Integer receiverId);

    boolean canMessage(Integer senderId, Integer receiverId);
}
