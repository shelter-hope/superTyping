package com.typinggame.chat.im;

import com.typinggame.chat.repository.UserInfoRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import org.springframework.web.socket.server.HandshakeInterceptor;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Map;

@Configuration
@EnableWebSocket
@EnableScheduling
public class ImWebSocketConfig implements WebSocketConfigurer {
    private final ImWebSocketHandler handler;
    private final WsTicketService tickets;
    private final UserInfoRepository userInfoRepository;
    private final ImSessionRegistry registry;

    public ImWebSocketConfig(ImWebSocketHandler handler, WsTicketService tickets, UserInfoRepository userInfoRepository, ImSessionRegistry registry) {
        this.handler = handler;
        this.tickets = tickets;
        this.userInfoRepository = userInfoRepository;
        this.registry = registry;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(handler, "/ws").addInterceptors(ticketInterceptor()).setAllowedOriginPatterns("*");
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
        handler.sweepIdle(System.currentTimeMillis(), 60_000);
    }

    private void deliverLocal(String payload) {
        try {
            var node = new com.fasterxml.jackson.databind.ObjectMapper().readTree(payload);
            int userId = node.path("userId").asInt(0);
            int receiverId = node.path("receiverId").asInt(0);
            if (userId > 0) registry.send(userId, payload);
            if (receiverId > 0 && receiverId != userId) registry.send(receiverId, payload);
        } catch (Exception ignored) {
            // A malformed broadcast does not drop the listener.
        }
    }

    private HandshakeInterceptor ticketInterceptor() {
        return new HandshakeInterceptor() {
            @Override
            public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response, WebSocketHandler wsHandler, Map<String, Object> attributes) {
                String ticket = UriComponentsBuilder.fromUri(request.getURI()).build().getQueryParams().getFirst("ticket");
                Integer userId = tickets.consume(ticket);
                if (userId == null) return false;
                boolean enabled = userInfoRepository.findById(userId).map(user -> MessagePolicy.userEnabled(user.getEnabled())).orElse(false);
                if (!enabled) return false;
                attributes.put(ImWebSocketHandler.USER_ID, userId);
                return true;
            }

            @Override
            public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response, WebSocketHandler wsHandler, Exception exception) {
            }
        };
    }
}
