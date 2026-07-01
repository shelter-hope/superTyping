package com.typinggame.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.typinggame.config.AiLlmConfig;
import com.typinggame.dto.ai.AiChatRequestDTO;
import com.typinggame.dto.ai.AiChatResponseDTO;
import com.typinggame.entity.message.AiChatMessage;
import com.typinggame.entity.message.AiConversation;
import com.typinggame.entity.user.UserInfo;
import com.typinggame.repository.AiChatMessageRepository;
import com.typinggame.repository.AiConversationRepository;
import com.typinggame.repository.UserInfoRepository;
import com.typinggame.service.AiChatService;
import com.typinggame.service.AiService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.util.*;

/**
 * AI 对话服务实现：会话存储 + 训练摘要注入 + 调用 LLM（可选）
 */
@Slf4j
@Service
public class AiChatServiceImpl implements AiChatService {

    private static final int ROLE_USER = 1;
    private static final int ROLE_ASSISTANT = 2;
    private static final int TYPE_TEXT = 1;

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
        userMsg.setMessageContent(request.getMessage().trim());
        userMsg.setSendTime(now);
        userMsg.setIsSuccess(true);
        userMsg = messageRepository.save(userMsg);

        String systemPrompt = aiService.getTrainingSummary(userId).getContextForPrompt();
        if (systemPrompt == null) systemPrompt = "你是打字训练助手。";
        String replyContent = callLlm(systemPrompt, request.getMessage().trim());

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

    /**
     * 调用外部 LLM/工作流。
     *
     * 当前实现：按 Coze workflow/run 接口调用：
     * POST https://api.coze.cn/v1/workflow/run
     * {
     *   "workflow_id": "...",
     *   "parameters": { "input": "<系统摘要 + 用户问题>" }
     * }
     */
    private String callLlm(String systemPrompt, String userMessage) {
        String url = aiLlmProperties.getLlm().getUrl();
        if (url == null || url.isBlank()) {
            return "LLM 未配置。请在服务端配置 ai.llm.url（例如 Coze workflow/run 地址）后使用。你可以在「个人页」复制训练摘要到外部 AI 使用。";
        }
        String workflowId = aiLlmProperties.getLlm().getWorkflowId();
        if (workflowId == null || workflowId.isBlank()) {
            return "未配置 workflowId。请在 ai.llm.workflow-id 中配置你的第三方工作流 ID。";
        }
        try {
            // 将训练摘要与用户问题合并为一次输入
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
                    // 尝试从常见字段中提取回答
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
                    // 如果无法识别结构，就直接返回原始 JSON 字符串
                    return respBody;
                } catch (Exception parseEx) {
                    log.warn("解析 LLM 响应失败: {}", parseEx.getMessage());
                    return response.getBody();
                }
            }
            return "LLM 调用失败，HTTP 状态码：" + response.getStatusCodeValue();
        } catch (Exception e) {
            log.warn("LLM 调用失败: {}", e.getMessage());
            return "调用 LLM 时出错：" + e.getMessage() + "。请检查 ai.llm.url 与 api-key 配置或稍后重试。";
        }
    }
}
