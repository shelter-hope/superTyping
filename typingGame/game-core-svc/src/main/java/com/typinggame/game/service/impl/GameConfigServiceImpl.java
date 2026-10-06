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
import com.typinggame.entity.config.SystemMonsterTypeConfig;
import com.typinggame.entity.user.CharacterStats;
import com.typinggame.enums.GameMode;
import com.typinggame.game.repository.GameCharTypeConfigRepository;
import com.typinggame.game.repository.GameModeConfigRepository;
import com.typinggame.game.repository.SystemMonsterTypeConfigRepository;
import com.typinggame.game.service.CharacterStatsService;
import com.typinggame.game.service.DifficultyModelService;
import com.typinggame.game.service.GameConfigService;
import com.typinggame.game.service.StatsService;
import com.typinggame.game.service.word.WordSourceContext;
import com.typinggame.game.service.word.WordSourceStrategy;
import com.typinggame.entity.game.GameRound;
import com.typinggame.entity.game.GameSession;
import com.typinggame.game.repository.GameRoundRepository;
import com.typinggame.game.repository.GameSessionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class GameConfigServiceImpl implements GameConfigService {

    private static final AtomicLong PLAN_SEQUENCE = new AtomicLong(0);

    private static final String DEFAULT_CHAR_TYPES = "lower,upper,number,punct";
    private static final Set<String> ALLOWED_CHAR_TYPES = Set.of("lower", "upper", "number", "punct", "operator", "chinese", "special");

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

    public GameConfigServiceImpl(StatsService statsService,
                                 CharacterStatsService characterStatsService,
                                 DifficultyModelService difficultyModelService,
                                 GameModeConfigRepository gameModeConfigRepository,
                                 GameCharTypeConfigRepository charTypeConfigRepository,
                                 SystemMonsterTypeConfigRepository monsterTypeConfigRepository,
                                 List<WordSourceStrategy> wordSourceStrategies,
                                 GameSessionRepository gameSessionRepository,
                                 GameRoundRepository gameRoundRepository,
                                 ObjectMapper objectMapper) {
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
        GameAdjustParam adjustParam = difficultyModelService.calculate(userId, mode, 1, suggestion.getRecentAccuracy(), suggestion.getRecentSpeed(), focusChars.size());
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
        TrainingSuggestionDTO suggestion = statsService.getTrainingSuggestions(userId);
        List<CharacterStats> weakChars = characterStatsService.getWeakChars(userId, 10);
        List<String> focusChars = weakChars.stream()
                .map(CharacterStats::getCharValue)
                .filter(s -> s != null && !s.isBlank())
                .collect(Collectors.toList());
        GameAdjustParam adjustParam = difficultyModelService.calculate(userId, mode, roundNum, suggestion.getRecentAccuracy(), suggestion.getRecentSpeed(), focusChars.size());
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
        NextRoundResponseDTO response = NextRoundResponseDTO.builder()
                .planVersion(planVersion)
                .targetAccuracy(BigDecimal.valueOf(0.85))
                .adjustParam(adjustParam)
                .monsters(buildMonsters(words, adjustParam, Math.max(1, roundNum)))
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
            round.setFormulaVersion("v1");
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
            monsters.add(monster);
        }
        return monsters;
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
