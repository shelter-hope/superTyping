package com.typinggame.dict;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication(scanBasePackages = {"com.typinggame.dict", "com.typinggame.common"})
@EntityScan(basePackages = "com.typinggame.entity")
@EnableJpaRepositories(basePackages = "com.typinggame.dict.repository")
@EnableDiscoveryClient
public class DictServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(DictServiceApplication.class, args);
    }
}
