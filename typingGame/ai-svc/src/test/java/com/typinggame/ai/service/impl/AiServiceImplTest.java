package com.typinggame.ai.service.impl;

import com.typinggame.ai.support.GameCoreClient;
import com.typinggame.dto.ai.AiTrainingSummaryDTO;
import com.typinggame.dto.game.GameRecordSummaryDTO;
import com.typinggame.dto.stats.WeakCharDTO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AiServiceImplTest {

    @Mock
    private GameCoreClient gameCoreClient;

    @InjectMocks
    private AiServiceImpl aiService;

    @Test
    void buildsSummaryFromWeakCharsAndRecentRecords() {
        when(gameCoreClient.weakChars(7, 10)).thenReturn(Arrays.asList(
                weakChar("i"), weakChar("t"), weakChar(" "), weakChar(null)));
        when(gameCoreClient.recentRecords(7, 20)).thenReturn(Arrays.asList(
                record(100, 85, 80, 100), record(100, 85, 90, 120)));
        when(gameCoreClient.recordsBetween(any(), any(), any()))
                .thenReturn(Collections.singletonList(record(100, 85, 80, 100)))
                .thenReturn(Collections.singletonList(record(50, 40, 80, 100)));

        AiTrainingSummaryDTO summary = aiService.getTrainingSummary(7);

        assertThat(summary.getFocusChars()).containsExactly("i", "t");
        assertThat(summary.getSuggestedMode()).isEqualTo("endless");
        assertThat(summary.getRecentAccuracy()).isEqualTo(85.0);
        assertThat(summary.getRecentSpeed()).isEqualTo(110.0);
        assertThat(summary.getErrorComparisonSummary()).contains("较上周增加 5 次");
        assertThat(summary.getContextForPrompt()).contains("建议重点练习的字符：i、t");
    }

    @Test
    void fallsBackToEmptyContextWhenNoData() {
        when(gameCoreClient.weakChars(7, 10)).thenReturn(Collections.emptyList());
        when(gameCoreClient.recentRecords(7, 20)).thenReturn(Collections.emptyList());
        when(gameCoreClient.recordsBetween(any(), any(), any()))
                .thenReturn(Collections.emptyList())
                .thenReturn(Collections.emptyList());

        AiTrainingSummaryDTO summary = aiService.getTrainingSummary(7);

        assertThat(summary.getFocusChars()).isEmpty();
        assertThat(summary.getRecentAccuracy()).isZero();
        assertThat(summary.getRecentSpeed()).isZero();
        assertThat(summary.getErrorComparisonSummary()).contains("与上周持平");
        assertThat(summary.getContextForPrompt()).contains("暂无足够字符统计数据");
    }

    private WeakCharDTO weakChar(String charValue) {
        WeakCharDTO dto = new WeakCharDTO();
        dto.setUserId(7);
        dto.setCharValue(charValue);
        return dto;
    }

    private GameRecordSummaryDTO record(int total, int correct, int accuracy, int speed) {
        GameRecordSummaryDTO dto = new GameRecordSummaryDTO();
        dto.setUserId(7);
        dto.setTotalChars(total);
        dto.setCorrectChars(correct);
        dto.setAvgAccuracy(BigDecimal.valueOf(accuracy));
        dto.setAvgSpeed(BigDecimal.valueOf(speed));
        return dto;
    }
}
