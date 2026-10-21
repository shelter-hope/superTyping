package com.typinggame.chat.im;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.ConcurrentWebSocketSessionDecorator;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 在线 IM 连接表：一个用户可同时开多个标签页，按 userId → sessionId 维护。
 */
@Slf4j
@Component
public class ImSessionRegistry {

    private static final int SEND_TIME_LIMIT_MS = 5_000;
    private static final int BUFFER_SIZE_LIMIT = 512 * 1024;

    private final Map<Integer, Map<String, WebSocketSession>> sessionsByUser = new ConcurrentHashMap<>();

    public void register(Integer userId, WebSocketSession session) {
        // WebSocketSession.sendMessage 不是线程安全的，推送与心跳回复可能并发
        WebSocketSession safe = new ConcurrentWebSocketSessionDecorator(session, SEND_TIME_LIMIT_MS, BUFFER_SIZE_LIMIT);
        sessionsByUser.computeIfAbsent(userId, k -> new ConcurrentHashMap<>()).put(session.getId(), safe);
    }

    public void unregister(Integer userId, String sessionId) {
        sessionsByUser.computeIfPresent(userId, (k, sessions) -> {
            sessions.remove(sessionId);
            return sessions.isEmpty() ? null : sessions;
        });
    }

    public boolean isOnline(Integer userId) {
        Map<String, WebSocketSession> sessions = sessionsByUser.get(userId);
        return sessions != null && !sessions.isEmpty();
    }

    /**
     * @return 实际送达的连接数
     */
    public int sendToUser(Integer userId, String payload) {
        Map<String, WebSocketSession> sessions = sessionsByUser.get(userId);
        if (sessions == null || sessions.isEmpty()) {
            return 0;
        }
        int delivered = 0;
        for (WebSocketSession session : sessions.values()) {
            if (send(session, payload)) {
                delivered++;
            } else {
                unregister(userId, session.getId());
            }
        }
        return delivered;
    }

    public boolean sendToSession(Integer userId, String sessionId, String payload) {
        Map<String, WebSocketSession> sessions = sessionsByUser.get(userId);
        WebSocketSession session = sessions == null ? null : sessions.get(sessionId);
        return session != null && send(session, payload);
    }

    private boolean send(WebSocketSession session, String payload) {
        if (!session.isOpen()) {
            return false;
        }
        try {
            session.sendMessage(new TextMessage(payload));
            return true;
        } catch (IOException | IllegalStateException e) {
            log.warn("IM push failed, sessionId={}", session.getId(), e);
            return false;
        }
    }
}
