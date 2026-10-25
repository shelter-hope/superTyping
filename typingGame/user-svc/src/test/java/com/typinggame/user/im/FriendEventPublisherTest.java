package com.typinggame.user.im;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.typinggame.common.im.ImContracts;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.redis.core.StringRedisTemplate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class FriendEventPublisherTest {
    @Test
    void publishesEnvelopeOnSharedChannel() throws Exception {
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        ObjectMapper mapper = new ObjectMapper();
        FriendEventPublisher publisher = new FriendEventPublisher(redis, mapper);

        publisher.friendRequest(3, 5, 8, "hi");

        ArgumentCaptor<String> body = ArgumentCaptor.forClass(String.class);
        verify(redis).convertAndSend(eq(ImContracts.CHANNEL), body.capture());
        JsonNode envelope = mapper.readTree(body.getValue());
        assertEquals(5, envelope.get("targets").get(0).asInt());
        assertEquals(ImContracts.TYPE_FRIEND_REQUEST, envelope.get("payload").get("type").asText());
        assertEquals(3, envelope.get("payload").get("senderId").asInt());
        assertEquals("hi", envelope.get("payload").get("applyMessage").asText());
    }
}
