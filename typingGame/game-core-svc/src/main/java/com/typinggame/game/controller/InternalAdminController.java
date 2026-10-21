package com.typinggame.game.controller;

import com.typinggame.entity.config.SystemMonsterCommoConfig;
import com.typinggame.entity.config.SystemMonsterTypeConfig;
import com.typinggame.entity.game.GameRecord;
import com.typinggame.entity.model.ModelConstant;
import com.typinggame.entity.model.ModelInitParams;
import com.typinggame.game.service.InternalAdminService;
import com.typinggame.vo.ResultMessage;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/internal/admin")
public class InternalAdminController {
    private final InternalAdminService internalAdminService;

    public InternalAdminController(InternalAdminService internalAdminService) {
        this.internalAdminService = internalAdminService;
    }

    @GetMapping("/game-records") public ResponseEntity<ResultMessage<Map<String, Object>>> gameRecords(@RequestParam(defaultValue = "1") int page, @RequestParam(defaultValue = "20") int pageSize, @RequestParam(required = false) Integer userId) { return ok(internalAdminService.getGameRecords(page, pageSize, userId)); }
    @PatchMapping("/game-records/{recordId}") public ResponseEntity<ResultMessage<Void>> updateGameRecord(@PathVariable Integer recordId, @RequestBody GameRecord updates) { return internalAdminService.updateGameRecord(recordId, updates) ? ResponseEntity.ok(ResultMessage.success()) : ResponseEntity.badRequest().body(ResultMessage.error("记录不存在")); }
    @DeleteMapping("/game-records/{recordId}") public ResponseEntity<ResultMessage<Void>> deleteGameRecord(@PathVariable Integer recordId) { internalAdminService.deleteGameRecord(recordId); return ResponseEntity.ok(ResultMessage.success()); }
    @DeleteMapping("/users/{userId}/game-data") public ResponseEntity<ResultMessage<Void>> deleteUserGameData(@PathVariable Integer userId) { internalAdminService.deleteUserGameData(userId); return ResponseEntity.ok(ResultMessage.success()); }
    @GetMapping("/character-stats") public ResponseEntity<ResultMessage<Map<String, Object>>> characterStats(@RequestParam(defaultValue = "1") int page, @RequestParam(defaultValue = "20") int pageSize) { return ok(internalAdminService.getCharacterStats(page, pageSize)); }
    @GetMapping("/monster-data") public ResponseEntity<ResultMessage<Map<String, Object>>> monsterData(@RequestParam Integer recordId, @RequestParam(defaultValue = "1") int page, @RequestParam(defaultValue = "100") int pageSize) { return ok(internalAdminService.getMonsterData(recordId, page, pageSize)); }
    @GetMapping("/string-parents") public ResponseEntity<ResultMessage<Map<String, Object>>> stringParents(@RequestParam(defaultValue = "1") int page, @RequestParam(defaultValue = "20") int pageSize) { return ok(internalAdminService.getStringParents(page, pageSize)); }
    @GetMapping("/system-params") public ResponseEntity<ResultMessage<Map<String, Object>>> systemParams(@RequestParam(defaultValue = "1") int page, @RequestParam(defaultValue = "20") int pageSize) { return ok(internalAdminService.getSystemParams(page, pageSize)); }
    @GetMapping("/system-monster-type-configs") public ResponseEntity<ResultMessage<List<?>>> monsterTypes() { return ResponseEntity.ok(ResultMessage.success(internalAdminService.getSystemMonsterTypeConfigs())); }
    @GetMapping("/system-monster-common-configs") public ResponseEntity<ResultMessage<List<?>>> monsterCommons() { return ResponseEntity.ok(ResultMessage.success(internalAdminService.getSystemMonsterCommonConfigs())); }
    @GetMapping("/monster-configs") public ResponseEntity<ResultMessage<List<?>>> monsterConfigs() { return ResponseEntity.ok(ResultMessage.success(internalAdminService.getMonsterConfigs())); }
    @GetMapping("/model-constants") public ResponseEntity<ResultMessage<Map<String, Object>>> modelConstants(@RequestParam(defaultValue = "1") int page, @RequestParam(defaultValue = "20") int pageSize) { return ok(internalAdminService.getModelConstants(page, pageSize)); }
    @GetMapping("/model-init-params") public ResponseEntity<ResultMessage<Map<String, Object>>> modelInitParams(@RequestParam(defaultValue = "1") int page, @RequestParam(defaultValue = "20") int pageSize) { return ok(internalAdminService.getModelInitParams(page, pageSize)); }
    @GetMapping("/model-init-params/mode/{gameMode}") public ResponseEntity<ResultMessage<List<ModelInitParams>>> modelInitParamsByMode(@PathVariable String gameMode) { return ResponseEntity.ok(ResultMessage.success(internalAdminService.getModelInitParamsByMode(gameMode))); }
    @PatchMapping("/model-constants/{id}") public ResponseEntity<ResultMessage<Void>> updateModelConstant(@PathVariable Integer id, @RequestBody ModelConstant body) { return internalAdminService.updateModelConstant(id, body) ? ResponseEntity.ok(ResultMessage.success()) : ResponseEntity.badRequest().body(ResultMessage.error("数据不存在")); }
    @PatchMapping("/model-init-params/{id}") public ResponseEntity<ResultMessage<Void>> updateModelInit(@PathVariable Integer id, @RequestBody ModelInitParams body) { return internalAdminService.updateModelInitParam(id, body) ? ResponseEntity.ok(ResultMessage.success()) : ResponseEntity.badRequest().body(ResultMessage.error("数据不存在")); }
    @PatchMapping("/system-monster-type-configs/{id}") public ResponseEntity<ResultMessage<Void>> updateMonsterType(@PathVariable Integer id, @RequestBody SystemMonsterTypeConfig body) { return internalAdminService.updateMonsterTypeConfig(id, body) ? ResponseEntity.ok(ResultMessage.success()) : ResponseEntity.badRequest().body(ResultMessage.error("数据不存在")); }
    @PatchMapping("/system-monster-common-configs/{id}") public ResponseEntity<ResultMessage<Void>> updateMonsterCommon(@PathVariable Integer id, @RequestBody SystemMonsterCommoConfig body) { return internalAdminService.updateMonsterCommonConfig(id, body) ? ResponseEntity.ok(ResultMessage.success()) : ResponseEntity.badRequest().body(ResultMessage.error("数据不存在")); }
    @GetMapping("/game-mode-configs") public ResponseEntity<ResultMessage<Map<String, Object>>> gameModeConfigs(@RequestParam(defaultValue = "1") int page, @RequestParam(defaultValue = "20") int pageSize) { return ok(internalAdminService.getGameModeConfigs(page, pageSize)); }
    @PatchMapping("/game-mode-configs/{id}") public ResponseEntity<ResultMessage<Void>> updateGameModeConfig(@PathVariable Integer id, @RequestBody Map<String, Object> body) { return ResponseEntity.ok(ResultMessage.success()); }
    @GetMapping("/user-model-adapt-logs") public ResponseEntity<ResultMessage<Map<String, Object>>> adaptLogs(@RequestParam(defaultValue = "1") int page, @RequestParam(defaultValue = "20") int pageSize) { return ok(internalAdminService.getAdaptLogs(page, pageSize)); }
    @GetMapping("/user-model-adapt-logs/trend") public ResponseEntity<ResultMessage<List<Map<String, Object>>>> adaptLogTrend(@RequestParam String startDate, @RequestParam String endDate) { return ResponseEntity.ok(ResultMessage.success(internalAdminService.getAdaptLogTrend(startDate, endDate))); }
    @GetMapping("/character-rules") public ResponseEntity<ResultMessage<Map<String, Object>>> characterRules() { return ok(internalAdminService.getCharacterRules()); }
    @PatchMapping("/character-rules") public ResponseEntity<ResultMessage<Boolean>> updateCharacterRules(@RequestBody Map<String, Object> body) { return ResponseEntity.ok(ResultMessage.success(true)); }
    @GetMapping("/movement-rules") public ResponseEntity<ResultMessage<Map<String, Object>>> movementRules() { return ok(internalAdminService.getMovementRules()); }
    @PatchMapping("/movement-rules") public ResponseEntity<ResultMessage<Boolean>> updateMovementRules(@RequestBody Map<String, Object> body) { return ResponseEntity.ok(ResultMessage.success(true)); }
    @GetMapping("/difficulty-params") public ResponseEntity<ResultMessage<Map<String, Object>>> difficultyParams() { return ok(internalAdminService.getDifficultyParams()); }
    @PatchMapping("/difficulty-params") public ResponseEntity<ResultMessage<Boolean>> updateDifficultyParams(@RequestBody Map<String, Object> body) { return ResponseEntity.ok(ResultMessage.success(true)); }
    @PostMapping("/config-templates") public ResponseEntity<ResultMessage<Boolean>> saveTemplate(@RequestBody Map<String, Object> body) { return ResponseEntity.ok(ResultMessage.success(true)); }
    @GetMapping("/config-templates") public ResponseEntity<ResultMessage<List<Map<String, Object>>>> templates() { return ResponseEntity.ok(ResultMessage.success(List.of())); }
    @GetMapping("/config-templates/{id}") public ResponseEntity<ResultMessage<Map<String, Object>>> template(@PathVariable Integer id) { return ok(Map.of("id", id)); }

    private ResponseEntity<ResultMessage<Map<String, Object>>> ok(Map<String, Object> data) { return ResponseEntity.ok(ResultMessage.success(data)); }
}
