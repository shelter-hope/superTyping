package com.typinggame.chat;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication(scanBasePackages = {"com.typinggame.chat", "com.typinggame.common"})
@EntityScan(basePackages = {"com.typinggame.entity", "com.typinggame.chat.entity"})
@EnableJpaRepositories(basePackages = "com.typinggame.chat.repository")
@EnableDiscoveryClient
public class ChatServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(ChatServiceApplication.class, args);
    }
}
