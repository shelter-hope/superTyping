package com.typinggame.game.admin;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.typinggame.common.admin.AdminFilter;
import com.typinggame.common.admin.AdminPageSupport;
import com.typinggame.common.exception.BusinessException;
import com.typinggame.entity.config.SystemGameModeConfig;
import com.typinggame.game.entity.SystemMonsterCommoConfig;
import com.typinggame.game.entity.SystemMonsterTypeConfig;
import com.typinggame.game.entity.SystemParam;
import com.typinggame.entity.game.SystemMonsterConfig;
import com.typinggame.game.entity.ModelConstant;
import com.typinggame.entity.model.ModelInitParams;
import com.typinggame.game.entity.UserModelAdaptLog;
import com.typinggame.game.repository.GameModeConfigRepository;
import com.typinggame.game.repository.ModelConstantRepository;
import com.typinggame.game.repository.ModelInitParamsRepository;
import com.typinggame.game.repository.SystemMonsterCommoConfigRepository;
import com.typinggame.game.repository.SystemMonsterConfigRepository;
import com.typinggame.game.repository.SystemMonsterTypeConfigRepository;
import com.typinggame.game.repository.SystemParamRepository;
import com.typinggame.game.repository.UserModelAdaptLogRepository;
import com.typinggame.game.repository.UserGameModeConfigRepository;
import com.typinggame.game.repository.UserModelParamsRepository;
import com.typinggame.game.repository.UserMonsterConfigRepository;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
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
public class ConfigAdminService {
    private final SystemParamRepository systemParamRepository;
    private final SystemMonsterTypeConfigRepository systemMonsterTypeConfigRepository;
    private final SystemMonsterCommoConfigRepository systemMonsterCommoConfigRepository;
    private final SystemMonsterConfigRepository systemMonsterConfigRepository;
    private final ModelConstantRepository modelConstantRepository;
    private final ModelInitParamsRepository modelInitParamsRepository;
    private final GameModeConfigRepository gameModeConfigRepository;
    private final UserModelAdaptLogRepository userModelAdaptLogRepository;
    private final UserGameModeConfigRepository userGameModeConfigRepository;
    private final UserMonsterConfigRepository userMonsterConfigRepository;
    private final UserModelParamsRepository userModelParamsRepository;
    private final ObjectMapper objectMapper;

    public ConfigAdminService(SystemParamRepository systemParamRepository,
                              SystemMonsterTypeConfigRepository systemMonsterTypeConfigRepository,
                              SystemMonsterCommoConfigRepository systemMonsterCommoConfigRepository,
                              SystemMonsterConfigRepository systemMonsterConfigRepository,
                              ModelConstantRepository modelConstantRepository,
                              ModelInitParamsRepository modelInitParamsRepository,
                              GameModeConfigRepository gameModeConfigRepository,
                              UserModelAdaptLogRepository userModelAdaptLogRepository,
                              UserGameModeConfigRepository userGameModeConfigRepository,
                              UserMonsterConfigRepository userMonsterConfigRepository,
                              UserModelParamsRepository userModelParamsRepository,
                              ObjectMapper objectMapper) {
        this.systemParamRepository = systemParamRepository;
        this.systemMonsterTypeConfigRepository = systemMonsterTypeConfigRepository;
        this.systemMonsterCommoConfigRepository = systemMonsterCommoConfigRepository;
        this.systemMonsterConfigRepository = systemMonsterConfigRepository;
        this.modelConstantRepository = modelConstantRepository;
        this.modelInitParamsRepository = modelInitParamsRepository;
        this.gameModeConfigRepository = gameModeConfigRepository;
        this.userModelAdaptLogRepository = userModelAdaptLogRepository;
        this.userGameModeConfigRepository = userGameModeConfigRepository;
        this.userMonsterConfigRepository = userMonsterConfigRepository;
        this.userModelParamsRepository = userModelParamsRepository;
        this.objectMapper = objectMapper;
    }

    public Map<String, Object> getUserGameModeConfigs(int page, int pageSize, Integer userId, String modeName) {
        AdminFilter<com.typinggame.game.entity.UserGameModeConfig> f = AdminFilter.of(com.typinggame.game.entity.UserGameModeConfig.class)
                .eq("userId", userId).eq("modeName", modeName);
        return AdminPageSupport.pageMap("items", userGameModeConfigRepository.findAll(f, pageable(page, pageSize, "id")));
    }

    public Map<String, Object> getUserMonsterConfigs(int page, int pageSize, Integer userId, String type) {
        AdminFilter<com.typinggame.game.entity.UserMonsterConfig> f = AdminFilter.of(com.typinggame.game.entity.UserMonsterConfig.class)
                .eq("userId", userId).eq("type", type);
        return AdminPageSupport.pageMap("items", userMonsterConfigRepository.findAll(f, pageable(page, pageSize, "id")));
    }

    public Map<String, Object> getUserModelParams(int page, int pageSize, Integer userId, String gameMode) {
        AdminFilter<com.typinggame.game.entity.UserModelParams> f = AdminFilter.of(com.typinggame.game.entity.UserModelParams.class)
                .eq("userId", userId).eq("gameMode", gameMode);
        return AdminPageSupport.pageMap("items", userModelParamsRepository.findAll(f, pageable(page, pageSize, "paramId")));
    }

    public Map<String, Object> getSystemParams(int page, int pageSize, String paramName) {
        AdminFilter<SystemParam> f = AdminFilter.of(SystemParam.class).like("paramName", paramName);
        return AdminPageSupport.pageMap("items", systemParamRepository.findAll(f, pageable(page, pageSize, null)));
    }

    public List<?> getSystemMonsterTypeConfigs() { return systemMonsterTypeConfigRepository.findAll(); }
    public List<?> getSystemMonsterCommonConfigs() { return systemMonsterCommoConfigRepository.findAll(); }
    public List<?> getMonsterConfigs() { return systemMonsterConfigRepository.findAll(); }

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

    public Map<String, Object> getModelConstants(int page, int pageSize) {
        return AdminPageSupport.pageMap("items", modelConstantRepository.findAll(pageable(page, pageSize, null)));
    }

    public Map<String, Object> getModelInitParams(int page, int pageSize) {
        return AdminPageSupport.pageMap("items", modelInitParamsRepository.findAll(pageable(page, pageSize, null)));
    }

    public List<ModelInitParams> getModelInitParamsByMode(String gameMode) {
        return modelInitParamsRepository.findByGameMode(gameMode);
    }

    public boolean updateModelConstant(Integer id, ModelConstant body) {
        ModelConstant existing = modelConstantRepository.findById(id).orElse(null);
        if (existing == null) return false;
        PartialUpdate.copyNonNull(body, existing, "constantId");
        existing.setUpdateTime(new Date());
        modelConstantRepository.save(existing);
        return true;
    }

    public boolean updateModelInitParam(Integer id, ModelInitParams body) {
        ModelInitParams existing = modelInitParamsRepository.findById(id).orElse(null);
        if (existing == null) return false;
        PartialUpdate.copyNonNull(body, existing, "paramId", "updatedAt");
        modelInitParamsRepository.save(existing);
        return true;
    }

    public boolean updateMonsterTypeConfig(Integer id, SystemMonsterTypeConfig body) {
        SystemMonsterTypeConfig existing = systemMonsterTypeConfigRepository.findById(id).orElse(null);
        if (existing == null || body == null) return false;
        PartialUpdate.copyNonNull(body, existing, "id", "updatedAt");
        systemMonsterTypeConfigRepository.save(existing);
        return true;
    }

    public boolean updateMonsterCommonConfig(Integer id, SystemMonsterCommoConfig body) {
        SystemMonsterCommoConfig existing = systemMonsterCommoConfigRepository.findById(id).orElse(null);
        if (existing == null || body == null) return false;
        PartialUpdate.copyNonNull(body, existing, "id", "updatedAt");
        systemMonsterCommoConfigRepository.save(existing);
        return true;
    }

    public Map<String, Object> getGameModeConfigs(int page, int pageSize) {
        return AdminPageSupport.pageMap("items", gameModeConfigRepository.findAll(pageable(page, pageSize, "id")));
    }

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

    public Map<String, Object> getAdaptLogs(int page, int pageSize, Integer userId, String gameMode, String startDate, String endDate) {
        AdminFilter<UserModelAdaptLog> f = adaptLogFilter(userId, gameMode, parseDate(startDate), parseDate(endDate));
        Page<UserModelAdaptLog> logs = userModelAdaptLogRepository.findAll(f, pageable(page, pageSize, "logId"));
        List<Map<String, Object>> items = logs.getContent().stream().map(this::adaptLogView).toList();
        return AdminPageSupport.map("logs", items, "total", logs.getTotalElements());
    }

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

    private AdminFilter<UserModelAdaptLog> adaptLogFilter(Integer userId, String gameMode, LocalDate start, LocalDate end) {
        return AdminFilter.of(UserModelAdaptLog.class)
                .eq("userId", userId)
                .eq("gameMode", gameMode)
                .between("logTime", start == null ? null : start.atStartOfDay(), end == null ? null : end.plusDays(1).atStartOfDay());
    }

    private Map<String, Object> adaptLogView(UserModelAdaptLog l) {
        String mode = l.getGameMode();
        if (mode == null && l.getGameRecord() != null) {
            mode = l.getGameRecord().getGameMode();
        }
        return AdminPageSupport.map(
                "logId", l.getLogId(),
                "userId", l.getUserId(),
                "gameRecordId", l.getGameRecord() != null ? l.getGameRecord().getRecordId() : null,
                "sessionId", l.getSessionId(),
                "gameMode", mode,
                "roundNum", l.getRoundNum(),
                "beforeDifficulty", l.getBeforeDifficulty(),
                "afterDifficulty", l.getAfterDifficulty(),
                "adjustParams", l.getAdjustParams(),
                "triggerRule", l.getTriggerRule(),
                "logTime", l.getLogTime());
    }

    private BigDecimal avgDifficulty(List<UserModelAdaptLog> logs) {
        List<BigDecimal> values = logs.stream().map(UserModelAdaptLog::getAfterDifficulty).filter(Objects::nonNull).toList();
        if (values.isEmpty()) return null;
        return values.stream().reduce(BigDecimal.ZERO, BigDecimal::add).divide(BigDecimal.valueOf(values.size()), 2, RoundingMode.HALF_UP);
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

    private PageRequest pageable(int page, int pageSize, String sortDescField) {
        int p = Math.max(0, page - 1);
        int size = Math.min(200, Math.max(1, pageSize));
        return sortDescField == null ? PageRequest.of(p, size) : PageRequest.of(p, size, Sort.by(Sort.Direction.DESC, sortDescField));
    }
}
