package com.typinggame.game.service;

import com.typinggame.dto.stats.ErrorComparisonDTO;
import com.typinggame.dto.stats.TrainingSuggestionDTO;
import com.typinggame.dto.stats.TrendPointDTO;
import com.typinggame.dto.stats.TypingBehaviorDTO;

import java.util.List;

public interface StatsService {
    TypingBehaviorDTO analyzeTypingBehavior(Integer userId);

    List<TrendPointDTO> getProgressTrend(Integer userId, String period);

    TrainingSuggestionDTO getTrainingSuggestions(Integer userId);

    List<TrendPointDTO> getTrendData(Integer userId, String timeRange);

    List<TrendPointDTO> getSpeedTrend(Integer userId, String timeRange);

    List<TrendPointDTO> getAccuracyTrend(Integer userId, String timeRange);

    ErrorComparisonDTO getErrorComparison(Integer userId);
}
