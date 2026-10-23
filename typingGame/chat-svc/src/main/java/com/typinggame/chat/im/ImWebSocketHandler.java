package com.typinggame.chat.im;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.typinggame.chat.repository.UserInfoRepository;
import com.typinggame.chat.service.MessageService;
import com.typinggame.dto.message.MessageDTO;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.Map;

@Component
public class ImWebSocketHandler extends TextWebSocketHandler {
    static final String USER_ID = "userId";

    private final ImSessionRegistry registry;
    private final MessageService messageService;
    private final UserInfoRepository userInfoRepository;
    private final ImEventPublisher publisher;
    private final ObjectMapper objectMapper;
    private final SlidingRateLimiter rateLimiter = new SlidingRateLimiter(5, 1_000);
    private final AckTracker ackTracker = new AckTracker(3_000);

    public ImWebSocketHandler(ImSessionRegistry registry, MessageService messageService, UserInfoRepository userInfoRepository, ImEventPublisher publisher, ObjectMapper objectMapper) {
        this.registry = registry;
        this.messageService = messageService;
        this.userInfoRepository = userInfoRepository;
        this.publisher = publisher;
        this.objectMapper = objectMapper;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        Integer userId = userId(session);
        if (!allowed(userId)) {
            close(session);
            return;
        }
        registry.add(userId, session);
        session.getAttributes().put("lastSeen", System.currentTimeMillis());
        publisher.publish(Map.of("event", "presence", "userId", userId, "online", true));
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        Integer userId = userId(session);
        if (!allowed(userId)) {
            close(session);
            return;
        }
        if (!rateLimiter.allow(userId, System.currentTimeMillis())) {
            session.sendMessage(new TextMessage("{\"event\":\"error\",\"message\":\"发送过于频繁\"}"));
            return;
        }
        session.getAttributes().put("lastSeen", System.currentTimeMillis());
        JsonNode body = objectMapper.readTree(message.getPayload());
        String action = body.path("action").asText("");
        if ("ack".equals(action)) {
            ackTracker.ack(body.path("clientMsgId").asText());
            return;
        }
        if ("typing".equals(action)) {
            publisher.publish(Map.of("event", "typing", "userId", userId, "receiverId", body.path("receiverId").asInt(), "typing", body.path("typing").asBoolean()));
            return;
        }
        if ("chat".equals(action)) {
            String clientMsgId = body.path("clientMsgId").asText(null);
            MessageDTO saved = messageService.sendMessage(userId, body.path("receiverId").asInt(), body.path("content").asText(), body.path("type").asText("text"), clientMsgId);
            String ack = objectMapper.writeValueAsString(Map.of("event", "ack", "clientMsgId", clientMsgId == null ? "" : clientMsgId, "id", saved.getId(), "serverSeq", saved.getServerSeq() == null ? 0 : saved.getServerSeq()));
            ackTracker.expect(clientMsgId, ack, System.currentTimeMillis());
            session.sendMessage(new TextMessage(ack));
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        Integer userId = userId(session);
        if (userId == null) return;
        registry.remove(userId, session);
        if (!registry.online(userId)) {
            publisher.publish(Map.of("event", "presence", "userId", userId, "online", false));
        }
    }

    public void sweepIdle(long now, long idleMillis) {
        for (Integer userId : registry.userIds()) {
            if (!allowed(userId)) {
                registry.sessions(userId).forEach(this::close);
                continue;
            }
            for (WebSocketSession session : registry.sessions(userId)) {
                Object seen = session.getAttributes().get("lastSeen");
                if (seen instanceof Long last && now - last > idleMillis) close(session);
                for (String payload : ackTracker.due(now)) {
                    try {
                        if (session.isOpen()) session.sendMessage(new TextMessage(payload));
                    } catch (Exception ignored) {
                        close(session);
                    }
                }
            }
        }
    }

    private boolean allowed(Integer userId) {
        if (userId == null) return false;
        return userInfoRepository.findById(userId)
                .map(user -> !user.isDisabled())
                .orElse(false);
    }

    private Integer userId(WebSocketSession session) {
        Object value = session.getAttributes().get(USER_ID);
        return value instanceof Integer id ? id : null;
    }

    private void close(WebSocketSession session) {
        try {
            session.close();
        } catch (Exception ignored) {
            // Already closing.
        }
    }
}
