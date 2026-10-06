package com.typinggame.game.controller;

import com.typinggame.dto.game.GameCharTypeConfigDTO;
import com.typinggame.dto.game.GameCharTypeConfigUpdateDTO;
import com.typinggame.dto.game.GameRecommendDTO;
import com.typinggame.dto.game.NextRoundResponseDTO;
import com.typinggame.dto.game.GameSessionCreateDTO;
import com.typinggame.dto.game.GameSessionDTO;
import com.typinggame.game.service.GameConfigService;
import com.typinggame.vo.ResultMessage;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/game/config")
public class GameConfigController {

    private final GameConfigService gameConfigService;

    public GameConfigController(GameConfigService gameConfigService) {
        this.gameConfigService = gameConfigService;
    }

    @GetMapping("/char-types")
    public ResponseEntity<ResultMessage<GameCharTypeConfigDTO>> getCharTypes(
            @RequestParam(value = "gameMode", required = false, defaultValue = "endless") String gameMode) {
        return ResponseEntity.ok(ResultMessage.success(gameConfigService.getCharTypes(getCurrentUserId(), gameMode)));
    }

    @PutMapping("/char-types")
    public ResponseEntity<ResultMessage<Boolean>> updateCharTypes(@RequestBody GameCharTypeConfigUpdateDTO request) {
        return ResponseEntity.ok(ResultMessage.success(gameConfigService.updateCharTypes(getCurrentUserId(), request)));
    }

    @GetMapping("/recommend")
    public ResponseEntity<ResultMessage<GameRecommendDTO>> getRecommend(
            @RequestParam(value = "gameMode", required = false, defaultValue = "endless") String gameMode) {
        return ResponseEntity.ok(ResultMessage.success(gameConfigService.getRecommend(getCurrentUserId(), gameMode)));
    }

    @GetMapping("/next-round")
    public ResponseEntity<ResultMessage<NextRoundResponseDTO>> getNextRound(
            @RequestParam(value = "sessionId", required = false) Long sessionId,
            @RequestParam(value = "roundNum", defaultValue = "1") int roundNum,
            @RequestParam(value = "gameMode", required = false, defaultValue = "endless") String gameMode,
            @RequestParam(value = "customDictIds", required = false) List<Integer> customDictIds,
            @RequestParam(value = "systemDictIds", required = false) List<Integer> systemDictIds) {
        return ResponseEntity.ok(ResultMessage.success(gameConfigService.getNextRound(getCurrentUserId(), sessionId, roundNum, gameMode, customDictIds, systemDictIds)));
    }

    @PostMapping("/sessions")
    public ResponseEntity<ResultMessage<GameSessionDTO>> createSession(@RequestBody GameSessionCreateDTO request) {
        return ResponseEntity.ok(ResultMessage.success(gameConfigService.createSession(getCurrentUserId(), request)));
    }

    private Integer getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return Integer.parseInt(authentication.getName());
    }
}
