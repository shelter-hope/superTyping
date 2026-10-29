package com.typinggame.game.service;

import com.typinggame.game.entity.GameRecord;
import com.typinggame.game.entity.ModelConstant;
import com.typinggame.game.entity.ModelInitParams;

import java.util.List;
import java.util.Map;

public interface InternalAdminService {
    Map<String, Object> getGameRecords(int page, int pageSize, Integer userId, String gameMode);
    boolean updateGameRecord(Integer recordId, GameRecord updates);
    void deleteGameRecord(Integer recordId);
    void deleteUserGameData(Integer userId);
    Map<String, Object> getCharacterStats(int page, int pageSize, Integer userId, String charType, String charValue);
    /** 按用户查看按键混淆档案（逐键埋点聚合），limit 上限 100 */
    Map<String, Object> getKeyConfusion(Integer userId, int limit);
    Map<String, Object> getMonsterData(Integer recordId, int page, int pageSize);
    Map<String, Object> getStringParents(int page, int pageSize, Integer userId);
    Map<String, Object> getSystemParams(int page, int pageSize, String paramName);
    List<?> getSystemMonsterTypeConfigs();
    List<?> getSystemMonsterCommonConfigs();
    List<?> getMonsterConfigs();
    /** 按怪物类型修改积分/初始速度/初始长度/描述，类型不存在抛 400 */
    void updateMonsterConfig(String type, Map<String, Object> updates);
    Map<String, Object> getModelConstants(int page, int pageSize);
    Map<String, Object> getModelInitParams(int page, int pageSize);
    List<ModelInitParams> getModelInitParamsByMode(String gameMode);
    boolean updateModelConstant(Integer id, ModelConstant body);
    boolean updateModelInitParam(Integer id, ModelInitParams body);
    boolean updateMonsterTypeConfig(Integer id, com.typinggame.game.entity.SystemMonsterTypeConfig body);
    boolean updateMonsterCommonConfig(Integer id, com.typinggame.game.entity.SystemMonsterCommoConfig body);
    Map<String, Object> getGameModeConfigs(int page, int pageSize);
    Map<String, Object> getUserGameModeConfigs(int page, int pageSize, Integer userId, String modeName);
    Map<String, Object> getUserMonsterConfigs(int page, int pageSize, Integer userId, String type);
    Map<String, Object> getUserModelParams(int page, int pageSize, Integer userId, String gameMode);
    void updateGameModeConfig(Integer id, Map<String, Object> updates);
    Map<String, Object> getAdaptLogs(int page, int pageSize, Integer userId, String gameMode, String startDate, String endDate);
    List<Map<String, Object>> getAdaptLogTrend(Integer userId, String gameMode, String startDate, String endDate);
    Map<String, Object> getRules(String ruleKey);
    Map<String, Object> saveRules(String ruleKey, Map<String, Object> rules);
    List<Map<String, Object>> getConfigTemplates();
    Map<String, Object> getConfigTemplate(Integer id);
    Map<String, Object> saveConfigTemplate(String name, Object config, Integer operatorId);
    void deleteConfigTemplate(Integer id);
    Map<String, Object> getLeaderboardEntries(int page, int pageSize, Integer userId, String gameMode);
    Map<String, Object> getOverview();
}
