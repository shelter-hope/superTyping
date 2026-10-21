package com.typinggame.game.service;

import com.typinggame.entity.game.GameRecord;
import com.typinggame.entity.model.ModelConstant;
import com.typinggame.entity.model.ModelInitParams;

import java.util.List;
import java.util.Map;

public interface InternalAdminService {
    Map<String, Object> getGameRecords(int page, int pageSize, Integer userId);
    boolean updateGameRecord(Integer recordId, GameRecord updates);
    void deleteGameRecord(Integer recordId);
    void deleteUserGameData(Integer userId);
    Map<String, Object> getCharacterStats(int page, int pageSize);
    Map<String, Object> getMonsterData(Integer recordId, int page, int pageSize);
    Map<String, Object> getStringParents(int page, int pageSize);
    Map<String, Object> getSystemParams(int page, int pageSize);
    List<?> getSystemMonsterTypeConfigs();
    List<?> getSystemMonsterCommonConfigs();
    List<?> getMonsterConfigs();
    Map<String, Object> getModelConstants(int page, int pageSize);
    Map<String, Object> getModelInitParams(int page, int pageSize);
    List<ModelInitParams> getModelInitParamsByMode(String gameMode);
    boolean updateModelConstant(Integer id, ModelConstant body);
    boolean updateModelInitParam(Integer id, ModelInitParams body);
    boolean updateMonsterTypeConfig(Integer id, com.typinggame.entity.config.SystemMonsterTypeConfig body);
    boolean updateMonsterCommonConfig(Integer id, com.typinggame.entity.config.SystemMonsterCommoConfig body);
    Map<String, Object> getGameModeConfigs(int page, int pageSize);
    Map<String, Object> getAdaptLogs(int page, int pageSize);
    List<Map<String, Object>> getAdaptLogTrend(String startDate, String endDate);
    Map<String, Object> getCharacterRules();
    Map<String, Object> getMovementRules();
    Map<String, Object> getDifficultyParams();
}
