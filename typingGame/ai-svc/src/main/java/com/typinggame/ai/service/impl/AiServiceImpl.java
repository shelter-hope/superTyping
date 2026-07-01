package com.typinggame.ai.service.impl;

import com.typinggame.ai.service.AiService;
import com.typinggame.dto.ai.AiTrainingSummaryDTO;
import com.typinggame.dto.stats.ErrorComparisonDTO;
import com.typinggame.dto.stats.TrainingSuggestionDTO;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class AiServiceImpl implements AiService {

    @Override
    public AiTrainingSummaryDTO getTrainingSummary(Integer userId) {
        return AiTrainingSummaryDTO.builder()
                .contextForPrompt("【用户打字训练数据摘要】\n你是打字训练助手。")
                .focusChars(Collections.emptyList())
                .suggestedMode("endless")
                .build();
    }

    private String formatErrorComparisonSummary(ErrorComparisonDTO dto) {
        if (dto == null) return "";
        int thisWeek = dto.getThisWeekErrors() != null ? dto.getThisWeekErrors() : 0;
        int lastWeek = dto.getLastWeekErrors() != null ? dto.getLastWeekErrors() : 0;
        Integer change = dto.getChange();
        Double rate = dto.getChangeRate();
        StringBuilder sb = new StringBuilder();
        sb.append("本周错误数：").append(thisWeek).append("，上周：").append(lastWeek);
        if (change != null && rate != null) {
            sb.append("；");
            if (change > 0) sb.append("较上周增加 ").append(change).append(" 次");
            else if (change < 0) sb.append("较上周减少 ").append(-change).append(" 次");
            else sb.append("与上周持平");
            if (rate != 0) sb.append("（变化率 ").append(String.format("%.0f%%", rate * 100)).append("）");
        }
        sb.append("。");
        return sb.toString();
    }

    private String buildContextForPrompt(TrainingSuggestionDTO suggestions, String errorSummary) {
        StringBuilder sb = new StringBuilder();
        sb.append("【用户打字训练数据摘要】\n");
        if (suggestions.getSummary() != null && !suggestions.getSummary().isBlank()) {
            sb.append(suggestions.getSummary()).append("\n");
        }
        if (suggestions.getRecentAccuracy() != null || suggestions.getRecentSpeed() != null) {
            sb.append("近期表现：");
            if (suggestions.getRecentAccuracy() != null) sb.append("平均准确率 ").append(suggestions.getRecentAccuracy()).append("%");
            if (suggestions.getRecentSpeed() != null) sb.append("，平均速度 ").append(suggestions.getRecentSpeed());
            sb.append("。\n");
        }
        if (errorSummary != null && !errorSummary.isBlank()) {
            sb.append("错误趋势：").append(errorSummary).append("\n");
        }
        if (suggestions.getFocusChars() != null && !suggestions.getFocusChars().isEmpty()) {
            String chars = suggestions.getFocusChars().stream()
                    .map(TrainingSuggestionDTO.CharSuggestionItem::getCharacter)
                    .limit(10)
                    .collect(Collectors.joining("、"));
            sb.append("建议重点练习的字符：").append(chars).append("。");
        }
        return sb.toString().trim();
    }
}