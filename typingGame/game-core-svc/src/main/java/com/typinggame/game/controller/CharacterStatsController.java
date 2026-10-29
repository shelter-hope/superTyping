package com.typinggame.game.controller;

import com.typinggame.dto.stats.CharacterStatsBatchUpdateDTO;
import com.typinggame.game.entity.CharacterStats;
import com.typinggame.game.service.CharacterStatsService;
import com.typinggame.vo.ResultMessage;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/stats/character")
public class CharacterStatsController {

    private final CharacterStatsService characterStatsService;

    public CharacterStatsController(CharacterStatsService characterStatsService) {
        this.characterStatsService = characterStatsService;
    }

    @PostMapping("/batch")
    public ResponseEntity<ResultMessage<Void>> batchUpdate(@Valid @RequestBody CharacterStatsBatchUpdateDTO batchDTO) {
        characterStatsService.batchUpdate(getCurrentUserId(), batchDTO);
        return ResponseEntity.ok(ResultMessage.success());
    }

    @GetMapping
    public ResponseEntity<ResultMessage<List<CharacterStats>>> getMyStats() {
        return ResponseEntity.ok(ResultMessage.success(characterStatsService.getUserStats(getCurrentUserId())));
    }

    @GetMapping("/weak")
    public ResponseEntity<ResultMessage<List<CharacterStats>>> getWeakChars(@RequestParam(value = "limit", defaultValue = "10") int limit) {
        return ResponseEntity.ok(ResultMessage.success(characterStatsService.getWeakChars(getCurrentUserId(), limit)));
    }

    private Integer getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return Integer.parseInt(authentication.getName());
    }
}
