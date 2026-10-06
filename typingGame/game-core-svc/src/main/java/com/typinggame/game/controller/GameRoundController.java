package com.typinggame.game.controller;

import com.typinggame.dto.game.GameRoundCompleteDTO;
import com.typinggame.dto.game.GameRoundCompleteResultDTO;
import com.typinggame.game.service.GameRoundService;
import com.typinggame.vo.ResultMessage;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/game/rounds")
public class GameRoundController {
    private final GameRoundService gameRoundService;

    public GameRoundController(GameRoundService gameRoundService) {
        this.gameRoundService = gameRoundService;
    }

    @PostMapping("/{roundNum}/complete")
    public ResponseEntity<ResultMessage<GameRoundCompleteResultDTO>> completeRound(
            @PathVariable Integer roundNum,
            @Valid @RequestBody GameRoundCompleteDTO request) {
        if (!roundNum.equals(request.getRoundNum())) {
            throw new IllegalArgumentException("URL轮次与请求轮次不一致");
        }
        return ResponseEntity.ok(ResultMessage.success(
                gameRoundService.completeRound(getCurrentUserId(), request)));
    }

    private Integer getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return Integer.parseInt(authentication.getName());
    }
}
