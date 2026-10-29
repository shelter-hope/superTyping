package com.typinggame.game.service;

import com.typinggame.dto.stats.CharacterStatsBatchUpdateDTO;
import com.typinggame.game.entity.CharacterStats;

import java.util.List;

public interface CharacterStatsService {
    void batchUpdate(Integer userId, CharacterStatsBatchUpdateDTO batchDTO);

    List<CharacterStats> getUserStats(Integer userId);

    List<CharacterStats> getWeakChars(Integer userId, int limit);

    List<CharacterStats> getLowAccuracyChars(Integer userId, int limit);
}
