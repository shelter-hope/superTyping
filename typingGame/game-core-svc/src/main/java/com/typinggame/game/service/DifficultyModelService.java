package com.typinggame.game.service;

import com.typinggame.dto.game.GameAdjustParam;
import com.typinggame.enums.GameMode;

public interface DifficultyModelService {
    GameAdjustParam calculate(Integer userId, GameMode gameMode, int roundNum, Double recentAccuracy, Double recentSpeed, int weakCharCount);

    GameAdjustParam calculate(Integer userId, GameMode gameMode, int roundNum, Double recentAccuracy, Double recentSpeed, int weakCharCount, Double previousDifficulty);
}
