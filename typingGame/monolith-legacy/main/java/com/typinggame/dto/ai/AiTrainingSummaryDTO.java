package com.typinggame.dto.ai;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 供 AI 助手使用的用户训练数据摘要（可注入到 LLM 提示词）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiTrainingSummaryDTO {

    /** 可直接拼进系统提示的文本摘要（易错字符、建议、近期表现等） */
    private String contextForPrompt;

    /** 建议重点练习的字符列表 */
    private List<String> focusChars;

    /** 建议的练习模式，如 endless / dictionary */
    private String suggestedMode;

    /** 近期平均准确率（0–100） */
    private Double recentAccuracy;

    /** 近期平均速度 */
    private Double recentSpeed;

    /** 本周 vs 上周错误数简要描述 */
    private String errorComparisonSummary;
}
