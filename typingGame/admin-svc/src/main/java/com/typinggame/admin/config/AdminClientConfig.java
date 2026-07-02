package com.typinggame.admin.config;

import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
public class AdminClientConfig {
    @Bean
    @LoadBalanced
    public RestTemplate adminRestTemplate() {
        return new RestTemplate();
    }
}
