package com.typinggame.chat.im;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
@EnableScheduling
public class ImWebSocketConfig implements WebSocketConfigurer {
    public static final String IM_PATH = "/ws/im";

    private final OvernightImWebSocketHandler handler;
    private final ImHandshakeInterceptor imHandshakeInterceptor;
    private final ImSessionRegistry registry;
    private final ObjectMapper objectMapper;

    public ImWebSocketConfig(OvernightImWebSocketHandler handler,
                             ImHandshakeInterceptor imHandshakeInterceptor,
                             ImSessionRegistry registry,
                             ObjectMapper objectMapper) {
        this.handler = handler;
        this.imHandshakeInterceptor = imHandshakeInterceptor;
        this.registry = registry;
        this.objectMapper = objectMapper;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(handler, IM_PATH)
                .addInterceptors(imHandshakeInterceptor)
                .setAllowedOriginPatterns("*");
    }

    @Bean
    public RedisMessageListenerContainer imListener(RedisConnectionFactory connectionFactory) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        container.addMessageListener((message, pattern) -> {
            String payload = new String(message.getBody());
            ImFanout.deliver(java.util.List.of(this::deliverLocal), payload);
        }, new ChannelTopic(ImEventPublisher.CHANNEL));
        return container;
    }

    @Scheduled(fixedDelay = 15_000)
    public void sweep() {
        handler.sweepIdle(System.currentTimeMillis(), 70_000);
    }

    private void deliverLocal(String raw) {
        try {
            JsonNode node = objectMapper.readTree(raw);
            JsonNode targets = node.get("targets");
            JsonNode payload = node.get("payload");
            if (targets == null || payload == null || !targets.isArray()) {
                return;
            }
            String body = objectMapper.writeValueAsString(payload);
            for (JsonNode target : targets) {
                int userId = target.asInt(0);
                if (userId > 0) {
                    registry.send(userId, body);
                }
            }
        } catch (Exception ignored) {
            // A malformed broadcast does not drop the listener.
        }
    }
}
