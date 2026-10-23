package com.typinggame.chat.im;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

/**
 * overnight-im：JWT 握手，下行推送；上行仅处理心跳 PING。消息发送仍走 REST。
 */
@Slf4j
@Component
public class OvernightImWebSocketHandler extends TextWebSocketHandler {

    private static final String PONG = "{\"type\":\"PONG\"}";

    private final ImSessionRegistry sessionRegistry;
    private final ObjectMapper objectMapper;

    public OvernightImWebSocketHandler(ImSessionRegistry sessionRegistry, ObjectMapper objectMapper) {
        this.sessionRegistry = sessionRegistry;
        this.objectMapper = objectMapper;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        Integer userId = userIdOf(session);
        if (userId == null) {
            close(session);
            return;
        }
        sessionRegistry.add(userId, session);
        log.debug("Overnight IM connected, userId={}, sessionId={}", userId, session.getId());
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        try {
            JsonNode node = objectMapper.readTree(message.getPayload());
            if ("PING".equals(node.path("type").asText())) {
                Integer userId = userIdOf(session);
                if (userId != null) {
                    sessionRegistry.sendToSession(userId, session.getId(), PONG);
                }
            }
        } catch (Exception e) {
            log.debug("Ignore malformed overnight IM frame, sessionId={}", session.getId());
        }
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        Integer userId = userIdOf(session);
        if (userId != null) {
            sessionRegistry.remove(userId, session);
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        Integer userId = userIdOf(session);
        if (userId != null) {
            sessionRegistry.remove(userId, session);
        }
        log.debug("Overnight IM disconnected, sessionId={}, status={}", session.getId(), status);
    }

    private Integer userIdOf(WebSocketSession session) {
        Object value = session.getAttributes().get(ImHandshakeInterceptor.USER_ID_ATTR);
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
