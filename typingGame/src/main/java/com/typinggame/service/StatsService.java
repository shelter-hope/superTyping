package com.typinggame.service;

import com.typinggame.dto.stats.ErrorComparisonDTO;
import com.typinggame.dto.stats.TrainingSuggestionDTO;
import com.typinggame.dto.stats.TrendPointDTO;
import com.typinggame.dto.stats.TypingBehaviorDTO;

import java.util.List;

/**
 * 统计分析服务：打字行为、进步趋势、训练建议、错误对比
 */
public interface StatsService {

    /**
     * 用户打字行为画像（速度稳定性、错误分布、易错字符）
     */
    TypingBehaviorDTO analyzeTypingBehavior(Integer userId);

    /**
     * 进步趋势（按周期聚合的速度、准确率等）
     * @param period week / month / year
     */
    List<TrendPointDTO> getProgressTrend(Integer userId, String period);

    /**
     * 针对性训练建议（易错字符、推荐模式、摘要）
     */
    TrainingSuggestionDTO getTrainingSuggestions(Integer userId);

    /**
     * 时间维度趋势数据（与 progress-trend 类似，timeRange 同 period）
     * @param timeRange week / month / year / all / 3days
     */
    List<TrendPointDTO> getTrendData(Integer userId, String timeRange);

    /**
     * 速度趋势（时间序列）
     */
    List<TrendPointDTO> getSpeedTrend(Integer userId, String timeRange);

    /**
     * 准确率趋势（时间序列）
     */
    List<TrendPointDTO> getAccuracyTrend(Integer userId, String timeRange);

    /**
     * 错误次数对比：本周 vs 上周
     */
    ErrorComparisonDTO getErrorComparison(Integer userId);
}
