package com.typinggame.chat.im;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.typinggame.chat.friends.UserSvcFriendsClient;
import com.typinggame.chat.service.MessageService;
import com.typinggame.chat.support.UserProfileClient;
import com.typinggame.common.im.ImContracts;
import com.typinggame.dto.message.MessageDTO;
import com.typinggame.dto.user.UserProfileDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * /ws/im：JWT 握手。解析帧后委托 MessageService / ImEventBus / PresenceStore。
 */
@Slf4j
@Component
public class OvernightImWebSocketHandler extends TextWebSocketHandler {
    static final String LAST_SEEN = "lastSeen";
    /** user_info.status 口径：0 禁用 / 1 正常，null 视为正常。 */
    static final int STATUS_DISABLED = 0;
    private static final String PONG = "{\"type\":\"" + ImContracts.TYPE_PONG + "\"}";

    private final ImSessionRegistry sessionRegistry;
    private final MessageService messageService;
    private final UserProfileClient userProfileClient;
    private final UserSvcFriendsClient userSvcFriendsClient;
    private final ImEventBus imEventBus;
    private final PresenceStore presenceStore;
    private final ObjectMapper objectMapper;
    private final SlidingRateLimiter rateLimiter = new SlidingRateLimiter(5, 1_000);

    public OvernightImWebSocketHandler(ImSessionRegistry sessionRegistry,
                                       MessageService messageService,
                                       UserProfileClient userProfileClient,
                                       UserSvcFriendsClient userSvcFriendsClient,
                                       ImEventBus imEventBus,
                                       PresenceStore presenceStore,
                                       ObjectMapper objectMapper) {
        this.sessionRegistry = sessionRegistry;
        this.messageService = messageService;
        this.userProfileClient = userProfileClient;
        this.userSvcFriendsClient = userSvcFriendsClient;
        this.imEventBus = imEventBus;
        this.presenceStore = presenceStore;
        this.objectMapper = objectMapper;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        Integer userId = userIdOf(session);
        if (!allowed(userId)) {
            close(session);
            return;
        }
        sessionRegistry.add(userId, session);
        session.getAttributes().put(LAST_SEEN, System.currentTimeMillis());
        presenceStore.markOnline(userId);
        publishPresence(userId, true);
        log.debug("IM connected, userId={}, sessionId={}", userId, session.getId());
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        Integer userId = userIdOf(session);
        if (!allowed(userId)) {
            close(session);
            return;
        }
        session.getAttributes().put(LAST_SEEN, System.currentTimeMillis());
        String clientMsgId = null;
        try {
            JsonNode body = objectMapper.readTree(message.getPayload());
            String type = body.path("type").asText("");
            String action = body.path("action").asText("");
            clientMsgId = textOrNull(body, "clientMsgId");
            if (ImContracts.TYPE_PING.equals(type)) {
                presenceStore.markOnline(userId);
                sessionRegistry.sendToSession(userId, session.getId(), PONG);
                return;
            }
            if (ImContracts.ACTION_ACK.equals(action)) {
                return;
            }
            if (!rateLimiter.allow(userId, System.currentTimeMillis())) {
                send(session, error("发送过于频繁", clientMsgId));
                return;
            }
            if (ImContracts.ACTION_TYPING.equals(action)) {
                int receiverId = body.path("receiverId").asInt();
                Map<String, Object> payload = new LinkedHashMap<>();
                payload.put("type", ImContracts.TYPE_TYPING);
                payload.put("userId", userId);
                payload.put("typing", body.path("typing").asBoolean());
                imEventBus.publish(java.util.List.of(receiverId), payload);
                return;
            }
            if (ImContracts.ACTION_CHAT.equals(action)) {
                MessageDTO saved = messageService.sendMessage(
                        userId,
                        body.path("receiverId").asInt(),
                        body.path("content").asText(),
                        body.path("type").asText("text"),
                        clientMsgId);
                Map<String, Object> ack = new LinkedHashMap<>();
                ack.put("type", ImContracts.TYPE_ACK);
                ack.put("clientMsgId", clientMsgId == null ? "" : clientMsgId);
                ack.put("id", saved.getId());
                ack.put("serverSeq", saved.getServerSeq() == null ? 0 : saved.getServerSeq());
                send(session, ack);
            }
        } catch (IllegalArgumentException exception) {
            send(session, error(exception.getMessage(), clientMsgId));
        } catch (Exception exception) {
            log.debug("Ignore malformed IM frame, sessionId={}", session.getId());
        }
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        Integer userId = userIdOf(session);
        if (userId != null) {
            sessionRegistry.remove(userId, session);
            if (!sessionRegistry.online(userId)) {
                markOffline(userId);
            }
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        Integer userId = userIdOf(session);
        if (userId != null) {
            sessionRegistry.remove(userId, session);
            if (!sessionRegistry.online(userId)) {
                markOffline(userId);
            }
        }
    }

    public void sweepIdle(long now, long idleMillis) {
        for (Integer userId : sessionRegistry.userIds()) {
            for (WebSocketSession session : sessionRegistry.sessions(userId)) {
                Object seen = session.getAttributes().get(LAST_SEEN);
                if (seen instanceof Long last && now - last > idleMillis) {
                    close(session);
                }
            }
        }
    }

    private void markOffline(Integer userId) {
        presenceStore.markOffline(userId);
        publishPresence(userId, false);
    }

    private void publishPresence(Integer userId, boolean online) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("type", ImContracts.TYPE_PRESENCE);
        payload.put("userId", userId);
        payload.put("online", online);
        imEventBus.publish(userSvcFriendsClient.findFriendIds(userId), payload);
    }

    private boolean allowed(Integer userId) {
        if (userId == null) {
            return false;
        }
        UserProfileDTO profile = userProfileClient.fetchProfile(userId);
        return profile != null
                && (profile.getStatus() == null || profile.getStatus() != STATUS_DISABLED);
    }

    private Integer userIdOf(WebSocketSession session) {
        Object value = session.getAttributes().get(ImHandshakeInterceptor.USER_ID_ATTR);
        return value instanceof Integer id ? id : null;
    }

    private void send(WebSocketSession session, Map<String, Object> payload) {
        try {
            if (session.isOpen()) {
                session.sendMessage(new TextMessage(objectMapper.writeValueAsString(payload)));
            }
        } catch (Exception exception) {
            log.debug("IM reply failed, sessionId={}", session.getId());
        }
    }

    private void close(WebSocketSession session) {
        try {
            session.close();
        } catch (Exception ignored) {
            // Already closing.
        }
    }

    private static Map<String, Object> error(String message, String clientMsgId) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("type", ImContracts.TYPE_ERROR);
        payload.put("message", message);
        if (clientMsgId != null) {
            payload.put("clientMsgId", clientMsgId);
        }
        return payload;
    }

    private static String textOrNull(JsonNode body, String field) {
        JsonNode node = body.get(field);
        if (node == null || node.isNull()) {
            return null;
        }
        String text = node.asText();
        return text == null || text.isBlank() ? null : text;
    }
}
