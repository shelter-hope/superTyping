package com.typinggame.chat.support.im;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ImRealtimeTest {
    @Test
    void broadcastsToEveryLocalRegistry() {
        List<String> nodeA = new CopyOnWriteArrayList<>();
        List<String> nodeB = new CopyOnWriteArrayList<>();
        String event = "{\"event\":\"message\",\"text\":\"hi\"}";
        ImFanout.deliver(List.of(nodeA::add, nodeB::add), event);
        assertEquals(List.of(event), nodeA);
        assertEquals(List.of(event), nodeB);
    }

    @Test
    void retriesUnackedPayloadsAndStopsAfterAck() {
        AckTracker tracker = new AckTracker(1_000);
        tracker.expect("m1", "{\"event\":\"ack\",\"clientMsgId\":\"m1\"}", 0);
        assertEquals(1, tracker.due(1_000).size());
        tracker.ack("m1");
        assertTrue(tracker.due(3_000).isEmpty());
    }

    @Test
    void limitsEachUserToTheWindow() {
        SlidingRateLimiter limiter = new SlidingRateLimiter(2, 1_000);
        assertTrue(limiter.allow(7, 0));
        assertTrue(limiter.allow(7, 10));
        assertFalse(limiter.allow(7, 20));
        assertTrue(limiter.allow(7, 1_010));
    }

    @Test
    void rejectsClientSystemMessagesAndBlankContent() {
        assertThrows(IllegalArgumentException.class, () -> MessagePolicy.requireChatType("system"));
        MessagePolicy.requireChatType("text");
        assertThrows(IllegalArgumentException.class, () -> MessagePolicy.requireContent("  "));
        assertFalse(MessagePolicy.userEnabled(false));
        assertTrue(MessagePolicy.userEnabled(null));
        assertEquals("emoji", MessagePolicy.typeName(MessagePolicy.TYPE_EMOJI, false));
        assertEquals("system", MessagePolicy.typeName(MessagePolicy.TYPE_EMOJI, true));
        assertEquals("system", MessagePolicy.typeName(MessagePolicy.TYPE_SYSTEM, false));
        assertThrows(IllegalArgumentException.class, () -> MessagePolicy.requireEmoji("nope"));
    }
}
