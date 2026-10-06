package com.typinggame.common.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Date;

@Slf4j
@Service
public class TokenRevocationService {

    private static final String KEY_PREFIX = "auth:revoked:";

    private final StringRedisTemplate redisTemplate;
    private final JwtTokenProvider jwtTokenProvider;

    public TokenRevocationService(StringRedisTemplate redisTemplate, JwtTokenProvider jwtTokenProvider) {
        this.redisTemplate = redisTemplate;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    public void revoke(String token) {
        if (token == null || token.isBlank()) return;
        try {
            String tokenId = jwtTokenProvider.getTokenId(token);
            Date expiration = jwtTokenProvider.getExpiration(token);
            long ttlMillis = expiration.getTime() - System.currentTimeMillis();
            if (tokenId == null || ttlMillis <= 0) return;
            redisTemplate.opsForValue().set(KEY_PREFIX + tokenId, "1", Duration.ofMillis(ttlMillis));
        } catch (Exception ex) {
            log.warn("撤销令牌失败", ex);
        }
    }

    public boolean isRevoked(String token) {
        if (token == null || token.isBlank()) return false;
        try {
            String tokenId = jwtTokenProvider.getTokenId(token);
            return tokenId != null && Boolean.TRUE.equals(redisTemplate.hasKey(KEY_PREFIX + tokenId));
        } catch (Exception ex) {
            log.warn("检查令牌撤销状态失败", ex);
            return false;
        }
    }
}
