package com.typinggame.chat.im;

import com.typinggame.common.im.ImContracts;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class ImContractsTest {
    @Test
    void channelAndEventNamesStayStable() {
        assertEquals("im.events", ImContracts.CHANNEL);
        assertEquals("FRIEND_REQUEST", ImContracts.TYPE_FRIEND_REQUEST);
        assertEquals("MESSAGE", ImContracts.TYPE_MESSAGE);
        assertEquals(2, ImContracts.RECALL_MINUTES);
        assertFalse(ImContracts.CHANNEL.isBlank());
    }
}
