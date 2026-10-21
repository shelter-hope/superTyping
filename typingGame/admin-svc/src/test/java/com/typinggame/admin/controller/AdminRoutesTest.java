package com.typinggame.admin.controller;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AdminRoutesTest {

    @Test
    void routesPathsToOwningService() {
        assertEquals("user-svc", AdminRoutes.resolveTargetService("/users/3/status"));
        assertEquals("game-core-svc", AdminRoutes.resolveTargetService("/users/3/game-data"));
        assertEquals("chat-svc", AdminRoutes.resolveTargetService("/system-notifications/5"));
        assertEquals("ai-svc", AdminRoutes.resolveTargetService("/ai-chat-messages"));
        assertEquals("game-core-svc", AdminRoutes.resolveTargetService("/character-rules"));
        assertEquals("game-core-svc", AdminRoutes.resolveTargetService("/config-templates/1"));
    }

    @Test
    void contextPathMatchesServiceName() {
        assertEquals("/chat-svc", AdminRoutes.contextPath("chat-svc"));
        assertThrows(IllegalArgumentException.class, () -> AdminRoutes.contextPath("admin-svc"));
    }
}
