package com.typinggame.dto.stats;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 针对性训练建议 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TrainingSuggestionDTO {
    /** 建议重点练习的字符列表（易错或准确率低） */
    private List<CharSuggestionItem> focusChars;
    /** 建议的练习模式，如 "endless" / "dictionary" */
    private String suggestedMode;
    /** 文字建议摘要 */
    private String summary;
    /** 近期平均准确率（%） */
    private Double recentAccuracy;
    /** 近期平均速度 */
    private Double recentSpeed;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CharSuggestionItem {
        private String character;
        private Integer errorCount;
        private Double accuracy;
        private String charType;
    }
}
