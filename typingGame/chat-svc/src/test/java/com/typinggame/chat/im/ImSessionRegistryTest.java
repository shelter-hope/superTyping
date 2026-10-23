package com.typinggame.chat.im;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ImSessionRegistryTest {

    private ImSessionRegistry registry;

    @BeforeEach
    void setUp() {
        registry = new ImSessionRegistry();
    }

    @Test
    void sendDeliversToEveryOpenTab() throws IOException {
        WebSocketSession tabA = openSession("a");
        WebSocketSession tabB = openSession("b");
        registry.add(7, tabA);
        registry.add(7, tabB);

        registry.send(7, "{\"type\":\"MESSAGE\"}");

        verify(tabA).sendMessage(new TextMessage("{\"type\":\"MESSAGE\"}"));
        verify(tabB).sendMessage(new TextMessage("{\"type\":\"MESSAGE\"}"));
    }

    @Test
    void offlineUserReceivesNothing() {
        assertFalse(registry.online(9));
        registry.send(9, "{}");
    }

    @Test
    void removeClearsUserWhenLastTabCloses() throws IOException {
        WebSocketSession tab = openSession("a");
        registry.add(7, tab);

        registry.remove(7, tab);

        assertFalse(registry.online(7));
        registry.send(7, "{}");
        verify(tab, never()).sendMessage(any());
    }

    @Test
    void brokenSessionSurvivesFailedSend() throws IOException {
        WebSocketSession broken = openSession("a");
        doThrow(new IOException("reset")).when(broken).sendMessage(any());
        registry.add(7, broken);

        registry.send(7, "{}");
        assertTrue(registry.online(7));
    }

    @Test
    void closedSessionIsSkipped() throws IOException {
        WebSocketSession closed = openSession("a");
        when(closed.isOpen()).thenReturn(false);
        registry.add(7, closed);

        registry.send(7, "{}");
        verify(closed, never()).sendMessage(any());
        assertFalse(registry.sendToSession(7, "a", "{}"));
    }

    private WebSocketSession openSession(String id) {
        WebSocketSession session = mock(WebSocketSession.class);
        when(session.getId()).thenReturn(id);
        when(session.isOpen()).thenReturn(true);
        return session;
    }
}
