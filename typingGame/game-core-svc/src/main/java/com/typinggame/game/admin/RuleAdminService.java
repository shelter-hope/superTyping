package com.typinggame.game.admin;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.typinggame.common.admin.AdminPageSupport;
import com.typinggame.common.exception.BusinessException;
import com.typinggame.entity.config.ConfigTemplate;
import com.typinggame.entity.config.SystemParam;
import com.typinggame.game.repository.ConfigTemplateRepository;
import com.typinggame.game.repository.SystemParamRepository;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class RuleAdminService {
    static final Map<String, String> RULE_PARAMS = Map.of(
            "character-rules", "admin.character_rules",
            "movement-rules", "admin.movement_rules",
            "difficulty-params", "admin.difficulty_params");

    private static final Map<String, Map<String, Object>> RULE_DEFAULTS = Map.of(
            "character-rules", Map.of("charTypes", List.of("lower", "upper", "number", "punct")),
            "movement-rules", Map.of("minSpeed", 0.8, "maxSpeed", 3.0),
            "difficulty-params", Map.of("minDifficulty", 0.0, "maxDifficulty", 1.0));

    private final SystemParamRepository systemParamRepository;
    private final ConfigTemplateRepository configTemplateRepository;
    private final ObjectMapper objectMapper;

    public RuleAdminService(SystemParamRepository systemParamRepository,
                            ConfigTemplateRepository configTemplateRepository,
                            ObjectMapper objectMapper) {
        this.systemParamRepository = systemParamRepository;
        this.configTemplateRepository = configTemplateRepository;
        this.objectMapper = objectMapper;
    }

    public Map<String, Object> getRules(String ruleKey) {
        String paramName = ruleParam(ruleKey);
        return systemParamRepository.findByParamName(paramName)
                .map(p -> readJsonMap(p.getParamValue()))
                .orElseGet(() -> new LinkedHashMap<>(RULE_DEFAULTS.get(ruleKey)));
    }

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

    public List<Map<String, Object>> getConfigTemplates() {
        return configTemplateRepository.findAll(Sort.by(Sort.Direction.DESC, "id")).stream()
                .map(t -> AdminPageSupport.map("id", t.getId(), "name", t.getName(),
                        "createdBy", t.getCreatedBy(), "createTime", t.getCreateTime(), "updateTime", t.getUpdateTime()))
                .toList();
    }

    public Map<String, Object> getConfigTemplate(Integer id) {
        ConfigTemplate t = configTemplateRepository.findById(id)
                .orElseThrow(() -> BusinessException.notFound("配置模板不存在"));
        return templateView(t);
    }

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

    @Transactional
    public void deleteConfigTemplate(Integer id) {
        if (!configTemplateRepository.existsById(id)) throw BusinessException.notFound("配置模板不存在");
        configTemplateRepository.deleteById(id);
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

    private static BigDecimal toDecimal(Object value, String field) {
        if (value == null) return null;
        try {
            return new BigDecimal(value.toString().trim());
        } catch (NumberFormatException e) {
            throw BusinessException.badRequest(field + " 必须是数字");
        }
    }
}
