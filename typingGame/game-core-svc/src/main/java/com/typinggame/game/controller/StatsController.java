package com.typinggame.game.controller;

import com.typinggame.dto.stats.ErrorComparisonDTO;
import com.typinggame.dto.stats.TrainingSuggestionDTO;
import com.typinggame.dto.stats.TrendPointDTO;
import com.typinggame.dto.stats.TypingBehaviorDTO;
import com.typinggame.game.service.StatsService;
import com.typinggame.vo.ResultMessage;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/stats")
public class StatsController {

    private final StatsService statsService;

    public StatsController(StatsService statsService) {
        this.statsService = statsService;
    }

    @GetMapping("/typing-behavior/{userId}")
    public ResponseEntity<ResultMessage<TypingBehaviorDTO>> analyzeTypingBehavior(@PathVariable("userId") Integer userId) {
        ensureSelf(userId);
        return ResponseEntity.ok(ResultMessage.success(statsService.analyzeTypingBehavior(userId)));
    }

    @GetMapping("/progress-trend")
    public ResponseEntity<ResultMessage<List<TrendPointDTO>>> getProgressTrend(
            @RequestParam("userId") Integer userId,
            @RequestParam(value = "period", defaultValue = "week") String period) {
        ensureSelf(userId);
        return ResponseEntity.ok(ResultMessage.success(statsService.getProgressTrend(userId, period)));
    }

    @GetMapping("/training-suggestions/{userId}")
    public ResponseEntity<ResultMessage<TrainingSuggestionDTO>> getTrainingSuggestions(@PathVariable("userId") Integer userId) {
        ensureSelf(userId);
        return ResponseEntity.ok(ResultMessage.success(statsService.getTrainingSuggestions(userId)));
    }

    @GetMapping("/trend")
    public ResponseEntity<ResultMessage<List<TrendPointDTO>>> getTrendData(
            @RequestParam("userId") Integer userId,
            @RequestParam(value = "timeRange", defaultValue = "week") String timeRange) {
        ensureSelf(userId);
        return ResponseEntity.ok(ResultMessage.success(statsService.getTrendData(userId, timeRange)));
    }

    @GetMapping("/speed-trend")
    public ResponseEntity<ResultMessage<List<TrendPointDTO>>> getSpeedTrend(
            @RequestParam("userId") Integer userId,
            @RequestParam(value = "timeRange", defaultValue = "week") String timeRange) {
        ensureSelf(userId);
        return ResponseEntity.ok(ResultMessage.success(statsService.getSpeedTrend(userId, timeRange)));
    }

    @GetMapping("/accuracy-trend")
    public ResponseEntity<ResultMessage<List<TrendPointDTO>>> getAccuracyTrend(
            @RequestParam("userId") Integer userId,
            @RequestParam(value = "timeRange", defaultValue = "week") String timeRange) {
        ensureSelf(userId);
        return ResponseEntity.ok(ResultMessage.success(statsService.getAccuracyTrend(userId, timeRange)));
    }

    @GetMapping("/error-comparison")
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
            throw new AccessDeniedException("只能查询本人的统计数据");
        }
    }
}
