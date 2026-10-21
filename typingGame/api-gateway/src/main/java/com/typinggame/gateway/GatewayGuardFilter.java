package com.typinggame.gateway;

import com.typinggame.gateway.limit.WindowLimiter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

@Component
public class GatewayGuardFilter implements GlobalFilter, Ordered {
    private final WindowLimiter ipLimiter;
    private final WindowLimiter userLimiter;

    public GatewayGuardFilter(
            @Value("${typing.gateway.ip-per-second:30}") int ipPerSecond,
            @Value("${typing.gateway.user-per-second:20}") int userPerSecond) {
        this.ipLimiter = new WindowLimiter(ipPerSecond, 1000);
        this.userLimiter = new WindowLimiter(userPerSecond, 1000);
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();
        if (path != null && path.contains("/internal/")) {
            exchange.getResponse().setStatusCode(HttpStatus.NOT_FOUND);
            return exchange.getResponse().setComplete();
        }
        long now = System.currentTimeMillis();
        String address = exchange.getRequest().getRemoteAddress() == null
                ? "unknown"
                : String.valueOf(exchange.getRequest().getRemoteAddress().getAddress().getHostAddress());
        if (!ipLimiter.allow(address, now)) {
            exchange.getResponse().setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
            return exchange.getResponse().setComplete();
        }
        String authorization = exchange.getRequest().getHeaders().getFirst("Authorization");
        if (authorization != null && authorization.startsWith("Bearer ") && !userLimiter.allow(digest(authorization.substring(7)), now)) {
            exchange.getResponse().setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
            return exchange.getResponse().setComplete();
        }
        String traceId = exchange.getRequest().getHeaders().getFirst("X-Trace-Id");
        if (traceId == null || traceId.isBlank()) {
            traceId = java.util.UUID.randomUUID().toString();
        }
        exchange.getResponse().getHeaders().set("X-Trace-Id", traceId);
        return chain.filter(exchange);
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }

    private static String digest(String token) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException ex) {
            return "user";
        }
    }
}
