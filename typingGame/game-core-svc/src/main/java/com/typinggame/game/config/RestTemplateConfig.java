package com.typinggame.game.config;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

/**
 * game-core-svc 访问其它服务的 HTTP 客户端。
 * 当前服务间调用一律走服务名（Nacos 名解析由网关/宿主机 DNS 提供），因此这里不引入负载均衡组件。
 */
@Configuration
public class RestTemplateConfig {

    @Bean
    public RestTemplate gameRestTemplate(RestTemplateBuilder builder) {
        return builder
                .setConnectTimeout(Duration.ofSeconds(2))
                .setReadTimeout(Duration.ofSeconds(3))
                .build();
    }
}
