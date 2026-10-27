package com.typinggame.game.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.typinggame.game.rule.DifficultyRule;
import com.typinggame.game.rule.RuleConfigService;
import com.typinggame.game.rule.RuleValidator;
import com.typinggame.vo.ResultMessage;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/internal/admin/game-rules")
public class GameRuleAdminController {
    private final RuleConfigService ruleConfigService;
    private final ObjectMapper objectMapper;

    public GameRuleAdminController(RuleConfigService ruleConfigService, ObjectMapper objectMapper) {
        this.ruleConfigService = ruleConfigService;
        this.objectMapper = objectMapper;
    }

    @GetMapping("/{key}")
    public ResponseEntity<ResultMessage<Map<String, Object>>> active(@PathVariable String key) {
        return ResponseEntity.ok(ResultMessage.success(ruleConfigService.activeView(key)));
    }

    @PutMapping("/{key}")
    public ResponseEntity<ResultMessage<Map<String, Object>>> draft(@PathVariable String key, @RequestBody Map<String, Object> body) {
        Object payload = body.containsKey("payload") ? body.get("payload") : body;
        return ResponseEntity.ok(ResultMessage.success(ruleConfigService.saveDraft(key, payload, null)));
    }

    @PostMapping("/{key}/publish")
    public ResponseEntity<ResultMessage<Map<String, Object>>> publish(@PathVariable String key, @RequestBody Map<String, Object> body) {
        int version = ((Number) body.get("version")).intValue();
        return ResponseEntity.ok(ResultMessage.success(ruleConfigService.publish(key, version, null)));
    }

    @GetMapping("/{key}/versions")
    public ResponseEntity<ResultMessage<List<Map<String, Object>>>> versions(@PathVariable String key) {
        return ResponseEntity.ok(ResultMessage.success(ruleConfigService.versions(key)));
    }

    @PostMapping("/simulate")
    public ResponseEntity<ResultMessage<Map<String, Object>>> simulate(@RequestBody Map<String, Object> body) {
        List<?> accuracies = body.get("accuracies") instanceof List<?> list ? list : List.of();
        List<?> speeds = body.get("speeds") instanceof List<?> list ? list : List.of();
        int rounds = body.get("rounds") instanceof Number number ? number.intValue() : 8;
        if (body.get("payload") instanceof Map<?, ?> payload && !payload.isEmpty()) {
            DifficultyRule candidate = objectMapper.convertValue(payload, DifficultyRule.class);
            RuleValidator.validateDifficulty(candidate);
            return ResponseEntity.ok(ResultMessage.success(
                    ruleConfigService.simulate(candidate, accuracies, speeds, rounds)));
        }
        return ResponseEntity.ok(ResultMessage.success(ruleConfigService.simulate(accuracies, speeds, rounds)));
    }
}
