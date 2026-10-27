package com.typinggame.game.controller;

import com.typinggame.entity.config.SystemMonsterCommoConfig;
import com.typinggame.entity.config.SystemMonsterTypeConfig;
import com.typinggame.entity.game.GameRecord;
import com.typinggame.entity.model.ModelConstant;
import com.typinggame.entity.model.ModelInitParams;
import com.typinggame.game.service.InternalAdminService;
import com.typinggame.vo.ResultMessage;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
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

    @GetMapping("/game-records")
    public ResponseEntity<ResultMessage<Map<String, Object>>> gameRecords(@RequestParam(defaultValue = "1") int page, @RequestParam(defaultValue = "20") int pageSize,
                                                                          @RequestParam(required = false) Integer userId, @RequestParam(required = false) String gameMode) {
        return ok(internalAdminService.getGameRecords(page, pageSize, userId, gameMode));
    }

    @PatchMapping("/game-records/{recordId}")
    public ResponseEntity<ResultMessage<Void>> updateGameRecord(@PathVariable Integer recordId, @RequestBody GameRecord updates) {
        return internalAdminService.updateGameRecord(recordId, updates) ? ResponseEntity.ok(ResultMessage.success()) : ResponseEntity.badRequest().body(ResultMessage.error("记录不存在"));
    }

    @DeleteMapping("/game-records/{recordId}")
    public ResponseEntity<ResultMessage<Void>> deleteGameRecord(@PathVariable Integer recordId) {
        internalAdminService.deleteGameRecord(recordId);
        return ResponseEntity.ok(ResultMessage.success());
    }

    @DeleteMapping("/users/{userId}/game-data")
    public ResponseEntity<ResultMessage<Void>> deleteUserGameData(@PathVariable Integer userId) {
        internalAdminService.deleteUserGameData(userId);
        return ResponseEntity.ok(ResultMessage.success());
    }

    @GetMapping("/character-stats")
    public ResponseEntity<ResultMessage<Map<String, Object>>> characterStats(@RequestParam(defaultValue = "1") int page, @RequestParam(defaultValue = "20") int pageSize,
                                                                             @RequestParam(required = false) Integer userId, @RequestParam(required = false) String charType,
                                                                             @RequestParam(required = false) String charValue) {
        return ok(internalAdminService.getCharacterStats(page, pageSize, userId, charType, charValue));
    }

    @GetMapping("/key-confusion")
    public ResponseEntity<ResultMessage<Map<String, Object>>> keyConfusion(@RequestParam Integer userId, @RequestParam(defaultValue = "20") int limit) {
        return ok(internalAdminService.getKeyConfusion(userId, limit));
    }

    @GetMapping("/monster-data")
    public ResponseEntity<ResultMessage<Map<String, Object>>> monsterData(@RequestParam Integer recordId, @RequestParam(defaultValue = "1") int page, @RequestParam(defaultValue = "100") int pageSize) {
        return ok(internalAdminService.getMonsterData(recordId, page, pageSize));
    }

    @GetMapping("/string-parents")
    public ResponseEntity<ResultMessage<Map<String, Object>>> stringParents(@RequestParam(defaultValue = "1") int page, @RequestParam(defaultValue = "20") int pageSize,
                                                                            @RequestParam(required = false) Integer userId) {
        return ok(internalAdminService.getStringParents(page, pageSize, userId));
    }

    @GetMapping("/system-params")
    public ResponseEntity<ResultMessage<Map<String, Object>>> systemParams(@RequestParam(defaultValue = "1") int page, @RequestParam(defaultValue = "20") int pageSize,
                                                                           @RequestParam(required = false) String paramName) {
        return ok(internalAdminService.getSystemParams(page, pageSize, paramName));
    }

    @GetMapping("/system-monster-type-configs") public ResponseEntity<ResultMessage<List<?>>> monsterTypes() { return ResponseEntity.ok(ResultMessage.success(internalAdminService.getSystemMonsterTypeConfigs())); }
    @GetMapping("/system-monster-common-configs") public ResponseEntity<ResultMessage<List<?>>> monsterCommons() { return ResponseEntity.ok(ResultMessage.success(internalAdminService.getSystemMonsterCommonConfigs())); }
    @GetMapping("/monster-configs") public ResponseEntity<ResultMessage<List<?>>> monsterConfigs() { return ResponseEntity.ok(ResultMessage.success(internalAdminService.getMonsterConfigs())); }

    @PatchMapping("/monster-configs/{type}")
    public ResponseEntity<ResultMessage<Void>> updateMonsterConfig(@PathVariable String type, @RequestBody Map<String, Object> body) {
        internalAdminService.updateMonsterConfig(type, body);
        return ResponseEntity.ok(ResultMessage.success());
    }

    @GetMapping("/model-constants") public ResponseEntity<ResultMessage<Map<String, Object>>> modelConstants(@RequestParam(defaultValue = "1") int page, @RequestParam(defaultValue = "20") int pageSize) { return ok(internalAdminService.getModelConstants(page, pageSize)); }
    @GetMapping("/model-init-params") public ResponseEntity<ResultMessage<Map<String, Object>>> modelInitParams(@RequestParam(defaultValue = "1") int page, @RequestParam(defaultValue = "20") int pageSize) { return ok(internalAdminService.getModelInitParams(page, pageSize)); }
    @GetMapping("/model-init-params/mode/{gameMode}") public ResponseEntity<ResultMessage<List<ModelInitParams>>> modelInitParamsByMode(@PathVariable String gameMode) { return ResponseEntity.ok(ResultMessage.success(internalAdminService.getModelInitParamsByMode(gameMode))); }
    @PatchMapping("/model-constants/{id}") public ResponseEntity<ResultMessage<Void>> updateModelConstant(@PathVariable Integer id, @RequestBody ModelConstant body) { return internalAdminService.updateModelConstant(id, body) ? ResponseEntity.ok(ResultMessage.success()) : ResponseEntity.badRequest().body(ResultMessage.error("数据不存在")); }
    @PatchMapping("/model-init-params/{id}") public ResponseEntity<ResultMessage<Void>> updateModelInit(@PathVariable Integer id, @RequestBody ModelInitParams body) { return internalAdminService.updateModelInitParam(id, body) ? ResponseEntity.ok(ResultMessage.success()) : ResponseEntity.badRequest().body(ResultMessage.error("数据不存在")); }
    @PatchMapping("/system-monster-type-configs/{id}")
    public ResponseEntity<ResultMessage<Void>> updateMonsterType(@PathVariable Integer id, @RequestBody SystemMonsterTypeConfig body) {
        return internalAdminService.updateMonsterTypeConfig(id, body)
                ? ResponseEntity.ok(ResultMessage.success())
                : ResponseEntity.badRequest().body(ResultMessage.error("数据不存在"));
    }

    @PatchMapping("/system-monster-common-configs/{id}")
    public ResponseEntity<ResultMessage<Void>> updateMonsterCommon(@PathVariable Integer id, @RequestBody SystemMonsterCommoConfig body) {
        return internalAdminService.updateMonsterCommonConfig(id, body)
                ? ResponseEntity.ok(ResultMessage.success())
                : ResponseEntity.badRequest().body(ResultMessage.error("数据不存在"));
    }

    @GetMapping("/game-mode-configs")
    public ResponseEntity<ResultMessage<Map<String, Object>>> gameModeConfigs(@RequestParam(defaultValue = "1") int page, @RequestParam(defaultValue = "20") int pageSize) {
        return ok(internalAdminService.getGameModeConfigs(page, pageSize));
    }

    @PatchMapping("/game-mode-configs/{id}")
    public ResponseEntity<ResultMessage<Void>> updateGameModeConfig(@PathVariable Integer id, @RequestBody Map<String, Object> body) {
        internalAdminService.updateGameModeConfig(id, body);
        return ResponseEntity.ok(ResultMessage.success());
    }

    @GetMapping("/user-model-adapt-logs")
    public ResponseEntity<ResultMessage<Map<String, Object>>> adaptLogs(@RequestParam(defaultValue = "1") int page, @RequestParam(defaultValue = "20") int pageSize,
                                                                        @RequestParam(required = false) Integer userId, @RequestParam(required = false) String gameMode,
                                                                        @RequestParam(required = false) String startDate, @RequestParam(required = false) String endDate) {
        return ok(internalAdminService.getAdaptLogs(page, pageSize, userId, gameMode, startDate, endDate));
    }

    @GetMapping("/user-model-adapt-logs/trend")
    public ResponseEntity<ResultMessage<List<Map<String, Object>>>> adaptLogTrend(@RequestParam(required = false) Integer userId, @RequestParam(required = false) String gameMode,
                                                                                  @RequestParam String startDate, @RequestParam String endDate) {
        return ResponseEntity.ok(ResultMessage.success(internalAdminService.getAdaptLogTrend(userId, gameMode, startDate, endDate)));
    }

    @GetMapping({"/character-rules", "/movement-rules", "/difficulty-params"})
    public ResponseEntity<ResultMessage<Map<String, Object>>> rules(jakarta.servlet.http.HttpServletRequest request) {
        return ok(internalAdminService.getRules(ruleKey(request)));
    }

    @PatchMapping({"/character-rules", "/movement-rules", "/difficulty-params"})
    public ResponseEntity<ResultMessage<Boolean>> saveRules(jakarta.servlet.http.HttpServletRequest request, @RequestBody Map<String, Object> body) {
        internalAdminService.saveRules(ruleKey(request), body);
        return ResponseEntity.ok(ResultMessage.success(true));
    }

    @GetMapping("/config-templates")
    public ResponseEntity<ResultMessage<List<Map<String, Object>>>> templates() {
        return ResponseEntity.ok(ResultMessage.success(internalAdminService.getConfigTemplates()));
    }

    @GetMapping("/config-templates/{id}")
    public ResponseEntity<ResultMessage<Map<String, Object>>> template(@PathVariable Integer id) {
        return ok(internalAdminService.getConfigTemplate(id));
    }

    @PostMapping("/config-templates")
    public ResponseEntity<ResultMessage<Boolean>> saveTemplate(@RequestBody Map<String, Object> body, Authentication authentication) {
        Object name = body.get("name");
        internalAdminService.saveConfigTemplate(name == null ? null : name.toString(), body.get("config"), operatorId(authentication));
        return ResponseEntity.ok(ResultMessage.success(true));
    }

    @DeleteMapping("/config-templates/{id}")
    public ResponseEntity<ResultMessage<Void>> deleteTemplate(@PathVariable Integer id) {
        internalAdminService.deleteConfigTemplate(id);
        return ResponseEntity.ok(ResultMessage.success());
    }

    @GetMapping("/leaderboard-entries")
    public ResponseEntity<ResultMessage<Map<String, Object>>> leaderboardEntries(@RequestParam(defaultValue = "1") int page,
                                                                                 @RequestParam(defaultValue = "20") int pageSize,
                                                                                 @RequestParam(required = false) Integer userId,
                                                                                 @RequestParam(required = false) String gameMode) {
        return ok(internalAdminService.getLeaderboardEntries(page, pageSize, userId, gameMode));
    }

    @GetMapping("/overview/games")
    public ResponseEntity<ResultMessage<Map<String, Object>>> overview() {
        return ok(internalAdminService.getOverview());
    }

    private static String ruleKey(jakarta.servlet.http.HttpServletRequest request) {
        String uri = request.getRequestURI();
        return uri.substring(uri.lastIndexOf('/') + 1);
    }

    private static Integer operatorId(Authentication authentication) {
        if (authentication == null) return null;
        try {
            return Integer.parseInt(authentication.getName());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private ResponseEntity<ResultMessage<Map<String, Object>>> ok(Map<String, Object> data) { return ResponseEntity.ok(ResultMessage.success(data)); }
}
