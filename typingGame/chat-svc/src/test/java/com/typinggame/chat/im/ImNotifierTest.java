package com.typinggame.chat.im;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.typinggame.dto.message.MessageDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ImNotifierTest {

    private ImSessionRegistry registry;
    private ImNotifier notifier;
    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    @BeforeEach
    void setUp() {
        registry = mock(ImSessionRegistry.class);
        notifier = new ImNotifier(registry, objectMapper);
    }

    @Test
    void pushMessageWrapsDtoInMessageEnvelope() throws Exception {
        when(registry.online(5)).thenReturn(true);
        MessageDTO dto = new MessageDTO();
        dto.setId(42);
        dto.setSenderId(3);
        dto.setReceiverId(5);
        dto.setContent("ping-1");
        dto.setSendTime(LocalDateTime.of(2026, 10, 22, 1, 2, 3));

        notifier.pushMessage(5, dto);

        ArgumentCaptor<String> payload = ArgumentCaptor.forClass(String.class);
        verify(registry).send(eq(5), payload.capture());
        JsonNode event = objectMapper.readTree(payload.getValue());
        assertEquals("MESSAGE", event.get("type").asText());
        assertEquals(42, event.get("message").get("id").asInt());
        assertEquals("ping-1", event.get("message").get("content").asText());
        assertEquals("2026-10-22T01:02:03", event.get("message").get("sendTime").asText());
    }

    @Test
    void pushReadCarriesReaderId() throws Exception {
        when(registry.online(3)).thenReturn(true);

        notifier.pushRead(3, 5);

        ArgumentCaptor<String> payload = ArgumentCaptor.forClass(String.class);
        verify(registry).send(eq(3), payload.capture());
        JsonNode event = objectMapper.readTree(payload.getValue());
        assertEquals("READ", event.get("type").asText());
        assertEquals(5, event.get("readerId").asInt());
    }

    @Test
    void offlineUserIsSkipped() {
        when(registry.online(5)).thenReturn(false);

        notifier.pushRead(5, 3);

        verify(registry, never()).send(anyInt(), anyString());
    }

    @Test
    void registryFailureDoesNotPropagate() {
        when(registry.online(5)).thenReturn(true);
        doThrow(new IllegalStateException("boom")).when(registry).send(eq(5), anyString());

        notifier.pushRead(5, 3);
    }
}
