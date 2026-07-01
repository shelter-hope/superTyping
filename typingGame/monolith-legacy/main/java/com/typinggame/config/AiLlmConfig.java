package com.typinggame.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

/**
 * AI LLM 调用配置：RestTemplate + 可选 URL/API Key
 */
@Configuration
public class AiLlmConfig {

    @Bean("aiRestTemplate")
    public RestTemplate aiRestTemplate() {
        return new RestTemplate();
    }

    @Bean
    @ConfigurationProperties(prefix = "ai")
    public AiLlmProperties aiLlmProperties() {
        return new AiLlmProperties();
    }

    @lombok.Data
    public static class AiLlmProperties {
        private Llm llm = new Llm();
        private int defaultRobotId = 1;
        private String defaultConversationTitle = "默认";

        @lombok.Data
        public static class Llm {
            private String url;
            private String apiKey;
            /**
             * 可选：第三方平台工作流 ID（如 Coze workflow_id）
             */
            private String workflowId;
        }
    }
}
