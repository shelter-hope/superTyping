package com.typinggame.game.controller;

import com.typinggame.dto.game.GameRecordSummaryDTO;
import com.typinggame.dto.stats.WeakCharDTO;
import com.typinggame.game.service.InternalPlayerDataService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Date;
import java.util.List;

/**
 * 跨服务只读数据接口。
 *
 * <p>网关对 {@code /api/v1/internal/**} 一律 404，只允许服务间直连调用；
 * 消费方失败时应按空结果降级，不得阻塞主流程。</p>
 */
@RestController
@RequestMapping("/api/v1/internal/player-data")
public class InternalPlayerDataController {

    private final InternalPlayerDataService internalPlayerDataService;

    public InternalPlayerDataController(InternalPlayerDataService internalPlayerDataService) {
        this.internalPlayerDataService = internalPlayerDataService;
    }

    @GetMapping("/game-records")
    public ResponseEntity<List<GameRecordSummaryDTO>> gameRecords(
            @RequestParam Integer userId,
            @RequestParam(name = "limit", defaultValue = "20") int limit,
            @RequestParam(name = "startDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Date startDate,
            @RequestParam(name = "endDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Date endDate) {
        if (startDate != null && endDate != null) {
            return ResponseEntity.ok(internalPlayerDataService.recordsBetween(userId, startDate, endDate));
        }
        return ResponseEntity.ok(internalPlayerDataService.recentRecords(userId, limit));
    }

    @GetMapping("/weak-chars")
    public ResponseEntity<List<WeakCharDTO>> weakChars(
            @RequestParam Integer userId,
            @RequestParam(name = "limit", defaultValue = "10") int limit) {
        return ResponseEntity.ok(internalPlayerDataService.weakChars(userId, limit));
    }
}
