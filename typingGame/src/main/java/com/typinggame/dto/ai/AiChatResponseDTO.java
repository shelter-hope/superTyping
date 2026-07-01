package com.typinggame.dto.ai;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * AI 对话响应：助手回复与会话信息
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiChatResponseDTO {
    /** 助手回复内容 */
    private String reply;
    /** 会话 ID（新会话或已有） */
    private Integer conversationId;
    /** 本条助手消息 ID（可选） */
    private Integer messageId;
}
