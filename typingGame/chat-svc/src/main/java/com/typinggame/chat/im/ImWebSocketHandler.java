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
 * 服务端只负责下行推送；上行仅处理心跳 PING。消息发送仍走 REST，保证落库与鉴权逻辑只有一份。
 */
@Slf4j
@Component
public class ImWebSocketHandler extends TextWebSocketHandler {

    private static final String PONG = "{\"type\":\"PONG\"}";

    private final ImSessionRegistry sessionRegistry;
    private final ObjectMapper objectMapper;

    public ImWebSocketHandler(ImSessionRegistry sessionRegistry, ObjectMapper objectMapper) {
        this.sessionRegistry = sessionRegistry;
        this.objectMapper = objectMapper;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        Integer userId = userIdOf(session);
        sessionRegistry.register(userId, session);
        log.debug("IM connected, userId={}, sessionId={}", userId, session.getId());
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        try {
            JsonNode node = objectMapper.readTree(message.getPayload());
            if ("PING".equals(node.path("type").asText())) {
                sessionRegistry.sendToSession(userIdOf(session), session.getId(), PONG);
            }
        } catch (Exception e) {
            log.debug("Ignore malformed IM frame, sessionId={}", session.getId());
        }
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        sessionRegistry.unregister(userIdOf(session), session.getId());
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        sessionRegistry.unregister(userIdOf(session), session.getId());
        log.debug("IM disconnected, sessionId={}, status={}", session.getId(), status);
    }

    private Integer userIdOf(WebSocketSession session) {
        return (Integer) session.getAttributes().get(ImHandshakeInterceptor.USER_ID_ATTR);
    }
}
