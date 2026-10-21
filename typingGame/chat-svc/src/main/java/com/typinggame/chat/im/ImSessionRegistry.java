package com.typinggame.chat.im;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class ImSessionRegistry {
    private final ConcurrentHashMap<Integer, Set<WebSocketSession>> sessions = new ConcurrentHashMap<>();

    public void add(Integer userId, WebSocketSession session) {
        sessions.computeIfAbsent(userId, ignored -> ConcurrentHashMap.newKeySet()).add(session);
    }

    public void remove(Integer userId, WebSocketSession session) {
        Set<WebSocketSession> owned = sessions.get(userId);
        if (owned == null) return;
        owned.remove(session);
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
            } catch (IOException ignored) {
                // The next idle sweep or close frame drops the socket.
            }
        }
    }

    public Set<Integer> userIds() {
        return Set.copyOf(sessions.keySet());
    }

    public Set<WebSocketSession> sessions(Integer userId) {
        return sessions.getOrDefault(userId, Set.of());
    }
}
