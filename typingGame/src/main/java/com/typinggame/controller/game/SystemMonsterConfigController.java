package com.typinggame.controller.game;

import com.typinggame.entity.game.SystemMonsterConfig;
import com.typinggame.repository.SystemMonsterConfigRepository;
import com.typinggame.service.MonsterConfigService;
import com.typinggame.vo.ResultMessage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@Tag(name = "系统怪物配置 API")
@RequestMapping("/api/v1/game/monster-config")
public class SystemMonsterConfigController {

    @Autowired
    private MonsterConfigService monsterConfigService;

    @Autowired
    private SystemMonsterConfigRepository configRepository;

    @GetMapping
    @Operation(summary = "获取所有怪物配置")
    public ResponseEntity<ResultMessage<List<SystemMonsterConfig>>> getAllMonsterConfigs() {
        List<SystemMonsterConfig> configs = monsterConfigService.getAllConfigs();
        return ResponseEntity.ok(ResultMessage.success(configs));
    }

    @GetMapping("/{type}")
    @Operation(summary = "根据类型获取怪物配置")
    public ResponseEntity<ResultMessage<SystemMonsterConfig>> getMonsterConfigByType(
            @PathVariable String type) {
        return configRepository.findByType(type.toUpperCase())
                .map(config -> ResponseEntity.ok(ResultMessage.success(config)))
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{type}")
    @Operation(summary = "更新怪物配置")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ResultMessage<SystemMonsterConfig>> updateMonsterConfig(
            @PathVariable String type,
            @RequestBody SystemMonsterConfig updates) {
        SystemMonsterConfig config = monsterConfigService.updateConfig(type, updates);
        if (config != null) {
            return ResponseEntity.ok(ResultMessage.success(config));
        }
        return ResponseEntity.notFound().build();
    }
}