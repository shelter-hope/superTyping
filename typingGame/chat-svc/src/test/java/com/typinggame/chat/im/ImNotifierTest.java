package com.typinggame.chat.im;

import com.typinggame.dto.message.MessageDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class ImNotifierTest {

    private ImEventPublisher publisher;
    private ImNotifier notifier;

    @BeforeEach
    void setUp() {
        publisher = mock(ImEventPublisher.class);
        notifier = new ImNotifier(publisher);
    }

    @Test
    void pushMessageWrapsDtoInMessageEnvelope() {
        MessageDTO dto = new MessageDTO();
        dto.setId(42);
        dto.setContent("ping-1");

        notifier.pushMessage(5, dto);

        ArgumentCaptor<Map<String, Object>> envelope = ArgumentCaptor.forClass(Map.class);
        verify(publisher).publish(envelope.capture());
        assertEquals(List.of(5), envelope.getValue().get("targets"));
        @SuppressWarnings("unchecked")
        Map<String, Object> payload = (Map<String, Object>) envelope.getValue().get("payload");
        assertEquals("MESSAGE", payload.get("type"));
        assertEquals(42, ((MessageDTO) payload.get("message")).getId());
    }

    @Test
    void pushReadCarriesReaderId() {
        notifier.pushRead(3, 5);

        ArgumentCaptor<Map<String, Object>> envelope = ArgumentCaptor.forClass(Map.class);
        verify(publisher).publish(envelope.capture());
        assertEquals(List.of(3), envelope.getValue().get("targets"));
        @SuppressWarnings("unchecked")
        Map<String, Object> payload = (Map<String, Object>) envelope.getValue().get("payload");
        assertEquals("READ", payload.get("type"));
        assertEquals(5, payload.get("readerId"));
    }

    @Test
    void nonPositiveTargetIsSkipped() {
        notifier.pushRead(0, 3);
        verify(publisher, never()).publish(any());
    }

    @Test
    void publishFailurePropagates() {
        doThrow(new IllegalStateException("boom")).when(publisher).publish(any());
        assertThrows(IllegalStateException.class, () -> notifier.pushRead(5, 3));
    }
}
