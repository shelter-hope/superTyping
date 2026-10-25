package com.typinggame.chat.im;

import com.typinggame.common.im.PresenceKeys;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
public class RedisPresenceStore implements PresenceStore {
    private final StringRedisTemplate redisTemplate;

    public RedisPresenceStore(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public void markOnline(Integer userId) {
        redisTemplate.opsForValue().set(PresenceKeys.online(userId), "1", PresenceKeys.TTL);
    }

    @Override
    public void markOffline(Integer userId) {
        redisTemplate.delete(PresenceKeys.online(userId));
    }
}
