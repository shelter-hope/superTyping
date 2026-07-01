package com.typinggame.controller;

import com.typinggame.dto.stats.CharacterStatsBatchUpdateDTO;
import com.typinggame.entity.user.CharacterStats;
import com.typinggame.service.CharacterStatsService;
import com.typinggame.vo.ResultMessage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;

/**
 * 字符统计 API
 */
@RestController
@Tag(name = "字符统计 API")
@RequestMapping("/api/v1/stats/character")
public class CharacterStatsController {

    @Autowired
    private CharacterStatsService characterStatsService;

    /**
     * 批量更新字符统计
     * @param batchDTO 批量更新数据
     * @return 成功响应
     */
    @PostMapping("/batch")
    @Operation(summary = "批量更新字符统计")
    public ResponseEntity<ResultMessage<Void>> batchUpdate(@Valid @RequestBody CharacterStatsBatchUpdateDTO batchDTO) {
        Integer userId = getCurrentUserId();
        characterStatsService.batchUpdate(userId, batchDTO);
        return ResponseEntity.ok(ResultMessage.success());
    }

    /**
     * 获取我的字符统计
     * @return 统计列表
     */
    @GetMapping
    @Operation(summary = "获取我的字符统计")
    public ResponseEntity<ResultMessage<List<CharacterStats>>> getMyStats() {
        Integer userId = getCurrentUserId();
        List<CharacterStats> stats = characterStatsService.getUserStats(userId);
        return ResponseEntity.ok(ResultMessage.success(stats));
    }

    /**
     * 获取易错字符
     * @param limit 数量限制
     * @return 易错字符列表
     */
    @GetMapping("/weak")
    @Operation(summary = "获取易错字符")
    public ResponseEntity<ResultMessage<List<CharacterStats>>> getWeakChars(@RequestParam(value = "limit", defaultValue = "10") int limit) {
        Integer userId = getCurrentUserId();
        List<CharacterStats> stats = characterStatsService.getWeakChars(userId, limit);
        return ResponseEntity.ok(ResultMessage.success(stats));
    }

    private Integer getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return Integer.parseInt(authentication.getName());
    }
}
