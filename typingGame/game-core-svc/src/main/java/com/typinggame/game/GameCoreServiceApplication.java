package com.typinggame.game;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication(scanBasePackages = {"com.typinggame.game", "com.typinggame.common"})
@EntityScan(basePackages = "com.typinggame.entity")
@EnableJpaRepositories(basePackages = "com.typinggame.game.repository")
@EnableDiscoveryClient
public class GameCoreServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(GameCoreServiceApplication.class, args);
    }
}
