package com.typinggame.game.rule;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.typinggame.game.entity.GameRuleConfig;
import com.typinggame.game.repository.GameRuleConfigRepository;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class RuleConfigService implements RuleConfigProvider {
    public static final String DIFFICULTY = "difficulty";
    public static final String GENETIC = "genetic";

    private static final Logger log = LoggerFactory.getLogger(RuleConfigService.class);

    private final GameRuleConfigRepository repository;
    private final ObjectMapper objectMapper;
    private final Cache<String, ActiveRule> cache = Caffeine.newBuilder().maximumSize(16).build();

    public RuleConfigService(GameRuleConfigRepository repository, ObjectMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    @PostConstruct
    public void preload() {
        activeDifficulty();
        activeGenetic();
    }

    @Override
    public DifficultyRule activeDifficulty() {
        ActiveRule active = load(DIFFICULTY, DifficultyRule.class, new DifficultyRule());
        return (DifficultyRule) active.payload();
    }

    @Override
    public GeneticRule activeGenetic() {
        ActiveRule active = load(GENETIC, GeneticRule.class, new GeneticRule());
        return (GeneticRule) active.payload();
    }

    @Override
    public int difficultyVersion() {
        return load(DIFFICULTY, DifficultyRule.class, new DifficultyRule()).version();
    }

    @Override
    public int geneticVersion() {
        return load(GENETIC, GeneticRule.class, new GeneticRule()).version();
    }

    public Map<String, Object> activeView(String key) {
        ActiveRule active = loadChecked(key);
        Map<String, Object> view = new LinkedHashMap<>();
        view.put("ruleKey", key);
        view.put("version", active.version());
        view.put("status", active.version() == 0 ? "default" : "active");
        view.put("payload", active.payload());
        return view;
    }

    public List<Map<String, Object>> versions(String key) {
        requireKey(key);
        List<Map<String, Object>> rows = new ArrayList<>();
        for (GameRuleConfig config : repository.findByRuleKeyOrderByVersionDesc(key)) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("version", config.getVersion());
            row.put("status", config.getStatus());
            row.put("updateId", config.getUpdateId());
            row.put("updateTime", config.getUpdateTime());
            row.put("payload", read(config.getPayload(), payloadType(key)));
            rows.add(row);
        }
        return rows;
    }

    @Transactional
    public Map<String, Object> saveDraft(String key, Object payload, Integer userId) {
        Object mergedPayload = mergeWithBase(key, payload);
        Object parsed = parseAndValidate(key, mergedPayload);
        int next = repository.findByRuleKeyOrderByVersionDesc(key).stream()
                .map(GameRuleConfig::getVersion)
                .findFirst()
                .orElse(0) + 1;
        GameRuleConfig row = new GameRuleConfig();
        row.setRuleKey(key);
        row.setVersion(next);
        row.setPayload(write(parsed));
        row.setStatus("draft");
        row.setUpdateId(userId);
        row.setUpdateTime(new Date());
        repository.save(row);
        return Map.of("ruleKey", key, "version", next, "status", "draft");
    }

    /**
     * 将请求字段覆盖到当前生效（或最新）规则上，避免前端漏传字段冲掉已有配置。
     */
    @SuppressWarnings("unchecked")
    private Object mergeWithBase(String key, Object payload) {
        requireKey(key);
        Map<String, Object> incoming = objectMapper.convertValue(payload == null ? Map.of() : payload, Map.class);
        Object base = latestPayloadOrDefault(key);
        Map<String, Object> merged = new LinkedHashMap<>(objectMapper.convertValue(base, Map.class));
        for (Map.Entry<String, Object> entry : incoming.entrySet()) {
            if (entry.getValue() != null) {
                merged.put(entry.getKey(), entry.getValue());
            }
        }
        return merged;
    }

    private Object latestPayloadOrDefault(String key) {
        return repository.findFirstByRuleKeyAndStatusOrderByVersionDesc(key, "active")
                .or(() -> repository.findByRuleKeyOrderByVersionDesc(key).stream().findFirst())
                .map(row -> (Object) read(row.getPayload(), payloadType(key)))
                .orElseGet(() -> DIFFICULTY.equals(key) ? (Object) new DifficultyRule() : new GeneticRule());
    }

    @Transactional
    public Map<String, Object> publish(String key, int version, Integer userId) {
        requireKey(key);
        GameRuleConfig row = repository.findByRuleKeyAndVersion(key, version)
                .orElseThrow(() -> new IllegalArgumentException("规则版本不存在"));
        parseAndValidate(key, read(row.getPayload(), payloadType(key)));
        for (GameRuleConfig active : repository.findByRuleKeyAndStatus(key, "active")) {
            if (!active.getId().equals(row.getId())) {
                active.setStatus("draft");
                repository.save(active);
            }
        }
        row.setStatus("active");
        row.setUpdateId(userId);
        row.setUpdateTime(new Date());
        repository.save(row);
        cache.invalidate(key);
        log.info("rule published key={} version={}", key, version);
        return activeView(key);
    }

    public Map<String, Object> simulate(List<?> accuracies, List<?> speeds, int rounds) {
        return simulate(activeDifficulty(), accuracies, speeds, rounds);
    }

    /**
     * 用指定规则做离线仿真：管理端可以先改参数、看曲线，再决定要不要存草稿。
     * 不落库、不碰缓存。
     */
    public Map<String, Object> simulate(DifficultyRule rule, List<?> accuracies, List<?> speeds, int rounds) {
        List<Map<String, Object>> curve = new ArrayList<>();
        for (DifficultyEngine.SimulationPoint point : DifficultyEngine.simulate(rule, toDoubles(accuracies),
                toDoubles(speeds), rounds)) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("round", point.round());
            row.put("accuracy", point.accuracy());
            row.put("difficulty", round4(point.difficulty()));
            row.put("monsterCount", point.monsterCount());
            row.put("normalRatio", point.normalRatio());
            row.put("attackRatio", point.attackRatio());
            row.put("evasiveRatio", point.evasiveRatio());
            row.put("smartRatio", point.smartRatio());
            curve.add(row);
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("version", difficultyVersion());
        result.put("curve", curve);
        return result;
    }

    public void refresh() {
        cache.invalidateAll();
        preload();
    }

    private ActiveRule loadChecked(String key) {
        requireKey(key);
        return key.equals(DIFFICULTY)
                ? load(DIFFICULTY, DifficultyRule.class, new DifficultyRule())
                : load(GENETIC, GeneticRule.class, new GeneticRule());
    }

    private <T> ActiveRule load(String key, Class<T> type, T fallback) {
        return cache.get(key, ignored -> {
            try {
                return repository.findFirstByRuleKeyAndStatusOrderByVersionDesc(key, "active")
                        .map(row -> {
                            T payload = read(row.getPayload(), type);
                            if (payload instanceof DifficultyRule difficulty) RuleValidator.validateDifficulty(difficulty);
                            if (payload instanceof GeneticRule genetic) RuleValidator.validateGenetic(genetic);
                            log.info("rule hit key={} version={} source=db", key, row.getVersion());
                            return new ActiveRule(row.getVersion(), payload);
                        })
                        .orElseGet(() -> {
                            log.info("rule hit key={} version=0 source=default", key);
                            return new ActiveRule(0, fallback);
                        });
            } catch (Exception exception) {
                log.warn("rule fallback key={} reason={}", key, exception.getMessage());
                return new ActiveRule(0, fallback);
            }
        });
    }

    private Object parseAndValidate(String key, Object payload) {
        requireKey(key);
        if (DIFFICULTY.equals(key)) {
            DifficultyRule rule = objectMapper.convertValue(payload, DifficultyRule.class);
            RuleValidator.validateDifficulty(rule);
            return rule;
        }
        GeneticRule rule = objectMapper.convertValue(payload, GeneticRule.class);
        RuleValidator.validateGenetic(rule);
        return rule;
    }

    private Class<?> payloadType(String key) {
        return DIFFICULTY.equals(key) ? DifficultyRule.class : GeneticRule.class;
    }

    private void requireKey(String key) {
        if (!DIFFICULTY.equals(key) && !GENETIC.equals(key)) {
            throw new IllegalArgumentException("未知规则: " + key);
        }
    }

    private <T> T read(String json, Class<T> type) {
        try {
            return objectMapper.readValue(json, type);
        } catch (Exception exception) {
            throw new IllegalArgumentException("规则 JSON 无法解析", exception);
        }
    }

    private String write(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception exception) {
            throw new IllegalArgumentException("规则 JSON 无法保存", exception);
        }
    }

    private static List<Double> toDoubles(List<?> values) {
        if (values == null || values.isEmpty()) {
            return List.of();
        }
        List<Double> numbers = new ArrayList<>(values.size());
        for (Object value : values) {
            if (value instanceof Number number) {
                numbers.add(number.doubleValue());
            }
        }
        return numbers;
    }

    private static double round4(double value) {
        return Math.round(value * 10000d) / 10000d;
    }

    private record ActiveRule(int version, Object payload) {
    }
}
