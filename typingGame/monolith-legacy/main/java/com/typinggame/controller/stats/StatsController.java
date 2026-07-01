package com.typinggame.controller.stats;

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

@RestController
@Tag(name = "数据分析 API")
@RequestMapping("/api/v1/stats")
public class StatsController {

    @Autowired
    private StatsService statsService;

    @GetMapping("/typing-behavior/{userId}")
    @Operation(summary = "分析打字行为")
    public ResponseEntity<ResultMessage<TypingBehaviorDTO>> analyzeTypingBehavior(@PathVariable("userId") Integer userId) {
        ensureSelf(userId);
        return ResponseEntity.ok(ResultMessage.success(statsService.analyzeTypingBehavior(userId)));
    }

    @GetMapping("/progress-trend")
    @Operation(summary = "分析进步趋势")
    public ResponseEntity<ResultMessage<List<TrendPointDTO>>> getProgressTrend(
            @RequestParam("userId") Integer userId,
            @RequestParam(value = "period", defaultValue = "week") String period) {
        ensureSelf(userId);
        return ResponseEntity.ok(ResultMessage.success(statsService.getProgressTrend(userId, period)));
    }

    @GetMapping("/training-suggestions/{userId}")
    @Operation(summary = "获取针对性训练建议")
    public ResponseEntity<ResultMessage<TrainingSuggestionDTO>> getTrainingSuggestions(@PathVariable("userId") Integer userId) {
        ensureSelf(userId);
        return ResponseEntity.ok(ResultMessage.success(statsService.getTrainingSuggestions(userId)));
    }

    @GetMapping("/trend")
    @Operation(summary = "查询时间维度数据")
    public ResponseEntity<ResultMessage<List<TrendPointDTO>>> getTrendData(
            @RequestParam("userId") Integer userId,
            @RequestParam(value = "timeRange", defaultValue = "week") String timeRange) {
        ensureSelf(userId);
        return ResponseEntity.ok(ResultMessage.success(statsService.getTrendData(userId, timeRange)));
    }

    @GetMapping("/speed-trend")
    @Operation(summary = "分析速度趋势")
    public ResponseEntity<ResultMessage<List<TrendPointDTO>>> getSpeedTrend(
            @RequestParam("userId") Integer userId,
            @RequestParam(value = "timeRange", defaultValue = "week") String timeRange) {
        ensureSelf(userId);
        return ResponseEntity.ok(ResultMessage.success(statsService.getSpeedTrend(userId, timeRange)));
    }

    @GetMapping("/accuracy-trend")
    @Operation(summary = "分析准确率趋势")
    public ResponseEntity<ResultMessage<List<TrendPointDTO>>> getAccuracyTrend(
            @RequestParam("userId") Integer userId,
            @RequestParam(value = "timeRange", defaultValue = "week") String timeRange) {
        ensureSelf(userId);
        return ResponseEntity.ok(ResultMessage.success(statsService.getAccuracyTrend(userId, timeRange)));
    }

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