package com.typinggame.game.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.typinggame.common.admin.AdminFilter;
import com.typinggame.common.admin.AdminPageSupport;
import com.typinggame.common.exception.BusinessException;
import com.typinggame.entity.config.ConfigTemplate;
import com.typinggame.entity.config.SystemGameModeConfig;
import com.typinggame.entity.config.SystemMonsterCommoConfig;
import com.typinggame.entity.config.SystemMonsterTypeConfig;
import com.typinggame.entity.config.SystemParam;
import com.typinggame.entity.game.GameRecord;
import com.typinggame.entity.game.StringParent;
import com.typinggame.entity.game.SystemMonsterConfig;
import com.typinggame.game.admin.PartialUpdate;
import com.typinggame.entity.model.ModelConstant;
import com.typinggame.entity.model.ModelInitParams;
import com.typinggame.entity.model.UserModelAdaptLog;
import com.typinggame.entity.user.CharacterStats;
import com.typinggame.game.repository.CharacterStatsRepository;
import com.typinggame.game.repository.ConfigTemplateRepository;
import com.typinggame.game.repository.GameModeConfigRepository;
import com.typinggame.game.repository.GameRecordRepository;
import com.typinggame.game.repository.ModelConstantRepository;
import com.typinggame.game.repository.ModelInitParamsRepository;
import com.typinggame.game.repository.MonsterDataRepository;
import com.typinggame.game.repository.StringParentRepository;
import com.typinggame.game.repository.SystemMonsterCommoConfigRepository;
import com.typinggame.game.repository.SystemMonsterConfigRepository;
import com.typinggame.game.repository.SystemMonsterTypeConfigRepository;
import com.typinggame.game.repository.SystemParamRepository;
import com.typinggame.game.repository.UserModelAdaptLogRepository;
import com.typinggame.game.service.InternalAdminService;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class InternalAdminServiceImpl implements InternalAdminService {

    /** 规则 key -> system_params.param_name */
    static final Map<String, String> RULE_PARAMS = Map.of(
            "character-rules", "admin.character_rules",
            "movement-rules", "admin.movement_rules",
            "difficulty-params", "admin.difficulty_params");

    private static final Map<String, Map<String, Object>> RULE_DEFAULTS = Map.of(
            "character-rules", Map.of("charTypes", List.of("lower", "upper", "number", "punct")),
            "movement-rules", Map.of("minSpeed", 0.8, "maxSpeed", 3.0),
            "difficulty-params", Map.of("minDifficulty", 0.0, "maxDifficulty", 1.0));

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
    private final GameModeConfigRepository gameModeConfigRepository;
    private final UserModelAdaptLogRepository userModelAdaptLogRepository;
    private final ConfigTemplateRepository configTemplateRepository;
    private final ObjectMapper objectMapper;

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
                                    GameModeConfigRepository gameModeConfigRepository,
                                    UserModelAdaptLogRepository userModelAdaptLogRepository,
                                    ConfigTemplateRepository configTemplateRepository,
                                    ObjectMapper objectMapper) {
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
        this.gameModeConfigRepository = gameModeConfigRepository;
        this.userModelAdaptLogRepository = userModelAdaptLogRepository;
        this.configTemplateRepository = configTemplateRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    public Map<String, Object> getGameRecords(int page, int pageSize, Integer userId, String gameMode) {
        AdminFilter<GameRecord> f = AdminFilter.of(GameRecord.class).eq("userId", userId).eq("gameMode", gameMode);
        return AdminPageSupport.pageMap("records", gameRecordRepository.findAll(f, pageable(page, pageSize, "recordId")));
    }

    @Override
    @Transactional
    public boolean updateGameRecord(Integer recordId, GameRecord updates) {
        GameRecord record = gameRecordRepository.findById(recordId).orElse(null);
        if (record == null) return false;
        if (updates.getTotalScore() != null) record.setTotalScore(updates.getTotalScore());
        if (updates.getRoundNum() != null) record.setRoundNum(updates.getRoundNum());
        if (updates.getAvgSpeed() != null) record.setAvgSpeed(updates.getAvgSpeed());
        if (updates.getPeakSpeed() != null) record.setPeakSpeed(updates.getPeakSpeed());
        if (updates.getAvgAccuracy() != null) record.setAvgAccuracy(updates.getAvgAccuracy());
        if (updates.getPeakAccuracy() != null) record.setPeakAccuracy(updates.getPeakAccuracy());
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

    @Override
    public Map<String, Object> getCharacterStats(int page, int pageSize, Integer userId, String charType, String charValue) {
        AdminFilter<CharacterStats> f = AdminFilter.of(CharacterStats.class)
                .eq("userId", userId).eq("charType", charType).eq("charValue", charValue);
        return AdminPageSupport.pageMap("items", characterStatsRepository.findAll(f, pageable(page, pageSize, "statId")));
    }

    @Override
    public Map<String, Object> getMonsterData(Integer recordId, int page, int pageSize) {
        return AdminPageSupport.pageMap("items", monsterDataRepository.findByRecordId(recordId, pageable(page, pageSize, null)));
    }

    @Override
    public Map<String, Object> getStringParents(int page, int pageSize, Integer userId) {
        AdminFilter<StringParent> f = AdminFilter.of(StringParent.class).eq("userId", userId);
        return AdminPageSupport.pageMap("items", stringParentRepository.findAll(f, pageable(page, pageSize, "id")));
    }

    @Override
    public Map<String, Object> getSystemParams(int page, int pageSize, String paramName) {
        AdminFilter<SystemParam> f = AdminFilter.of(SystemParam.class).like("paramName", paramName);
        return AdminPageSupport.pageMap("items", systemParamRepository.findAll(f, pageable(page, pageSize, null)));
    }

    @Override public List<?> getSystemMonsterTypeConfigs() { return systemMonsterTypeConfigRepository.findAll(); }
    @Override public List<?> getSystemMonsterCommonConfigs() { return systemMonsterCommoConfigRepository.findAll(); }
    @Override public List<?> getMonsterConfigs() { return systemMonsterConfigRepository.findAll(); }

    @Override
    @Transactional
    public void updateMonsterConfig(String type, Map<String, Object> updates) {
        SystemMonsterConfig config = systemMonsterConfigRepository.findByType(type)
                .orElseThrow(() -> BusinessException.badRequest("怪物类型不存在: " + type));
        Integer score = toInteger(updates.get("score"), "score");
        if (score != null) {
            if (score < 0) throw BusinessException.badRequest("积分不能为负数");
            config.setScore(score);
        }
        BigDecimal initSpeed = toDecimal(updates.get("initSpeed"), "initSpeed");
        if (initSpeed != null) {
            if (initSpeed.signum() <= 0) throw BusinessException.badRequest("初始速度必须大于 0");
            config.setInitSpeed(initSpeed.doubleValue());
        }
        Integer initStrLength = toInteger(updates.get("initStrLength"), "initStrLength");
        if (initStrLength != null) {
            if (initStrLength < 1) throw BusinessException.badRequest("初始长度至少为 1");
            config.setInitStrLength(initStrLength);
        }
        if (updates.get("description") instanceof String description) {
            config.setDescription(description);
        }
        systemMonsterConfigRepository.save(config);
    }

    @Override public Map<String, Object> getModelConstants(int page, int pageSize) { return AdminPageSupport.pageMap("items", modelConstantRepository.findAll(pageable(page, pageSize, null))); }
    @Override public Map<String, Object> getModelInitParams(int page, int pageSize) { return AdminPageSupport.pageMap("items", modelInitParamsRepository.findAll(pageable(page, pageSize, null))); }
    @Override public List<ModelInitParams> getModelInitParamsByMode(String gameMode) { return modelInitParamsRepository.findByGameMode(gameMode); }
    @Override public boolean updateModelConstant(Integer id, ModelConstant body) { return saveModelConstant(id, body); }
    @Override public boolean updateModelInitParam(Integer id, ModelInitParams body) { return saveModelInitParam(id, body); }
    @Override
    public boolean updateMonsterTypeConfig(Integer id, SystemMonsterTypeConfig body) {
        return saveMonsterType(id, body);
    }

    @Override
    public boolean updateMonsterCommonConfig(Integer id, SystemMonsterCommoConfig body) {
        return saveMonsterCommon(id, body);
    }

    @Override
    public Map<String, Object> getGameModeConfigs(int page, int pageSize) {
        return AdminPageSupport.pageMap("items", gameModeConfigRepository.findAll(pageable(page, pageSize, "id")));
    }

    @Override
    @Transactional
    public void updateGameModeConfig(Integer id, Map<String, Object> updates) {
        SystemGameModeConfig config = gameModeConfigRepository.findById(id)
                .orElseThrow(() -> BusinessException.badRequest("游戏模式配置不存在: " + id));
        if (updates.get("description") instanceof String description) config.setDescription(description);
        BigDecimal k0 = toDecimal(updates.get("mode_k0"), "mode_k0");
        if (k0 != null) config.setMode_k0(k0);
        Integer initNum = toInteger(updates.get("monster_init_num"), "monster_init_num");
        if (initNum != null) {
            if (initNum < 1) throw BusinessException.badRequest("初始怪物数量至少为 1");
            config.setMonster_init_num(initNum);
        }
        Object typePro = updates.get("monster_type_pro");
        if (typePro != null) config.setMonster_type_pro(typePro instanceof String s ? s : writeJson(typePro));
        config.setUpdateTime(new Date());
        gameModeConfigRepository.save(config);
    }

    @Override
    public Map<String, Object> getAdaptLogs(int page, int pageSize, Integer userId, String gameMode, String startDate, String endDate) {
        AdminFilter<UserModelAdaptLog> f = adaptLogFilter(userId, gameMode, parseDate(startDate), parseDate(endDate));
        Page<UserModelAdaptLog> logs = userModelAdaptLogRepository.findAll(f, pageable(page, pageSize, "logId"));
        List<Map<String, Object>> items = logs.getContent().stream().map(this::adaptLogView).toList();
        return AdminPageSupport.map("logs", items, "total", logs.getTotalElements());
    }

    @Override
    public List<Map<String, Object>> getAdaptLogTrend(Integer userId, String gameMode, String startDate, String endDate) {
        LocalDate start = parseDate(startDate);
        LocalDate end = parseDate(endDate);
        if (start == null || end == null) throw BusinessException.badRequest("startDate 和 endDate 必填，格式 yyyy-MM-dd");
        List<UserModelAdaptLog> logs = userModelAdaptLogRepository.findAll(
                adaptLogFilter(userId, gameMode, start, end), Sort.by("logTime"));
        Map<String, List<UserModelAdaptLog>> grouped = logs.stream().collect(Collectors.groupingBy(
                l -> l.getLogTime().toLocalDate().format(DateTimeFormatter.ISO_DATE), LinkedHashMap::new, Collectors.toList()));
        List<Map<String, Object>> result = new ArrayList<>();
        grouped.forEach((date, list) -> result.add(AdminPageSupport.map(
                "date", date,
                "avgAfterDifficulty", avgDifficulty(list),
                "count", list.size(),
                "maxRound", list.stream().map(UserModelAdaptLog::getRoundNum).filter(Objects::nonNull).max(Comparator.naturalOrder()).orElse(0))));
        return result;
    }

    @Override
    public Map<String, Object> getRules(String ruleKey) {
        String paramName = ruleParam(ruleKey);
        return systemParamRepository.findByParamName(paramName)
                .map(p -> readJsonMap(p.getParamValue()))
                .orElseGet(() -> new LinkedHashMap<>(RULE_DEFAULTS.get(ruleKey)));
    }

    @Override
    @Transactional
    public Map<String, Object> saveRules(String ruleKey, Map<String, Object> rules) {
        String paramName = ruleParam(ruleKey);
        if (rules == null || rules.isEmpty()) throw BusinessException.badRequest("配置内容不能为空");
        validateRules(ruleKey, rules);
        String json = writeJson(rules);
        if (json.length() > 2000) throw BusinessException.badRequest("配置内容过长（最多 2000 字符）");
        SystemParam param = systemParamRepository.findByParamName(paramName).orElseGet(() -> {
            SystemParam p = new SystemParam();
            p.setParamName(paramName);
            p.setParamType("json");
            p.setDescription("后台配置：" + ruleKey);
            p.setIsSystem(true);
            p.setIsActive(true);
            p.setCreateTime(new Date());
            return p;
        });
        param.setParamValue(json);
        param.setUpdateTime(new Date());
        systemParamRepository.save(param);
        return rules;
    }

    @Override
    public List<Map<String, Object>> getConfigTemplates() {
        return configTemplateRepository.findAll(Sort.by(Sort.Direction.DESC, "id")).stream()
                .map(t -> AdminPageSupport.map("id", t.getId(), "name", t.getName(),
                        "createdBy", t.getCreatedBy(), "createTime", t.getCreateTime(), "updateTime", t.getUpdateTime()))
                .toList();
    }

    @Override
    public Map<String, Object> getConfigTemplate(Integer id) {
        ConfigTemplate t = configTemplateRepository.findById(id)
                .orElseThrow(() -> BusinessException.notFound("配置模板不存在"));
        return templateView(t);
    }

    @Override
    @Transactional
    public Map<String, Object> saveConfigTemplate(String name, Object config, Integer operatorId) {
        if (name == null || name.isBlank() || name.length() > 100) {
            throw BusinessException.badRequest("模板名称必须为 1-100 个字符");
        }
        if (config == null) throw BusinessException.badRequest("模板配置不能为空");
        ConfigTemplate t = configTemplateRepository.findByName(name.trim()).orElseGet(ConfigTemplate::new);
        t.setName(name.trim());
        t.setConfigJson(writeJson(config));
        if (t.getCreatedBy() == null) t.setCreatedBy(operatorId);
        return templateView(configTemplateRepository.save(t));
    }

    @Override
    @Transactional
    public void deleteConfigTemplate(Integer id) {
        if (!configTemplateRepository.existsById(id)) throw BusinessException.notFound("配置模板不存在");
        configTemplateRepository.deleteById(id);
    }

    @Override
    public Map<String, Object> getOverview() {
        Date todayStart = Date.from(LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant());
        return AdminPageSupport.map(
                "recordCount", gameRecordRepository.count(),
                "recordsToday", gameRecordRepository.count(AdminFilter.of(GameRecord.class).between("createTime", todayStart, null)),
                "playersToday", gameRecordRepository.countDistinctUsersSince(todayStart));
    }

    private AdminFilter<UserModelAdaptLog> adaptLogFilter(Integer userId, String gameMode, LocalDate start, LocalDate end) {
        return AdminFilter.of(UserModelAdaptLog.class)
                .eq("user.userId", userId)
                .eq("gameRecord.gameMode", gameMode)
                .between("logTime", start == null ? null : start.atStartOfDay(), end == null ? null : end.plusDays(1).atStartOfDay());
    }

    private Map<String, Object> adaptLogView(UserModelAdaptLog l) {
        return AdminPageSupport.map(
                "logId", l.getLogId(),
                "userId", l.getUser() != null ? l.getUser().getUserId() : null,
                "gameRecordId", l.getGameRecord() != null ? l.getGameRecord().getRecordId() : null,
                "gameMode", l.getGameRecord() != null ? l.getGameRecord().getGameMode() : null,
                "roundNum", l.getRoundNum(),
                "beforeDifficulty", l.getBeforeDifficulty(),
                "afterDifficulty", l.getAfterDifficulty(),
                "adjustParams", l.getAdjustParams(),
                "triggerRule", l.getTriggerRule(),
                "logTime", l.getLogTime());
    }

    private Map<String, Object> templateView(ConfigTemplate t) {
        Object config;
        try {
            config = objectMapper.readValue(t.getConfigJson(), Object.class);
        } catch (JsonProcessingException e) {
            config = t.getConfigJson();
        }
        return AdminPageSupport.map("id", t.getId(), "name", t.getName(), "config", config,
                "createdBy", t.getCreatedBy(), "createTime", t.getCreateTime(), "updateTime", t.getUpdateTime());
    }

    private void validateRules(String ruleKey, Map<String, Object> rules) {
        switch (ruleKey) {
            case "movement-rules" -> {
                BigDecimal min = toDecimal(rules.get("minSpeed"), "minSpeed");
                BigDecimal max = toDecimal(rules.get("maxSpeed"), "maxSpeed");
                if (min != null && min.signum() <= 0) throw BusinessException.badRequest("minSpeed 必须大于 0");
                if (min != null && max != null && min.compareTo(max) > 0) throw BusinessException.badRequest("minSpeed 不能大于 maxSpeed");
            }
            case "difficulty-params" -> {
                BigDecimal min = toDecimal(rules.get("minDifficulty"), "minDifficulty");
                BigDecimal max = toDecimal(rules.get("maxDifficulty"), "maxDifficulty");
                if (min != null && max != null && min.compareTo(max) > 0) throw BusinessException.badRequest("minDifficulty 不能大于 maxDifficulty");
            }
            case "character-rules" -> {
                Object types = rules.get("charTypes");
                if (types != null && !(types instanceof List<?> list && !list.isEmpty())) {
                    throw BusinessException.badRequest("charTypes 必须是非空数组");
                }
            }
            default -> { }
        }
    }

    private String ruleParam(String ruleKey) {
        String paramName = RULE_PARAMS.get(ruleKey);
        if (paramName == null) throw BusinessException.notFound("未知配置项: " + ruleKey);
        return paramName;
    }

    private Map<String, Object> readJsonMap(String json) {
        try {
            return objectMapper.readValue(json, new TypeReference<LinkedHashMap<String, Object>>() { });
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("system_params 中的配置不是合法 JSON", e);
        }
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw BusinessException.badRequest("配置无法序列化为 JSON");
        }
    }

    private static LocalDate parseDate(String value) {
        if (value == null || value.isBlank()) return null;
        try {
            return LocalDate.parse(value.trim());
        } catch (DateTimeParseException e) {
            throw BusinessException.badRequest("日期格式应为 yyyy-MM-dd: " + value);
        }
    }

    private static Integer toInteger(Object value, String field) {
        BigDecimal d = toDecimal(value, field);
        return d == null ? null : d.intValue();
    }

    private static BigDecimal toDecimal(Object value, String field) {
        if (value == null) return null;
        try {
            return new BigDecimal(value.toString().trim());
        } catch (NumberFormatException e) {
            throw BusinessException.badRequest(field + " 必须是数字");
        }
    }

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
        return values.stream().reduce(BigDecimal.ZERO, BigDecimal::add).divide(BigDecimal.valueOf(values.size()), 2, RoundingMode.HALF_UP);
    }

    private PageRequest pageable(int page, int pageSize, String sortDescField) {
        int p = Math.max(0, page - 1);
        int size = Math.min(200, Math.max(1, pageSize));
        return sortDescField == null ? PageRequest.of(p, size) : PageRequest.of(p, size, Sort.by(Sort.Direction.DESC, sortDescField));
    }
}
