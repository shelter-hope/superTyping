package com.typinggame.controller.stats;

import com.typinggame.entity.user.UserDailyStats;
import com.typinggame.service.UserService;
import com.typinggame.vo.ResultMessage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@Tag(name = "玩家每日汇总数据 API")
@RequestMapping("/api/v1/stats/daily")
public class UserDailyStatsController {

    @Autowired
    private UserService userService;

    @GetMapping
    @Operation(summary = "获取用户每日统计数据")
    public ResponseEntity<ResultMessage<List<UserDailyStats>>> getDailyStats(
            @RequestParam(name = "userId", required = false) Integer userId,
            @RequestParam(name = "date", required = false) LocalDate date,
            @RequestParam(name = "startDate", required = false) LocalDate startDate,
            @RequestParam(name = "endDate", required = false) LocalDate endDate) {
        if (userId == null) {
            userId = getCurrentUserId();
        }
        List<UserDailyStats> stats = userService.getDailyStats(userId, date, startDate, endDate);
        return ResponseEntity.ok(ResultMessage.success(stats));
    }

    @GetMapping("/summary")
    @Operation(summary = "获取用户每日统计摘要")
    public ResponseEntity<ResultMessage<Object>> getDailyStatsSummary(
            @RequestParam(name = "userId", required = false) Integer userId,
            @RequestParam(name = "period", defaultValue = "week") String period) {
        if (userId == null) {
            userId = getCurrentUserId();
        }
        Object summary = userService.getDailyStatsSummary(userId, period);
        return ResponseEntity.ok(ResultMessage.success(summary));
    }

    private Integer getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return Integer.parseInt(authentication.getName());
    }
}