package com.typinggame.game.service.impl;

import com.typinggame.entity.config.SystemMonsterCommoConfig;
import com.typinggame.entity.config.SystemMonsterTypeConfig;
import com.typinggame.entity.game.GameRecord;
import com.typinggame.entity.model.ModelConstant;
import com.typinggame.entity.model.ModelInitParams;
import com.typinggame.game.admin.ConfigAdminService;
import com.typinggame.game.admin.ConfusionAdminService;
import com.typinggame.game.admin.LeaderboardAdminService;
import com.typinggame.game.admin.RecordAdminService;
import com.typinggame.game.admin.RuleAdminService;
import com.typinggame.game.service.InternalAdminService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/** 管理端门面：按资源委托 Record/Config/Rule/Leaderboard Admin。 */
@Service
public class InternalAdminServiceImpl implements InternalAdminService {
    private final RecordAdminService recordAdminService;
    private final ConfigAdminService configAdminService;
    private final RuleAdminService ruleAdminService;
    private final LeaderboardAdminService leaderboardAdminService;
    private final ConfusionAdminService confusionAdminService;

    public InternalAdminServiceImpl(RecordAdminService recordAdminService,
                                    ConfigAdminService configAdminService,
                                    RuleAdminService ruleAdminService,
                                    LeaderboardAdminService leaderboardAdminService,
                                    ConfusionAdminService confusionAdminService) {
        this.recordAdminService = recordAdminService;
        this.configAdminService = configAdminService;
        this.ruleAdminService = ruleAdminService;
        this.leaderboardAdminService = leaderboardAdminService;
        this.confusionAdminService = confusionAdminService;
    }

    @Override
    public Map<String, Object> getGameRecords(int page, int pageSize, Integer userId, String gameMode) {
        return recordAdminService.getGameRecords(page, pageSize, userId, gameMode);
    }

    @Override
    public boolean updateGameRecord(Integer recordId, GameRecord updates) {
        return recordAdminService.updateGameRecord(recordId, updates);
    }

    @Override
    public void deleteGameRecord(Integer recordId) {
        recordAdminService.deleteGameRecord(recordId);
    }

    @Override
    public void deleteUserGameData(Integer userId) {
        recordAdminService.deleteUserGameData(userId);
    }

    @Override
    public Map<String, Object> getCharacterStats(int page, int pageSize, Integer userId, String charType, String charValue) {
        return recordAdminService.getCharacterStats(page, pageSize, userId, charType, charValue);
    }

    @Override
    public Map<String, Object> getKeyConfusion(Integer userId, int limit) {
        return confusionAdminService.getKeyConfusion(userId, limit);
    }

    @Override
    public Map<String, Object> getMonsterData(Integer recordId, int page, int pageSize) {
        return recordAdminService.getMonsterData(recordId, page, pageSize);
    }

    @Override
    public Map<String, Object> getStringParents(int page, int pageSize, Integer userId) {
        return recordAdminService.getStringParents(page, pageSize, userId);
    }

    @Override
    public Map<String, Object> getSystemParams(int page, int pageSize, String paramName) {
        return configAdminService.getSystemParams(page, pageSize, paramName);
    }

    @Override
    public List<?> getSystemMonsterTypeConfigs() {
        return configAdminService.getSystemMonsterTypeConfigs();
    }

    @Override
    public List<?> getSystemMonsterCommonConfigs() {
        return configAdminService.getSystemMonsterCommonConfigs();
    }

    @Override
    public List<?> getMonsterConfigs() {
        return configAdminService.getMonsterConfigs();
    }

    @Override
    public void updateMonsterConfig(String type, Map<String, Object> updates) {
        configAdminService.updateMonsterConfig(type, updates);
    }

    @Override
    public Map<String, Object> getModelConstants(int page, int pageSize) {
        return configAdminService.getModelConstants(page, pageSize);
    }

    @Override
    public Map<String, Object> getModelInitParams(int page, int pageSize) {
        return configAdminService.getModelInitParams(page, pageSize);
    }

    @Override
    public List<ModelInitParams> getModelInitParamsByMode(String gameMode) {
        return configAdminService.getModelInitParamsByMode(gameMode);
    }

    @Override
    public boolean updateModelConstant(Integer id, ModelConstant body) {
        return configAdminService.updateModelConstant(id, body);
    }

    @Override
    public boolean updateModelInitParam(Integer id, ModelInitParams body) {
        return configAdminService.updateModelInitParam(id, body);
    }

    @Override
    public boolean updateMonsterTypeConfig(Integer id, SystemMonsterTypeConfig body) {
        return configAdminService.updateMonsterTypeConfig(id, body);
    }

    @Override
    public boolean updateMonsterCommonConfig(Integer id, SystemMonsterCommoConfig body) {
        return configAdminService.updateMonsterCommonConfig(id, body);
    }

    @Override
    public Map<String, Object> getGameModeConfigs(int page, int pageSize) {
        return configAdminService.getGameModeConfigs(page, pageSize);
    }

    @Override
    public void updateGameModeConfig(Integer id, Map<String, Object> updates) {
        configAdminService.updateGameModeConfig(id, updates);
    }

    @Override
    public Map<String, Object> getAdaptLogs(int page, int pageSize, Integer userId, String gameMode, String startDate, String endDate) {
        return configAdminService.getAdaptLogs(page, pageSize, userId, gameMode, startDate, endDate);
    }

    @Override
    public List<Map<String, Object>> getAdaptLogTrend(Integer userId, String gameMode, String startDate, String endDate) {
        return configAdminService.getAdaptLogTrend(userId, gameMode, startDate, endDate);
    }

    @Override
    public Map<String, Object> getRules(String ruleKey) {
        return ruleAdminService.getRules(ruleKey);
    }

    @Override
    public Map<String, Object> saveRules(String ruleKey, Map<String, Object> rules) {
        return ruleAdminService.saveRules(ruleKey, rules);
    }

    @Override
    public List<Map<String, Object>> getConfigTemplates() {
        return ruleAdminService.getConfigTemplates();
    }

    @Override
    public Map<String, Object> getConfigTemplate(Integer id) {
        return ruleAdminService.getConfigTemplate(id);
    }

    @Override
    public Map<String, Object> saveConfigTemplate(String name, Object config, Integer operatorId) {
        return ruleAdminService.saveConfigTemplate(name, config, operatorId);
    }

    @Override
    public void deleteConfigTemplate(Integer id) {
        ruleAdminService.deleteConfigTemplate(id);
    }

    @Override
    public Map<String, Object> getLeaderboardEntries(int page, int pageSize, Integer userId, String gameMode) {
        return leaderboardAdminService.getLeaderboardEntries(page, pageSize, userId, gameMode);
    }

    @Override
    public Map<String, Object> getOverview() {
        return leaderboardAdminService.getOverview();
    }
}
