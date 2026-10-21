package com.typinggame.game.rule;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.typinggame.entity.config.GameRuleConfig;
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
        Object parsed = parseAndValidate(key, payload);
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
        DifficultyRule rule = activeDifficulty();
        int count = Math.max(1, rounds);
        double previous = rule.getInitialDifficulty();
        List<Map<String, Object>> curve = new ArrayList<>();
        for (int round = 1; round <= count; round++) {
            Double accuracy = valueAt(accuracies, round - 1, rule.getTargetAccuracy());
            Double speed = valueAt(speeds, round - 1, 0.0);
            previous = DifficultyEngine.nextDifficulty(rule, previous, accuracy, speed, round, 0);
            DifficultyEngine.Outcome outcome = DifficultyEngine.outcome(rule, previous, 0.8, 3.0, 1, 15);
            Map<String, Object> point = new LinkedHashMap<>();
            point.put("round", round);
            point.put("accuracy", accuracy);
            point.put("difficulty", round4(previous));
            point.put("monsterCount", DifficultyEngine.monsterCount(rule, previous, round));
            point.put("normalRatio", outcome.normalRatio());
            point.put("attackRatio", outcome.attackRatio());
            point.put("evasiveRatio", outcome.evasiveRatio());
            point.put("smartRatio", outcome.smartRatio());
            curve.add(point);
        }
        return Map.of("version", difficultyVersion(), "curve", curve);
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

    private static Double valueAt(List<?> values, int index, double fallback) {
        if (values == null || values.isEmpty()) return fallback;
        Object value = values.get(Math.min(index, values.size() - 1));
        return value instanceof Number number ? number.doubleValue() : fallback;
    }

    private static double round4(double value) {
        return Math.round(value * 10000d) / 10000d;
    }

    private record ActiveRule(int version, Object payload) {
    }
}
