package com.typinggame.service;

import com.typinggame.dto.ai.AiTrainingSummaryDTO;

/**
 * AI 助手相关服务：训练数据摘要等，供前端或 LLM 提示词使用
 */
public interface AiService {

    /**
     * 获取当前用户的训练数据摘要，可注入到 AI 对话的系统提示中
     *
     * @param userId 当前用户 ID
     * @return 摘要 DTO，含 contextForPrompt、focusChars、suggestedMode 等
     */
    AiTrainingSummaryDTO getTrainingSummary(Integer userId);
}
