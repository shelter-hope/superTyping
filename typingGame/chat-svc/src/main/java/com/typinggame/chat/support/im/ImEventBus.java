package com.typinggame.chat.support.im;

import com.typinggame.dto.message.MessageDTO;

import java.util.Collection;
import java.util.Map;

/** 即时消息下行端口。实现可走 Redis 广播或本地投递。 */
public interface ImEventBus {
    void pushMessage(Integer toUserId, MessageDTO message);

    void pushRead(Integer toUserId, Integer readerId);

    void pushRecall(Collection<Integer> userIds, String clientMsgId);

    void pushUnread(Integer toUserId, int count);

    void publish(Collection<Integer> targets, Map<String, Object> payload);
}
