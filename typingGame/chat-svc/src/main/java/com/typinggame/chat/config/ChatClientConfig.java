package com.typinggame.chat.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
public class ChatClientConfig {
    @Bean
    public RestTemplate chatRestTemplate() {
        return new RestTemplate();
    }
}
