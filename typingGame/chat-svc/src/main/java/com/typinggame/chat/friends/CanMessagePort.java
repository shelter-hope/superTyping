package com.typinggame.chat.friends;

/** 发消息前好友鉴权端口。好友关系归 user-svc，实现统一走内部 API。 */
public interface CanMessagePort {
    boolean canMessage(Integer senderId, Integer receiverId);
}
