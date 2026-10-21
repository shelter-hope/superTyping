package com.typinggame.chat.im;

import com.typinggame.common.security.JwtTokenProvider;
import com.typinggame.common.security.TokenRevocationService;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.UUID;

@Service
public class WsTicketService {
    private static final Duration TTL = Duration.ofSeconds(30);

    private final StringRedisTemplate redisTemplate;
    private final JwtTokenProvider jwtTokenProvider;
    private final TokenRevocationService tokenRevocationService;

    public WsTicketService(StringRedisTemplate redisTemplate, JwtTokenProvider jwtTokenProvider, TokenRevocationService tokenRevocationService) {
        this.redisTemplate = redisTemplate;
        this.jwtTokenProvider = jwtTokenProvider;
        this.tokenRevocationService = tokenRevocationService;
    }

    public String issue(String accessToken, Integer userId) {
        if (!"access".equals(jwtTokenProvider.getTokenType(accessToken)) || tokenRevocationService.isRevoked(accessToken)) {
            throw new IllegalArgumentException("连接票据需要有效的访问令牌");
        }
        String ticket = UUID.randomUUID().toString();
        redisTemplate.opsForValue().set(key(ticket), String.valueOf(userId), TTL);
        return ticket;
    }

    public Integer consume(String ticket) {
        if (ticket == null || ticket.isBlank() || ticket.contains(".")) {
            return null;
        }
        String value = redisTemplate.opsForValue().getAndDelete(key(ticket));
        if (value == null) return null;
        try {
            return Integer.valueOf(value);
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private static String key(String ticket) {
        return "im:ticket:" + ticket;
    }
}
