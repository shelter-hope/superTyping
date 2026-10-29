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
        assertEquals("dict-svc", AdminRoutes.resolveTargetService("/system-dictionaries"));
        assertEquals("dict-svc", AdminRoutes.resolveTargetService("/custom-dictionary-words"));
        assertEquals("game-core-svc", AdminRoutes.resolveTargetService("/leaderboard-entries"));
        assertEquals("ai-svc", AdminRoutes.resolveTargetService("/ai-chat-messages"));
        assertEquals("game-core-svc", AdminRoutes.resolveTargetService("/character-rules"));
        assertEquals("game-core-svc", AdminRoutes.resolveTargetService("/config-templates/1"));
        assertEquals("game-core-svc", AdminRoutes.resolveTargetService("/user-game-mode-configs"));
        assertEquals("game-core-svc", AdminRoutes.resolveTargetService("/user-monster-configs"));
        assertEquals("game-core-svc", AdminRoutes.resolveTargetService("/user-model-params"));
        assertEquals("user-svc", AdminRoutes.resolveTargetService("/user-daily-stats"));
    }

    @Test
    void contextPathMatchesServiceName() {
        assertEquals("/chat-svc", AdminRoutes.contextPath("chat-svc"));
        assertEquals("/dict-svc", AdminRoutes.contextPath("dict-svc"));
        assertThrows(IllegalArgumentException.class, () -> AdminRoutes.contextPath("admin-svc"));
    }
}
