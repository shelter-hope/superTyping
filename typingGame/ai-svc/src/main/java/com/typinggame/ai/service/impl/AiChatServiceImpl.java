package com.typinggame.ai.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.typinggame.ai.repository.AiChatMessageRepository;
import com.typinggame.ai.repository.AiConversationRepository;
import com.typinggame.ai.repository.UserInfoRepository;
import com.typinggame.ai.service.AiChatService;
import com.typinggame.ai.service.AiService;
import com.typinggame.dto.ai.AiChatRequestDTO;
import com.typinggame.dto.ai.AiChatResponseDTO;
import com.typinggame.ai.entity.AiChatMessage;
import com.typinggame.ai.entity.AiConversation;
import com.typinggame.entity.user.UserInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.http.client.SimpleClientHttpRequestFactory;

import java.util.*;

@Slf4j
@Service
public class AiChatServiceImpl implements AiChatService {

    private static final int ROLE_USER = 1;
    private static final int ROLE_ASSISTANT = 2;
    private static final int TYPE_TEXT = 1;
    private static final int MAX_MESSAGE_LENGTH = 4000;
    private static final int MAX_REPLY_LENGTH = 12000;

    @Autowired
    private AiConversationRepository conversationRepository;
    @Autowired
    private AiChatMessageRepository messageRepository;
    @Autowired
    private UserInfoRepository userInfoRepository;
    @Autowired
    private AiService aiService;
    @Autowired
    @Qualifier("aiRestTemplate")
    private RestTemplate aiRestTemplate;
    @Autowired
    private AiLlmConfig.AiLlmProperties aiLlmProperties;
    @Autowired
    private ObjectMapper objectMapper;

    @Override
    @Transactional
    public AiChatResponseDTO chat(Integer userId, AiChatRequestDTO request) {
        if (request == null || request.getMessage() == null || request.getMessage().isBlank()) {
            return AiChatResponseDTO.builder()
                    .reply("请输入有效内容。")
                    .conversationId(0)
                    .build();
        }
        String userMessage = request.getMessage().trim();
        if (userMessage.length() > MAX_MESSAGE_LENGTH) {
            return AiChatResponseDTO.builder()
                    .reply("输入内容不能超过" + MAX_MESSAGE_LENGTH + "个字符。")
                    .conversationId(0)
                    .build();
        }
        UserInfo user = userInfoRepository.findByUserId(userId).orElse(null);
        if (user == null) {
            return AiChatResponseDTO.builder().reply("用户不存在。").conversationId(0).build();
        }

        int robotId = aiLlmProperties.getDefaultRobotId();
        String defaultTitle = aiLlmProperties.getDefaultConversationTitle();

        AiConversation conv;
        if (request.getConversationId() != null && request.getConversationId() > 0) {
            conv = conversationRepository.findById(request.getConversationId()).orElse(null);
            if (conv == null || !conv.getUser().getUserId().equals(userId)) {
                return AiChatResponseDTO.builder().reply("会话不存在或无权访问。").conversationId(0).build();
            }
        } else {
            conv = conversationRepository
                    .findFirstByUser_UserIdAndAiRobotIdAndConversationTitleOrderByUpdateTimeDesc(userId, robotId, defaultTitle)
                    .orElse(null);
            if (conv == null) {
                conv = new AiConversation();
                conv.setUser(user);
                conv.setAiRobotId(robotId);
                conv.setConversationTitle(defaultTitle);
                conv.setIsTop(false);
                conv.setIsMute(false);
                conv = conversationRepository.save(conv);
            }
        }

        Date now = new Date();

        AiChatMessage userMsg = new AiChatMessage();
        userMsg.setConversation(conv);
        userMsg.setUser(user);
        userMsg.setAiRobotId(robotId);
        userMsg.setMessageRole(ROLE_USER);
        userMsg.setMessageType(TYPE_TEXT);
        userMsg.setMessageContent(userMessage);
        userMsg.setSendTime(now);
        userMsg.setIsSuccess(true);
        userMsg = messageRepository.save(userMsg);

        String systemPrompt = aiService.getTrainingSummary(userId).getContextForPrompt();
        if (systemPrompt == null) systemPrompt = "你是打字训练助手。";
        String replyContent = callLlm(systemPrompt, userMessage);

        AiChatMessage assistantMsg = new AiChatMessage();
        assistantMsg.setConversation(conv);
        assistantMsg.setUser(user);
        assistantMsg.setAiRobotId(robotId);
        assistantMsg.setMessageRole(ROLE_ASSISTANT);
        assistantMsg.setMessageType(TYPE_TEXT);
        assistantMsg.setMessageContent(replyContent);
        assistantMsg.setSendTime(now);
        assistantMsg.setIsSuccess(true);
        assistantMsg = messageRepository.save(assistantMsg);

        conv.setLastMessageId(assistantMsg.getId());
        conv.setLastMessageTime(now);
        conversationRepository.save(conv);

        return AiChatResponseDTO.builder()
                .reply(replyContent)
                .conversationId(conv.getId())
                .messageId(assistantMsg.getId())
                .build();
    }

    private String callLlm(String systemPrompt, String userMessage) {
        String url = aiLlmProperties.getLlm().getUrl();
        if (url == null || url.isBlank()) {
            return "LLM 未配置。请在服务端配置 ai.llm.url（例如 Coze workflow/run 地址）后使用。";
        }
        String workflowId = aiLlmProperties.getLlm().getWorkflowId();
        if (workflowId == null || workflowId.isBlank()) {
            return "未配置 workflowId。请在 ai.llm.workflow-id 中配置你的第三方工作流 ID。";
        }
        try {
            StringBuilder inputBuilder = new StringBuilder();
            if (systemPrompt != null && !systemPrompt.isBlank()) {
                inputBuilder.append(systemPrompt).append("\n\n");
            }
            inputBuilder.append("用户提问：").append(userMessage);

            Map<String, Object> params = new HashMap<>();
            params.put("input", inputBuilder.toString());

            Map<String, Object> body = new HashMap<>();
            body.put("workflow_id", workflowId);
            body.put("parameters", params);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            String apiKey = aiLlmProperties.getLlm().getApiKey();
            if (apiKey != null && !apiKey.isBlank()) {
                headers.set("Authorization", "Bearer " + apiKey);
            }

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
            ResponseEntity<String> response = aiRestTemplate.exchange(url, HttpMethod.POST, entity, String.class);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                String respBody = response.getBody();
                try {
                    JsonNode root = objectMapper.readTree(respBody);
                    JsonNode dataNode = root.path("data");
                    if (dataNode.isTextual()) {
                        return dataNode.asText();
                    }
                    if (dataNode.isObject()) {
                        if (dataNode.has("output")) {
                            return dataNode.path("output").asText();
                        }
                        if (dataNode.has("answer")) {
                            return dataNode.path("answer").asText();
                        }
                    }
                    if (root.has("output")) {
                        return root.path("output").asText();
                    }
                    if (root.has("result")) {
                        return root.path("result").asText();
                    }
                    return limitReply(respBody);
                } catch (Exception parseEx) {
                    log.warn("解析 LLM 响应失败: {}", parseEx.getMessage());
                    return limitReply(response.getBody());
                }
            }
            return "LLM 调用失败，HTTP 状态码：" + response.getStatusCodeValue();
        } catch (ResourceAccessException e) {
            log.warn("LLM 调用超时或网络不可达: {}", e.getMessage());
            return "LLM 服务暂时不可用，请稍后重试。";
        } catch (Exception e) {
            log.warn("LLM 调用失败: {}", e.getMessage());
            return "LLM 服务调用失败，请稍后重试。";
        }
    }

    private String limitReply(String reply) {
        if (reply == null || reply.isBlank()) {
            return "LLM 未返回有效内容，请稍后重试。";
        }
        return reply.length() > MAX_REPLY_LENGTH ? reply.substring(0, MAX_REPLY_LENGTH) : reply;
    }

    @Slf4j
    @org.springframework.context.annotation.Configuration
    public static class AiLlmConfig {

        @org.springframework.context.annotation.Bean("aiRestTemplate")
        public RestTemplate aiRestTemplate(AiLlmProperties properties) {
            SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
            factory.setConnectTimeout(properties.getLlm().getConnectTimeoutMs());
            factory.setReadTimeout(properties.getLlm().getReadTimeoutMs());
            return new RestTemplate(factory);
        }

        @org.springframework.context.annotation.Bean
        @org.springframework.boot.context.properties.ConfigurationProperties(prefix = "ai")
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
                private String workflowId;
                private int connectTimeoutMs = 3000;
                private int readTimeoutMs = 15000;
            }
        }
    }
}
