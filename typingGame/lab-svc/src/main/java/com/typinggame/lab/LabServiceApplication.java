package com.typinggame.lab;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication(scanBasePackages = {"com.typinggame.lab", "com.typinggame.common"})
@EntityScan(basePackages = "com.typinggame.lab.entity")
@EnableJpaRepositories(basePackages = "com.typinggame.lab.repository")
@EnableDiscoveryClient
public class LabServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(LabServiceApplication.class, args);
    }
}
