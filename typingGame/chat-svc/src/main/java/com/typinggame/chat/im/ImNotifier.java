package com.typinggame.chat.im;

import com.typinggame.dto.message.MessageDTO;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 下行事件只写入 Redis im.events。本机和其他 chat-svc 实例的监听器再交给各自的连接。
 * 信封格式：{"targets":[用户id], "payload":{客户端事件}}。
 */
@Component
public class ImNotifier {
    public static final String TYPE_MESSAGE = "MESSAGE";
    public static final String TYPE_READ = "READ";
    public static final String TYPE_RECALL = "RECALL";
    public static final String TYPE_UNREAD = "UNREAD";

    private final ImEventPublisher publisher;

    public ImNotifier(ImEventPublisher publisher) {
        this.publisher = publisher;
    }

    public void pushMessage(Integer toUserId, MessageDTO message) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("type", TYPE_MESSAGE);
        payload.put("message", message);
        publish(List.of(toUserId), payload);
    }

    public void pushRead(Integer toUserId, Integer readerId) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("type", TYPE_READ);
        payload.put("readerId", readerId);
        publish(List.of(toUserId), payload);
    }

    public void pushRecall(Collection<Integer> userIds, String clientMsgId) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("type", TYPE_RECALL);
        payload.put("clientMsgId", clientMsgId);
        publish(userIds, payload);
    }

    public void pushUnread(Integer toUserId, int count) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("type", TYPE_UNREAD);
        payload.put("count", count);
        publish(List.of(toUserId), payload);
    }

    public void publish(Collection<Integer> targets, Map<String, Object> payload) {
        List<Integer> ids = targets.stream().filter(id -> id != null && id > 0).distinct().toList();
        if (ids.isEmpty()) {
            return;
        }
        Map<String, Object> envelope = new LinkedHashMap<>();
        envelope.put("targets", ids);
        envelope.put("payload", payload);
        publisher.publish(envelope);
    }
}
