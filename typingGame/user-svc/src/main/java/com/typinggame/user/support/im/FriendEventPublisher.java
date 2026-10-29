package com.typinggame.user.support.im;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.typinggame.common.im.ImContracts;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class FriendEventPublisher {
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public FriendEventPublisher(StringRedisTemplate redisTemplate, ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    public void friendRequest(Integer senderId, Integer receiverId, Integer requestId, String applyMessage) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("type", ImContracts.TYPE_FRIEND_REQUEST);
        payload.put("senderId", senderId);
        payload.put("requestId", requestId);
        payload.put("applyMessage", applyMessage);
        Map<String, Object> envelope = new LinkedHashMap<>();
        envelope.put("targets", List.of(receiverId));
        envelope.put("payload", payload);
        try {
            redisTemplate.convertAndSend(ImContracts.CHANNEL, objectMapper.writeValueAsString(envelope));
        } catch (Exception ignored) {
            // The request is already stored; a missed push is recovered by the inbox.
        }
    }
}
