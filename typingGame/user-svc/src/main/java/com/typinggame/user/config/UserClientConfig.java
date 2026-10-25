package com.typinggame.user.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
public class UserClientConfig {
    @Bean
    public RestTemplate userRestTemplate() {
        return new RestTemplate();
    }
}
