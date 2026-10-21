package com.typinggame.chat.im;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class ImWebSocketConfig implements WebSocketConfigurer {

    public static final String IM_PATH = "/ws/im";

    private final ImWebSocketHandler imWebSocketHandler;
    private final ImHandshakeInterceptor imHandshakeInterceptor;

    public ImWebSocketConfig(ImWebSocketHandler imWebSocketHandler, ImHandshakeInterceptor imHandshakeInterceptor) {
        this.imWebSocketHandler = imWebSocketHandler;
        this.imHandshakeInterceptor = imHandshakeInterceptor;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        // 鉴权靠 JWT，来源限制交给网关 CORS
        registry.addHandler(imWebSocketHandler, IM_PATH)
                .addInterceptors(imHandshakeInterceptor)
                .setAllowedOriginPatterns("*");
    }
}
