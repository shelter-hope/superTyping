package com.typinggame.dto.ai;

import lombok.Data;

/**
 * AI 对话请求：用户发送的一条消息，可选指定会话
 */
@Data
public class AiChatRequestDTO {
    /** 用户输入内容 */
    private String message;
    /** 可选：已有会话 ID，不传则使用或创建默认会话 */
    private Integer conversationId;
}
