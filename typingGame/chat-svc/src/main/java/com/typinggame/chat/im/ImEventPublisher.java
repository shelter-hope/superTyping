package com.typinggame.chat.im;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.typinggame.common.im.ImContracts;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class ImEventPublisher {
    public static final String CHANNEL = ImContracts.CHANNEL;

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public ImEventPublisher(StringRedisTemplate redisTemplate, ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    public void publish(Map<String, Object> event) {
        try {
            String json = objectMapper.writeValueAsString(event);
            redisTemplate.convertAndSend(CHANNEL, json);
        } catch (Exception exception) {
            throw new IllegalStateException("即时消息广播失败", exception);
        }
    }
}
