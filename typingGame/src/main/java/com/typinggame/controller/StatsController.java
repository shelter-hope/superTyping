package com.typinggame.controller;

import com.typinggame.dto.stats.ErrorComparisonDTO;
import com.typinggame.dto.stats.TrainingSuggestionDTO;
import com.typinggame.dto.stats.TrendPointDTO;
import com.typinggame.dto.stats.TypingBehaviorDTO;
import com.typinggame.service.StatsService;
import com.typinggame.vo.ResultMessage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 数据分析 API（打字行为、进步趋势、训练建议、错误对比）
 */
@RestController
@Tag(name = "数据分析 API")
@RequestMapping("/api/v1/stats")
public class StatsController {

    @Autowired
    private StatsService statsService;

    /**
     * 打字行为分析接口
     * @param userId 用户ID（仅允许当前用户查询自身）
     * @return 分析结果
     */
    @GetMapping("/typing-behavior/{userId}")
    @Operation(summary = "分析打字行为")
    public ResponseEntity<ResultMessage<TypingBehaviorDTO>> analyzeTypingBehavior(@PathVariable("userId") Integer userId) {
        ensureSelf(userId);
        return ResponseEntity.ok(ResultMessage.success(statsService.analyzeTypingBehavior(userId)));
    }

    /**
     * 进步趋势分析接口
     * @param userId 用户ID
     * @param period 时间周期 week / month / year
     * @return 分析结果
     */
    @GetMapping("/progress-trend")
    @Operation(summary = "分析进步趋势")
    public ResponseEntity<ResultMessage<List<TrendPointDTO>>> getProgressTrend(
            @RequestParam("userId") Integer userId,
            @RequestParam(value = "period", defaultValue = "week") String period) {
        ensureSelf(userId);
        return ResponseEntity.ok(ResultMessage.success(statsService.getProgressTrend(userId, period)));
    }

    /**
     * 针对性训练建议接口
     * @param userId 用户ID
     * @return 建议结果
     */
    @GetMapping("/training-suggestions/{userId}")
    @Operation(summary = "获取针对性训练建议")
    public ResponseEntity<ResultMessage<TrainingSuggestionDTO>> getTrainingSuggestions(@PathVariable("userId") Integer userId) {
        ensureSelf(userId);
        return ResponseEntity.ok(ResultMessage.success(statsService.getTrainingSuggestions(userId)));
    }

    /**
     * 时间维度数据查询接口
     * @param userId 用户ID
     * @param timeRange 时间范围 week / month / year / all / 3days
     * @return 查询结果
     */
    @GetMapping("/trend")
    @Operation(summary = "查询时间维度数据")
    public ResponseEntity<ResultMessage<List<TrendPointDTO>>> getTrendData(
            @RequestParam("userId") Integer userId,
            @RequestParam(value = "timeRange", defaultValue = "week") String timeRange) {
        ensureSelf(userId);
        return ResponseEntity.ok(ResultMessage.success(statsService.getTrendData(userId, timeRange)));
    }

    /**
     * 速度趋势分析接口
     * @param userId 用户ID
     * @param timeRange 时间范围
     * @return 分析结果
     */
    @GetMapping("/speed-trend")
    @Operation(summary = "分析速度趋势")
    public ResponseEntity<ResultMessage<List<TrendPointDTO>>> getSpeedTrend(
            @RequestParam("userId") Integer userId,
            @RequestParam(value = "timeRange", defaultValue = "week") String timeRange) {
        ensureSelf(userId);
        return ResponseEntity.ok(ResultMessage.success(statsService.getSpeedTrend(userId, timeRange)));
    }

    /**
     * 准确率趋势分析接口
     * @param userId 用户ID
     * @param timeRange 时间范围
     * @return 分析结果
     */
    @GetMapping("/accuracy-trend")
    @Operation(summary = "分析准确率趋势")
    public ResponseEntity<ResultMessage<List<TrendPointDTO>>> getAccuracyTrend(
            @RequestParam("userId") Integer userId,
            @RequestParam(value = "timeRange", defaultValue = "week") String timeRange) {
        ensureSelf(userId);
        return ResponseEntity.ok(ResultMessage.success(statsService.getAccuracyTrend(userId, timeRange)));
    }

    /**
     * 错误次数对比接口（本周 vs 上周）
     * @param userId 用户ID
     * @return 对比结果
     */
    @GetMapping("/error-comparison")
    @Operation(summary = "对比错误次数")
    public ResponseEntity<ResultMessage<ErrorComparisonDTO>> getErrorComparison(@RequestParam("userId") Integer userId) {
        ensureSelf(userId);
        return ResponseEntity.ok(ResultMessage.success(statsService.getErrorComparison(userId)));
    }

    private Integer getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return Integer.parseInt(authentication.getName());
    }

    private void ensureSelf(Integer userId) {
        if (userId == null || !userId.equals(getCurrentUserId())) {
            throw new org.springframework.security.access.AccessDeniedException("只能查询本人的统计数据");
        }
    }
}
