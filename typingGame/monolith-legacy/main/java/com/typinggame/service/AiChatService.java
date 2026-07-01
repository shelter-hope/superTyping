package com.typinggame.service;

import com.typinggame.dto.ai.AiChatRequestDTO;
import com.typinggame.dto.ai.AiChatResponseDTO;

/**
 * AI 对话服务（方案 A）：会话存储 + 调用 LLM，自动附带训练摘要
 */
public interface AiChatService {

    /**
     * 发送用户消息，写入会话，附带训练摘要调用 LLM，保存助手回复并返回
     *
     * @param userId 当前用户 ID
     * @param request 请求（message 必填，conversationId 可选）
     * @return 助手回复与会话 ID
     */
    AiChatResponseDTO chat(Integer userId, AiChatRequestDTO request);
}
