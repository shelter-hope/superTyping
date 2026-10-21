package com.typinggame.user.im;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class FriendEventPublisher {
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public FriendEventPublisher(StringRedisTemplate redisTemplate, ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    public void friendRequest(Integer senderId, Integer receiverId, Integer requestId) {
        Map<String, Object> event = new LinkedHashMap<>();
        event.put("event", "friend_request");
        event.put("userId", receiverId);
        event.put("senderId", senderId);
        event.put("requestId", requestId);
        try {
            redisTemplate.convertAndSend("im.events", objectMapper.writeValueAsString(event));
        } catch (Exception ignored) {
            // The request is already stored; a missed push is recovered by the inbox.
        }
    }
}
