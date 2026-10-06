package com.typinggame.game.service;

import com.typinggame.dto.game.GameRoundCompleteDTO;
import com.typinggame.dto.game.GameRoundCompleteResultDTO;

public interface GameRoundService {
    GameRoundCompleteResultDTO completeRound(Integer userId, GameRoundCompleteDTO request);
}
