package com.typinggame.game.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.typinggame.dto.game.GameAdjustParam;
import com.typinggame.dto.game.GameCharTypeConfigDTO;
import com.typinggame.dto.game.GameCharTypeConfigUpdateDTO;
import com.typinggame.dto.game.GameRecommendDTO;
import com.typinggame.dto.game.MonsterDataDTO;
import com.typinggame.dto.game.NextRoundResponseDTO;
import com.typinggame.dto.game.GameSessionCreateDTO;
import com.typinggame.dto.game.GameSessionDTO;
import com.typinggame.dto.stats.TrainingSuggestionDTO;
import com.typinggame.entity.config.GameCharTypeConfig;
import com.typinggame.entity.config.SystemGameModeConfig;
import com.typinggame.game.entity.SystemMonsterTypeConfig;
import com.typinggame.game.entity.MonsterAttempt;
import com.typinggame.game.entity.CharacterStats;
import com.typinggame.enums.GameMode;
import com.typinggame.game.repository.GameCharTypeConfigRepository;
import com.typinggame.game.repository.GameModeConfigRepository;
import com.typinggame.game.repository.SystemMonsterTypeConfigRepository;
import com.typinggame.game.service.CharacterStatsService;
import com.typinggame.game.service.DifficultyModelService;
import com.typinggame.game.service.GameConfigService;
import com.typinggame.game.service.KeyConfusionService;
import com.typinggame.game.service.StatsService;
import com.typinggame.game.rule.GeneticEngine;
import com.typinggame.game.rule.GeneticRule;
import com.typinggame.game.rule.RuleConfigProvider;
import com.typinggame.game.service.word.WordSourceContext;
import com.typinggame.game.service.word.WordSourceStrategy;
import com.typinggame.game.entity.GameRound;
import com.typinggame.game.entity.GameSession;
import com.typinggame.game.repository.GameRoundRepository;
import com.typinggame.game.repository.GameSessionRepository;
import com.typinggame.game.repository.MonsterAttemptRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicLong;
import java.util.Random;

@Service
public class GameConfigServiceImpl implements GameConfigService {

    private static final AtomicLong PLAN_SEQUENCE = new AtomicLong(0);

    private static final String DEFAULT_CHAR_TYPES = "lower,upper,number,punct";
    private static final Set<String> ALLOWED_CHAR_TYPES = Set.of("lower", "upper", "number", "punct", "operator", "chinese", "special");
    /** 定向变异：最多取多少个易错字符 */
    private static final int CONFUSION_EXPECTED_LIMIT = 20;
    /** 定向变异：每个易错字符最多带几个常打错的替代字符 */
    private static final int CONFUSION_TYPED_LIMIT = 3;

    private final StatsService statsService;
    private final CharacterStatsService characterStatsService;
    private final DifficultyModelService difficultyModelService;
    private final GameModeConfigRepository gameModeConfigRepository;
    private final GameCharTypeConfigRepository charTypeConfigRepository;
    private final SystemMonsterTypeConfigRepository monsterTypeConfigRepository;
    private final List<WordSourceStrategy> wordSourceStrategies;
    private final GameSessionRepository gameSessionRepository;
    private final GameRoundRepository gameRoundRepository;
    private final ObjectMapper objectMapper;
    private final MonsterAttemptRepository monsterAttemptRepository;
    private final RuleConfigProvider ruleConfigProvider;
    private final KeyConfusionService keyConfusionService;

    public GameConfigServiceImpl(StatsService statsService,
                                 CharacterStatsService characterStatsService,
                                 DifficultyModelService difficultyModelService,
                                 GameModeConfigRepository gameModeConfigRepository,
                                 GameCharTypeConfigRepository charTypeConfigRepository,
                                 SystemMonsterTypeConfigRepository monsterTypeConfigRepository,
                                 List<WordSourceStrategy> wordSourceStrategies,
                                 GameSessionRepository gameSessionRepository,
                                 GameRoundRepository gameRoundRepository,
                                 ObjectMapper objectMapper,
                                 MonsterAttemptRepository monsterAttemptRepository,
                                 RuleConfigProvider ruleConfigProvider,
                                 KeyConfusionService keyConfusionService) {
        this.statsService = statsService;
        this.characterStatsService = characterStatsService;
        this.difficultyModelService = difficultyModelService;
        this.gameModeConfigRepository = gameModeConfigRepository;
        this.charTypeConfigRepository = charTypeConfigRepository;
        this.monsterTypeConfigRepository = monsterTypeConfigRepository;
        this.wordSourceStrategies = wordSourceStrategies;
        this.gameSessionRepository = gameSessionRepository;
        this.gameRoundRepository = gameRoundRepository;
        this.objectMapper = objectMapper;
        this.monsterAttemptRepository = monsterAttemptRepository;
        this.ruleConfigProvider = ruleConfigProvider;
        this.keyConfusionService = keyConfusionService;
    }

    @Override
    public GameCharTypeConfigDTO getCharTypes(Integer userId, String gameMode) {
        GameMode mode = GameMode.fromClientMode(gameMode);
        String charType = resolveCharTypeConfig(userId, mode).getCharType();
        return GameCharTypeConfigDTO.builder()
                .gameMode(mode.toClientMode())
                .charType(charType)
                .charTypes(parseCharTypes(charType))
                .build();
    }

    @Override
    @Transactional
    public boolean updateCharTypes(Integer userId, GameCharTypeConfigUpdateDTO request) {
        GameMode mode = GameMode.fromClientMode(request == null ? null : request.getGameMode());
        String normalized = normalizeCharTypes(request);
        GameCharTypeConfig config = resolveCharTypeConfig(userId, mode);
        config.setCharType(normalized);
        config.setUpdateTime(new Date());
        charTypeConfigRepository.save(config);
        return true;
    }

    @Override
    public GameRecommendDTO getRecommend(Integer userId, String gameMode) {
        GameMode mode = GameMode.fromClientMode(gameMode);
        TrainingSuggestionDTO suggestion = statsService.getTrainingSuggestions(userId);
        List<String> focusChars = extractFocusChars(suggestion);
        GameAdjustParam adjustParam = difficultyModelService.project(userId, mode, 1, null);
        List<String> words = selectWordSource(mode).generateWords(new WordSourceContext(
                userId, mode, 8, 3, focusChars, parseCharTypes(resolveCharTypeConfig(userId, mode).getCharType()), null, null));

        return GameRecommendDTO.builder()
                .focusChars(focusChars)
                .suggestedBaseSpeed(adjustParam.getMonsterSpeed().doubleValue())
                .suggestedMode(mode.toClientMode())
                .previewWords(words)
                .summary(suggestion.getSummary())
                .suggestedMonsterCount(adjustParam.getMonsterCount())
                .suggestedAvgStringLength(adjustParam.getAvgStringLength())
                .suggestedNormalRatio(adjustParam.getNormalRatio())
                .suggestedAttackRatio(adjustParam.getAttackRatio())
                .suggestedEvasiveRatio(adjustParam.getEvasiveRatio())
                .suggestedSmartRatio(adjustParam.getSmartRatio())
                .currentDifficulty(adjustParam.getCurrentDifficulty())
                .build();
    }

    @Override
    public NextRoundResponseDTO getNextRound(Integer userId, int roundNum, String gameMode, List<Integer> customDictIds, List<Integer> systemDictIds) {
        return getNextRound(userId, null, roundNum, gameMode, customDictIds, systemDictIds);
    }

    @Override
    @Transactional
    public NextRoundResponseDTO getNextRound(Integer userId, Long sessionId, int roundNum, String gameMode,
                                             List<Integer> customDictIds, List<Integer> systemDictIds) {
        GameMode mode = GameMode.fromClientMode(gameMode);
        if (sessionId != null) {
            gameSessionRepository.findBySessionIdAndUserId(sessionId, userId)
                    .orElseThrow(() -> new IllegalArgumentException("游戏会话不存在或无权访问"));
        }
        List<CharacterStats> weakChars = characterStatsService.getWeakChars(userId, 10);
        List<String> focusChars = weakChars.stream()
                .map(CharacterStats::getCharValue)
                .filter(s -> s != null && !s.isBlank())
                .collect(Collectors.toList());
        Double settledDifficulty = null;
        if (sessionId != null && roundNum > 1) {
            GameRound previous = gameRoundRepository.findBySessionIdAndRoundNum(sessionId, roundNum - 1).orElse(null);
            if (previous != null && previous.getDifficultyAfter() != null) {
                settledDifficulty = previous.getDifficultyAfter().doubleValue();
            }
        }
        GameAdjustParam adjustParam = difficultyModelService.project(userId, mode, roundNum, settledDifficulty);
        List<String> words = selectWordSource(mode).generateWords(new WordSourceContext(
                userId,
                mode,
                adjustParam.getMonsterCount(),
                adjustParam.getAvgStringLength(),
                focusChars,
                parseCharTypes(resolveCharTypeConfig(userId, mode).getCharType()),
                customDictIds,
                systemDictIds));
        long planVersion = PLAN_SEQUENCE.incrementAndGet();
        List<MonsterDataDTO> monsters = buildMonsters(words, adjustParam, Math.max(1, roundNum));
        if (sessionId != null && mode == GameMode.ENDLESS_ADAPT && roundNum > 1) {
            List<MonsterAttempt> parents = monsterAttemptRepository
                    .findBySessionIdAndResultOrderByFitnessDesc(sessionId, "KILLED");
            if (!parents.isEmpty()) {
                Map<String, List<String>> confusion = keyConfusionService
                        .getConfusionMap(userId, CONFUSION_EXPECTED_LIMIT, CONFUSION_TYPED_LIMIT);
                monsters = buildGeneticMonsters(monsters, parents, focusChars, confusion, roundNum);
            }
        }
        NextRoundResponseDTO response = NextRoundResponseDTO.builder()
                .planVersion(planVersion)
                .targetAccuracy(BigDecimal.valueOf(ruleConfigProvider.activeDifficulty().getTargetAccuracy()))
                .adjustParam(adjustParam)
                .monsters(monsters)
                .build();
        if (sessionId != null) {
            GameRound round = gameRoundRepository.findBySessionIdAndRoundNum(sessionId, roundNum).orElseGet(GameRound::new);
            if (round.getRoundId() != null && round.getPlanPayload() != null && !round.getPlanPayload().isBlank()) {
                try {
                    response.setPlanVersion(round.getPlanVersion());
                    response.setMonsters(objectMapper.readValue(round.getPlanPayload(), new TypeReference<List<MonsterDataDTO>>() {}));
                    return response;
                } catch (JsonProcessingException ignored) {
                    // Invalid historical payload is regenerated and replaced below.
                }
            }
            round.setSessionId(sessionId);
            round.setRoundNum(roundNum);
            round.setPlanVersion(planVersion);
            round.setFormulaVersion("difficulty-v" + ruleConfigProvider.difficultyVersion());
            round.setDifficultyBefore(adjustParam.getCurrentDifficulty());
            round.setTargetAccuracy(response.getTargetAccuracy());
            round.setMonsterSpeed(adjustParam.getMonsterSpeed());
            round.setMonsterCount(adjustParam.getMonsterCount());
            round.setStringLength(adjustParam.getAvgStringLength());
            round.setNormalRatio(adjustParam.getNormalRatio());
            round.setAttackerRatio(adjustParam.getAttackRatio());
            round.setEvasiveRatio(adjustParam.getEvasiveRatio());
            round.setSmartRatio(adjustParam.getSmartRatio());
            try {
                round.setPlanPayload(objectMapper.writeValueAsString(response.getMonsters()));
            } catch (JsonProcessingException e) {
                throw new IllegalStateException("无法保存游戏轮次计划", e);
            }
            round.setStatus("CREATED");
            round.setCreatedAt(new Date());
            gameRoundRepository.save(round);
        }
        return response;
    }

    @Override
    @Transactional
    public GameSessionDTO createSession(Integer userId, GameSessionCreateDTO request) {
        GameMode mode = GameMode.fromClientMode(request == null ? null : request.getGameMode());
        Date now = new Date();
        GameSession session = new GameSession();
        session.setUserId(userId);
        session.setGameMode(mode.name());
        session.setStatus("RUNNING");
        session.setCurrentRound(1);
        session.setStartTime(now);
        session.setCreatedAt(now);
        session.setUpdatedAt(now);
        session = gameSessionRepository.save(session);
        return GameSessionDTO.builder().sessionId(session.getSessionId()).gameMode(mode.toClientMode())
                .status(session.getStatus()).currentRound(session.getCurrentRound()).build();
    }

    private GameCharTypeConfig resolveCharTypeConfig(Integer userId, GameMode mode) {
        SystemGameModeConfig modeConfig = gameModeConfigRepository.findByModeName(mode.name()).orElse(null);
        Integer gameModeId = modeConfig == null ? null : modeConfig.getId();
        if (gameModeId != null) {
            return charTypeConfigRepository.findByUserIdAndGameModeId(userId, gameModeId)
                    .orElseGet(() -> createDefaultCharTypeConfig(userId, gameModeId));
        }
        GameCharTypeConfig fallback = new GameCharTypeConfig();
        fallback.setUserId(userId);
        fallback.setCharType(DEFAULT_CHAR_TYPES);
        return fallback;
    }

    private GameCharTypeConfig createDefaultCharTypeConfig(Integer userId, Integer gameModeId) {
        Date now = new Date();
        GameCharTypeConfig config = new GameCharTypeConfig();
        config.setUserId(userId);
        config.setGameModeId(gameModeId);
        config.setCharType(DEFAULT_CHAR_TYPES);
        config.setCreateTime(now);
        config.setUpdateTime(now);
        return charTypeConfigRepository.save(config);
    }

    private String normalizeCharTypes(GameCharTypeConfigUpdateDTO request) {
        List<String> source = request == null ? Collections.emptyList() : request.getCharTypes();
        if (source == null || source.isEmpty()) {
            source = parseCharTypes(request == null ? null : request.getCharType());
        }
        LinkedHashSet<String> normalized = new LinkedHashSet<>();
        for (String item : source) {
            if (item == null) continue;
            String type = item.trim().toLowerCase();
            if (!ALLOWED_CHAR_TYPES.contains(type)) {
                throw new IllegalArgumentException("不支持的字符类型: " + item);
            }
            normalized.add(type);
        }
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException("至少选择一种字符类型");
        }
        return String.join(",", normalized);
    }

    private List<String> parseCharTypes(String value) {
        if (value == null || value.isBlank()) return Collections.emptyList();
        LinkedHashSet<String> set = new LinkedHashSet<>();
        for (String item : value.split(",")) {
            String trimmed = item.trim().toLowerCase();
            if (!trimmed.isEmpty()) set.add(trimmed);
        }
        return new ArrayList<>(set);
    }

    private List<String> extractFocusChars(TrainingSuggestionDTO suggestion) {
        if (suggestion == null || suggestion.getFocusChars() == null) return Collections.emptyList();
        return suggestion.getFocusChars().stream()
                .map(TrainingSuggestionDTO.CharSuggestionItem::getCharacter)
                .filter(s -> s != null && !s.isBlank())
                .limit(10)
                .collect(Collectors.toList());
    }

    private WordSourceStrategy selectWordSource(GameMode mode) {
        return wordSourceStrategies.stream()
                .filter(strategy -> strategy.supports(mode))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("未找到游戏模式词源策略: " + mode));
    }

    private List<MonsterDataDTO> buildMonsters(List<String> words, GameAdjustParam adjustParam, int roundNum) {
        Map<String, SystemMonsterTypeConfig> configs = monsterTypeConfigRepository.findByIsActiveTrue().stream()
                .collect(Collectors.toMap(SystemMonsterTypeConfig::getType, c -> c, (a, b) -> a, LinkedHashMap::new));
        List<MonsterDataDTO> monsters = new ArrayList<>();
        for (int i = 0; i < words.size(); i++) {
            String type = pickMonsterType(i, adjustParam);
            SystemMonsterTypeConfig typeConfig = configs.get(type);
            MonsterDataDTO monster = new MonsterDataDTO();
            monster.setMonsterType(type);
            monster.setTargetStr(words.get(i));
            monster.setSpeed(adjustParam.getMonsterSpeed());
            monster.setSurvivalTime(0);
            monster.setDestroyCostTime(0);
            monster.setTotalCount(words.get(i).length());
            monster.setInputErrorCount(0);
            monster.setIsTrueMean(1);
            monster.setScore(resolveScore(typeConfig, roundNum));
            monster.setDifficultyLevel(roundNum);
            monster.setSpawnWave(roundNum);
            monster.setIsParent(false);
            monster.setMonsterKey("round-" + roundNum + "-monster-" + i);
            monsters.add(monster);
        }
        return monsters;
    }

    private List<MonsterDataDTO> buildGeneticMonsters(List<MonsterDataDTO> baseMonsters,
                                                       List<MonsterAttempt> attempts,
                                                       List<String> focusChars,
                                                       Map<String, List<String>> confusion,
                                                       int roundNum) {
        GeneticRule genetic = ruleConfigProvider.activeGenetic();
        List<MonsterAttempt> ranked = attempts.stream()
                .filter(attempt -> attempt.getTargetStr() != null && !attempt.getTargetStr().isBlank())
                .sorted(Comparator.comparingDouble((MonsterAttempt attempt) ->
                        attempt.getFitness() == null ? 0d : attempt.getFitness().doubleValue()).reversed())
                .collect(Collectors.toList());
        List<MonsterAttempt> parents = GeneticEngine.limitParents(ranked, genetic.getInRoundParentLimit());
        if (parents.isEmpty()) {
            return baseMonsters;
        }
        Random random = new Random(Objects.hash("in-round-genetic", roundNum,
                parents.stream().map(MonsterAttempt::getAttemptId).toList()));
        for (int i = 0; i < baseMonsters.size(); i++) {
            MonsterDataDTO child = baseMonsters.get(i);
            MonsterAttempt parentA = GeneticEngine.tournamentPick(parents,
                    attempt -> attempt.getFitness() == null ? 0d : attempt.getFitness().doubleValue(),
                    genetic.getTournamentSize(), random);
            MonsterAttempt parentB = parents.size() > 1
                    ? GeneticEngine.tournamentPick(parents,
                    attempt -> attempt.getFitness() == null ? 0d : attempt.getFitness().doubleValue(),
                    genetic.getTournamentSize(), random)
                    : null;
            if (parentA == null) {
                continue;
            }
            if (parentB != null && Objects.equals(parentA.getAttemptId(), parentB.getAttemptId())) {
                parentB = parents.get((parents.indexOf(parentA) + 1) % parents.size());
            }
            int targetLength = Math.max(1, child.getTargetStr() == null ? 1 : child.getTargetStr().length());
            String parentBText = parentB == null ? parentA.getTargetStr() : parentB.getTargetStr();
            String crossed = GeneticEngine.crossover(parentA.getTargetStr(), parentBText, targetLength,
                    genetic.getCrossover(), random);
            int crossoverPoint = Math.min(targetLength, parentA.getTargetStr().length());
            GeneticEngine.Mutation mutation = GeneticEngine.mutate(crossed, focusChars, confusion,
                    genetic.getInRoundMutationRate(), random);
            String text = mutation.text();
            BigDecimal mutationRate = BigDecimal.valueOf(genetic.getInRoundMutationRate());
            List<Integer> mutationPositions = new ArrayList<>(mutation.positions());
            List<String> mutationChars = new ArrayList<>(mutation.chars());
            child.setTargetStr(text);
            child.setTotalCount(text.length());
            child.setMonsterKey("round-" + roundNum + "-monster-" + i);
            child.setGeneration(Math.max(parentA.getGeneration() == null ? 0 : parentA.getGeneration(),
                    parentB == null || parentB.getGeneration() == null ? 0 : parentB.getGeneration()) + 1);
            child.setParentAttemptAId(parentA.getAttemptId());
            child.setParentAttemptBId(parentB == null ? null : parentB.getAttemptId());
            child.setCrossoverPoint(crossoverPoint);
            child.setMutationRate(mutationRate);
            child.setMutationPositions(mutationPositions.stream().map(String::valueOf).collect(Collectors.joining(",")));
            child.setMutationChars(String.join("", mutationChars));
        }
        return baseMonsters;
    }

    private String crossover(String parentA, String parentB, int crossoverPoint, int targetLength) {
        String prefix = parentA.substring(0, Math.min(crossoverPoint, parentA.length()));
        String suffix = parentB.substring(Math.min(crossoverPoint, parentB.length()));
        String child = prefix + suffix;
        if (child.length() < targetLength) {
            String source = parentA + parentB;
            int sourceIndex = 0;
            while (child.length() < targetLength) {
                child += source.charAt(sourceIndex++ % source.length());
            }
        }
        return child.substring(0, Math.min(targetLength, child.length()));
    }

    private String chooseMutationChar(String text, int position, List<String> focusChars) {
        if (focusChars != null && !focusChars.isEmpty()) {
            return focusChars.get(ThreadLocalRandom.current().nextInt(focusChars.size()));
        }
        return String.valueOf(text.charAt((position + 1) % text.length()));
    }

    private String pickMonsterType(int index, GameAdjustParam adjustParam) {
        int bucket = ThreadLocalRandom.current().nextInt(100);
        if (bucket < adjustParam.getNormalRatio()) return "NORMAL";
        if (bucket < adjustParam.getNormalRatio() + adjustParam.getAttackRatio()) return "ATTACKER";
        if (bucket < adjustParam.getNormalRatio() + adjustParam.getAttackRatio() + adjustParam.getEvasiveRatio()) return "EVASIVE";
        return "SMART";
    }

    private Integer resolveScore(SystemMonsterTypeConfig typeConfig, int roundNum) {
        Integer baseScore = typeConfig == null || typeConfig.getScore() == null ? 10 : typeConfig.getScore();
        return baseScore + Math.max(0, roundNum - 1) * 2;
    }
}
