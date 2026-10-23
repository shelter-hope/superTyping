package com.typinggame.game.service;

import com.typinggame.dto.game.GameAdjustParam;
import com.typinggame.enums.GameMode;

public interface DifficultyModelService {
    GameAdjustParam calculate(Integer userId, GameMode gameMode, int roundNum, Double recentAccuracy, Double recentSpeed, int weakCharCount);

    GameAdjustParam calculate(Integer userId, GameMode gameMode, int roundNum, Double recentAccuracy, Double recentSpeed, int weakCharCount, Double previousDifficulty);

    /**
     * 把已确定的难度映射成速度、数量、词长和类型占比，不再积分。
     * difficulty 为空时使用玩家上次难度或规则初始值。
     */
    GameAdjustParam project(Integer userId, GameMode gameMode, int roundNum, Double difficulty);
}
