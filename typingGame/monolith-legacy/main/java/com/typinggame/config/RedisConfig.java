package com.typinggame.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * Redis配置类，用于配置RedisTemplate和其他Redis相关的配置
 */
@Configuration
public class RedisConfig {

    /**
     * 配置RedisTemplate
     * @param connectionFactory Redis连接工厂
     * @return RedisTemplate实例
     */
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        
        // 设置键序列化器
        template.setKeySerializer(new StringRedisSerializer());
        // 设置值序列化器
        template.setValueSerializer(new GenericJackson2JsonRedisSerializer());
        
        // 设置哈希键序列化器
        template.setHashKeySerializer(new StringRedisSerializer());
        // 设置哈希值序列化器
        template.setHashValueSerializer(new GenericJackson2JsonRedisSerializer());
        
        template.afterPropertiesSet();
        return template;
    }
}