package com.typinggame.chat.im;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
    void sendToUserDeliversToEveryOpenTab() throws IOException {
        WebSocketSession tabA = openSession("a");
        WebSocketSession tabB = openSession("b");
        registry.register(7, tabA);
        registry.register(7, tabB);

        int delivered = registry.sendToUser(7, "{\"type\":\"MESSAGE\"}");

        assertEquals(2, delivered);
        verify(tabA).sendMessage(new TextMessage("{\"type\":\"MESSAGE\"}"));
        verify(tabB).sendMessage(new TextMessage("{\"type\":\"MESSAGE\"}"));
    }

    @Test
    void offlineUserReceivesNothing() {
        assertFalse(registry.isOnline(9));
        assertEquals(0, registry.sendToUser(9, "{}"));
    }

    @Test
    void unregisterRemovesUserWhenLastTabCloses() throws IOException {
        WebSocketSession tab = openSession("a");
        registry.register(7, tab);

        registry.unregister(7, "a");

        assertFalse(registry.isOnline(7));
        assertEquals(0, registry.sendToUser(7, "{}"));
        verify(tab, never()).sendMessage(any());
    }

    @Test
    void brokenSessionIsDroppedOnSendFailure() throws IOException {
        WebSocketSession broken = openSession("a");
        doThrow(new IOException("reset")).when(broken).sendMessage(any());
        registry.register(7, broken);

        assertEquals(0, registry.sendToUser(7, "{}"));
        assertFalse(registry.isOnline(7));
    }

    @Test
    void closedSessionIsSkipped() throws IOException {
        WebSocketSession closed = openSession("a");
        when(closed.isOpen()).thenReturn(false);
        registry.register(7, closed);

        assertEquals(0, registry.sendToUser(7, "{}"));
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
