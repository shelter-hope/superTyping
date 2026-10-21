package com.typinggame.game.service.impl;

import com.typinggame.common.admin.AdminPageSupport;
import com.typinggame.entity.config.SystemMonsterCommoConfig;
import com.typinggame.entity.config.SystemMonsterTypeConfig;
import com.typinggame.entity.game.GameRecord;
import com.typinggame.game.admin.PartialUpdate;
import com.typinggame.entity.model.ModelConstant;
import com.typinggame.entity.model.ModelInitParams;
import com.typinggame.entity.model.UserModelAdaptLog;
import com.typinggame.game.repository.CharacterStatsRepository;
import com.typinggame.game.repository.GameRecordRepository;
import com.typinggame.game.repository.ModelConstantRepository;
import com.typinggame.game.repository.ModelInitParamsRepository;
import com.typinggame.game.repository.MonsterDataRepository;
import com.typinggame.game.repository.StringParentRepository;
import com.typinggame.game.repository.SystemMonsterCommoConfigRepository;
import com.typinggame.game.repository.SystemMonsterConfigRepository;
import com.typinggame.game.repository.SystemMonsterTypeConfigRepository;
import com.typinggame.game.repository.SystemParamRepository;
import com.typinggame.game.repository.UserGameModeConfigRepository;
import com.typinggame.game.repository.UserModelAdaptLogRepository;
import com.typinggame.game.service.InternalAdminService;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Date;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class InternalAdminServiceImpl implements InternalAdminService {
    private final GameRecordRepository gameRecordRepository;
    private final MonsterDataRepository monsterDataRepository;
    private final CharacterStatsRepository characterStatsRepository;
    private final StringParentRepository stringParentRepository;
    private final SystemParamRepository systemParamRepository;
    private final SystemMonsterTypeConfigRepository systemMonsterTypeConfigRepository;
    private final SystemMonsterCommoConfigRepository systemMonsterCommoConfigRepository;
    private final SystemMonsterConfigRepository systemMonsterConfigRepository;
    private final ModelConstantRepository modelConstantRepository;
    private final ModelInitParamsRepository modelInitParamsRepository;
    private final UserGameModeConfigRepository userGameModeConfigRepository;
    private final UserModelAdaptLogRepository userModelAdaptLogRepository;

    public InternalAdminServiceImpl(GameRecordRepository gameRecordRepository,
                                    MonsterDataRepository monsterDataRepository,
                                    CharacterStatsRepository characterStatsRepository,
                                    StringParentRepository stringParentRepository,
                                    SystemParamRepository systemParamRepository,
                                    SystemMonsterTypeConfigRepository systemMonsterTypeConfigRepository,
                                    SystemMonsterCommoConfigRepository systemMonsterCommoConfigRepository,
                                    SystemMonsterConfigRepository systemMonsterConfigRepository,
                                    ModelConstantRepository modelConstantRepository,
                                    ModelInitParamsRepository modelInitParamsRepository,
                                    UserGameModeConfigRepository userGameModeConfigRepository,
                                    UserModelAdaptLogRepository userModelAdaptLogRepository) {
        this.gameRecordRepository = gameRecordRepository;
        this.monsterDataRepository = monsterDataRepository;
        this.characterStatsRepository = characterStatsRepository;
        this.stringParentRepository = stringParentRepository;
        this.systemParamRepository = systemParamRepository;
        this.systemMonsterTypeConfigRepository = systemMonsterTypeConfigRepository;
        this.systemMonsterCommoConfigRepository = systemMonsterCommoConfigRepository;
        this.systemMonsterConfigRepository = systemMonsterConfigRepository;
        this.modelConstantRepository = modelConstantRepository;
        this.modelInitParamsRepository = modelInitParamsRepository;
        this.userGameModeConfigRepository = userGameModeConfigRepository;
        this.userModelAdaptLogRepository = userModelAdaptLogRepository;
    }

    @Override
    public Map<String, Object> getGameRecords(int page, int pageSize, Integer userId) {
        PageRequest pageable = pageable(page, pageSize);
        Page<GameRecord> records = userId == null
                ? gameRecordRepository.findAll(pageable)
                : gameRecordRepository.findAllByUserId(userId, pageable);
        return AdminPageSupport.pageMap("records", records);
    }

    @Override
    @Transactional
    public boolean updateGameRecord(Integer recordId, GameRecord updates) {
        GameRecord record = gameRecordRepository.findById(recordId).orElse(null);
        if (record == null) return false;
        if (updates.getTotalScore() != null) record.setTotalScore(updates.getTotalScore());
        if (updates.getRoundNum() != null) record.setRoundNum(updates.getRoundNum());
        if (updates.getAvgSpeed() != null) record.setAvgSpeed(updates.getAvgSpeed());
        if (updates.getAvgAccuracy() != null) record.setAvgAccuracy(updates.getAvgAccuracy());
        gameRecordRepository.save(record);
        return true;
    }

    @Override
    @Transactional
    public void deleteGameRecord(Integer recordId) {
        monsterDataRepository.deleteByRecordId(recordId);
        if (gameRecordRepository.existsById(recordId)) {
            gameRecordRepository.deleteById(recordId);
        }
    }

    @Override
    @Transactional
    public void deleteUserGameData(Integer userId) {
        monsterDataRepository.deleteByUserId(userId);
        gameRecordRepository.deleteByUserId(userId);
        characterStatsRepository.deleteByUserId(userId);
    }

    @Override public Map<String, Object> getCharacterStats(int page, int pageSize) { return AdminPageSupport.pageMap("items", characterStatsRepository.findAll(pageable(page, pageSize))); }
    @Override public Map<String, Object> getMonsterData(Integer recordId, int page, int pageSize) { return AdminPageSupport.pageMap("items", monsterDataRepository.findByRecordId(recordId, pageable(page, pageSize))); }
    @Override public Map<String, Object> getStringParents(int page, int pageSize) { return AdminPageSupport.pageMap("items", stringParentRepository.findAll(pageable(page, pageSize))); }
    @Override public Map<String, Object> getSystemParams(int page, int pageSize) { return AdminPageSupport.pageMap("items", systemParamRepository.findAll(pageable(page, pageSize))); }
    @Override public List<?> getSystemMonsterTypeConfigs() { return systemMonsterTypeConfigRepository.findAll(); }
    @Override public List<?> getSystemMonsterCommonConfigs() { return systemMonsterCommoConfigRepository.findAll(); }
    @Override public List<?> getMonsterConfigs() { return systemMonsterConfigRepository.findAll(); }
    @Override public Map<String, Object> getModelConstants(int page, int pageSize) { return AdminPageSupport.pageMap("items", modelConstantRepository.findAll(pageable(page, pageSize))); }
    @Override public Map<String, Object> getModelInitParams(int page, int pageSize) { return AdminPageSupport.pageMap("items", modelInitParamsRepository.findAll(pageable(page, pageSize))); }
    @Override public List<ModelInitParams> getModelInitParamsByMode(String gameMode) { return modelInitParamsRepository.findByGameMode(gameMode); }
    @Override public boolean updateModelConstant(Integer id, ModelConstant body) { return saveModelConstant(id, body); }
    @Override public boolean updateModelInitParam(Integer id, ModelInitParams body) { return saveModelInitParam(id, body); }
    @Override public boolean updateMonsterTypeConfig(Integer id, SystemMonsterTypeConfig body) { return saveMonsterType(id, body); }
    @Override public boolean updateMonsterCommonConfig(Integer id, SystemMonsterCommoConfig body) { return saveMonsterCommon(id, body); }
    @Override public Map<String, Object> getGameModeConfigs(int page, int pageSize) { return AdminPageSupport.pageMap("items", userGameModeConfigRepository.findAll(pageable(page, pageSize))); }
    @Override public Map<String, Object> getAdaptLogs(int page, int pageSize) { return AdminPageSupport.pageMap("logs", userModelAdaptLogRepository.findAll(pageable(page, pageSize))); }

    @Override
    public List<Map<String, Object>> getAdaptLogTrend(String startDate, String endDate) {
        LocalDate start = LocalDate.parse(startDate);
        LocalDate end = LocalDate.parse(endDate);
        List<UserModelAdaptLog> logs = userModelAdaptLogRepository.findByLogTimeGreaterThanEqualAndLogTimeLessThan(
                start.atStartOfDay(), end.plusDays(1).atStartOfDay());
        Map<String, List<UserModelAdaptLog>> grouped = logs.stream().collect(Collectors.groupingBy(l -> l.getLogTime().toLocalDate().format(DateTimeFormatter.ISO_DATE), LinkedHashMap::new, Collectors.toList()));
        List<Map<String, Object>> result = new ArrayList<>();
        grouped.forEach((date, list) -> result.add(AdminPageSupport.map("date", date, "avgAfterDifficulty", avgDifficulty(list), "count", list.size(), "maxRound", list.stream().map(UserModelAdaptLog::getRoundNum).max(Comparator.naturalOrder()).orElse(0))));
        return result;
    }

    @Override public Map<String, Object> getCharacterRules() { return AdminPageSupport.map("charTypes", List.of("lower", "upper", "number", "punct")); }
    @Override public Map<String, Object> getMovementRules() { return AdminPageSupport.map("minSpeed", 0.8, "maxSpeed", 3.0); }
    @Override public Map<String, Object> getDifficultyParams() { return AdminPageSupport.map("minDifficulty", 0.0, "maxDifficulty", 1.0); }

    private boolean saveModelConstant(Integer id, ModelConstant body) {
        ModelConstant existing = modelConstantRepository.findById(id).orElse(null);
        if (existing == null) return false;
        PartialUpdate.copyNonNull(body, existing, "constantId");
        existing.setUpdateTime(new Date());
        modelConstantRepository.save(existing);
        return true;
    }

    private boolean saveModelInitParam(Integer id, ModelInitParams body) {
        ModelInitParams existing = modelInitParamsRepository.findById(id).orElse(null);
        if (existing == null) return false;
        PartialUpdate.copyNonNull(body, existing, "paramId", "updatedAt");
        modelInitParamsRepository.save(existing);
        return true;
    }

    private boolean saveMonsterType(Integer id, SystemMonsterTypeConfig body) {
        SystemMonsterTypeConfig existing = systemMonsterTypeConfigRepository.findById(id).orElse(null);
        if (existing == null || body == null) return false;
        PartialUpdate.copyNonNull(body, existing, "id", "updatedAt");
        systemMonsterTypeConfigRepository.save(existing);
        return true;
    }

    private boolean saveMonsterCommon(Integer id, SystemMonsterCommoConfig body) {
        SystemMonsterCommoConfig existing = systemMonsterCommoConfigRepository.findById(id).orElse(null);
        if (existing == null || body == null) return false;
        PartialUpdate.copyNonNull(body, existing, "id", "updatedAt");
        systemMonsterCommoConfigRepository.save(existing);
        return true;
    }

    private BigDecimal avgDifficulty(List<UserModelAdaptLog> logs) {
        List<BigDecimal> values = logs.stream().map(UserModelAdaptLog::getAfterDifficulty).filter(Objects::nonNull).toList();
        if (values.isEmpty()) return null;
        return values.stream().reduce(BigDecimal.ZERO, BigDecimal::add).divide(BigDecimal.valueOf(values.size()), 2, java.math.RoundingMode.HALF_UP);
    }

    private PageRequest pageable(int page, int pageSize) {
        return PageRequest.of(Math.max(0, page - 1), Math.max(1, pageSize));
    }
}
