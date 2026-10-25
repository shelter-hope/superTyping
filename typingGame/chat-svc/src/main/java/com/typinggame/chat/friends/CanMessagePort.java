package com.typinggame.chat.friends;

/** 发消息前好友鉴权端口。实现可走本地 JPA 或 user-svc 内部 API。 */
public interface CanMessagePort {
    boolean canMessage(Integer senderId, Integer receiverId);
}
