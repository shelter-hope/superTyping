package com.typinggame.controller.game;

import com.typinggame.dto.game.GameRecordUploadDTO;
import com.typinggame.dto.game.GameRecordUploadResultDTO;
import com.typinggame.entity.game.GameRecord;
import com.typinggame.service.GameRecordService;
import com.typinggame.vo.ResultMessage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.Date;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;

@RestController
@Tag(name = "游戏记录 API")
@RequestMapping("/api/v1/game")
public class GameRecordController {

    @Autowired
    private GameRecordService gameRecordService;

    @PostMapping("/records")
    @Operation(summary = "上传游戏结算数据")
    public ResponseEntity<ResultMessage<GameRecordUploadResultDTO>> uploadRecord(@Valid @RequestBody GameRecordUploadDTO recordDTO) {
        Integer userId = getCurrentUserId();
        GameRecordUploadResultDTO result = gameRecordService.saveRecord(userId, recordDTO);
        return ResponseEntity.ok(ResultMessage.success(result));
    }

    @GetMapping("/records")
    @Operation(summary = "获取历史记录列表")
    public ResponseEntity<ResultMessage<List<GameRecord>>> getRecords(
            @RequestParam(name="userId", required = false) Integer userId,
            @RequestParam(name="limit", defaultValue = "20") Integer limit,
            @RequestParam(name="mode", required = false) String mode,
            @RequestParam(name="startDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Date startDate,
            @RequestParam(name="endDate",required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Date endDate) {
        if (userId == null) {
            userId = getCurrentUserId();
        }
        List<GameRecord> records = gameRecordService.getRecords(userId, mode, limit, startDate, endDate);
        return ResponseEntity.ok(ResultMessage.success(records));
    }

    @GetMapping("/records/{id}")
    @Operation(summary = "获取单局详情")
    public ResponseEntity<ResultMessage<GameRecord>> getRecordDetail(@PathVariable Long id) {
        GameRecord record = gameRecordService.getRecordDetail(id);
        return ResponseEntity.ok(ResultMessage.success(record));
    }

    @GetMapping("/best")
    @Operation(summary = "获取个人最佳成绩")
    public ResponseEntity<ResultMessage<GameRecord>> getBestRecord(
            @RequestParam(required = false) Integer userId,
            @RequestParam(defaultValue = "endless") String mode) {
        if (userId == null) {
            userId = getCurrentUserId();
        }
        GameRecord record = gameRecordService.getBestRecord(userId, mode);
        return ResponseEntity.ok(ResultMessage.success(record));
    }

    private Integer getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return Integer.parseInt(authentication.getName());
    }
}