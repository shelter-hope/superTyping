package com.typinggame.service.impl;

import com.typinggame.dto.stats.ErrorComparisonDTO;
import com.typinggame.dto.stats.TrainingSuggestionDTO;
import com.typinggame.service.StatsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;

/**
 * AI 训练数据摘要服务单元测试
 */
@ExtendWith(MockitoExtension.class)
class AiServiceImplTest {

    @Mock
    private StatsService statsService;

    @InjectMocks
    private AiServiceImpl aiService;

    @BeforeEach
    void setUp() {
        TrainingSuggestionDTO suggestions = TrainingSuggestionDTO.builder()
                .focusChars(List.of(
                        new TrainingSuggestionDTO.CharSuggestionItem("a", 5, 0.8, "letter"),
                        new TrainingSuggestionDTO.CharSuggestionItem("b", 3, 0.85, "letter")))
                .suggestedMode("endless")
                .summary("建议重点练习以下字符：a、b。")
                .recentAccuracy(85.0)
                .recentSpeed(120.0)
                .build();
        ErrorComparisonDTO errorComp = ErrorComparisonDTO.builder()
                .thisWeekErrors(10)
                .lastWeekErrors(12)
                .change(-2)
                .changeRate(-0.17)
                .thisWeekTopErrors(List.of())
                .lastWeekTopErrors(List.of())
                .build();
        when(statsService.getTrainingSuggestions(anyInt())).thenReturn(suggestions);
        when(statsService.getErrorComparison(anyInt())).thenReturn(errorComp);
    }

    @Test
    void getTrainingSummary_returnsDtoWithContextAndFocusChars() {
        var result = aiService.getTrainingSummary(1);

        assertThat(result).isNotNull();
        assertThat(result.getContextForPrompt()).contains("用户打字训练数据摘要");
        assertThat(result.getContextForPrompt()).contains("建议重点练习");
        assertThat(result.getFocusChars()).containsExactly("a", "b");
        assertThat(result.getSuggestedMode()).isEqualTo("endless");
        assertThat(result.getRecentAccuracy()).isEqualTo(85.0);
        assertThat(result.getRecentSpeed()).isEqualTo(120.0);
        assertThat(result.getErrorComparisonSummary()).contains("本周错误数：10");
        assertThat(result.getErrorComparisonSummary()).contains("较上周减少 2 次");
    }
}
