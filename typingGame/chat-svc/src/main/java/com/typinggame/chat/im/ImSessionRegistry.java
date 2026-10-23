package com.typinggame.chat.im;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.ConcurrentWebSocketSessionDecorator;

import java.io.IOException;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class ImSessionRegistry {
    private static final int SEND_TIME_LIMIT_MS = 5_000;
    private static final int BUFFER_SIZE_LIMIT = 512 * 1024;

    private final ConcurrentHashMap<Integer, Set<WebSocketSession>> sessions = new ConcurrentHashMap<>();

    public void add(Integer userId, WebSocketSession session) {
        WebSocketSession safe = new ConcurrentWebSocketSessionDecorator(session, SEND_TIME_LIMIT_MS, BUFFER_SIZE_LIMIT);
        sessions.computeIfAbsent(userId, ignored -> ConcurrentHashMap.newKeySet()).add(safe);
    }

    public void remove(Integer userId, WebSocketSession session) {
        Set<WebSocketSession> owned = sessions.get(userId);
        if (owned == null) return;
        owned.removeIf(s -> s.getId().equals(session.getId()));
        if (owned.isEmpty()) sessions.remove(userId, owned);
    }

    public boolean online(Integer userId) {
        Set<WebSocketSession> owned = sessions.get(userId);
        return owned != null && owned.stream().anyMatch(WebSocketSession::isOpen);
    }

    public void send(Integer userId, String payload) {
        Set<WebSocketSession> owned = sessions.get(userId);
        if (owned == null) return;
        for (WebSocketSession session : owned) {
            if (!session.isOpen()) continue;
            try {
                session.sendMessage(new TextMessage(payload));
            } catch (IOException | IllegalStateException e) {
                log.warn("IM push failed, sessionId={}", session.getId(), e);
            }
        }
    }

    public boolean sendToSession(Integer userId, String sessionId, String payload) {
        Set<WebSocketSession> owned = sessions.get(userId);
        if (owned == null) return false;
        for (WebSocketSession session : owned) {
            if (!session.getId().equals(sessionId)) continue;
            if (!session.isOpen()) return false;
            try {
                session.sendMessage(new TextMessage(payload));
                return true;
            } catch (IOException | IllegalStateException e) {
                log.warn("IM session push failed, sessionId={}", sessionId, e);
                return false;
            }
        }
        return false;
    }

    public Set<Integer> userIds() {
        return Set.copyOf(sessions.keySet());
    }

    public Set<WebSocketSession> sessions(Integer userId) {
        return sessions.getOrDefault(userId, Set.of());
    }
}
