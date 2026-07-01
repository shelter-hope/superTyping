package com.typinggame.controller.game;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.typinggame.dto.game.GameAdjustParam;
import com.typinggame.dto.game.GameCharTypeConfigDTO;
import com.typinggame.dto.game.GameCharTypeConfigUpdateDTO;
import com.typinggame.dto.game.GamePlayerPerformanceDTO;
import com.typinggame.dto.game.GameRecommendDTO;
import com.typinggame.dto.game.MonsterDataDTO;
import com.typinggame.dto.game.NextRoundResponseDTO;
import com.typinggame.dto.stats.TrainingSuggestionDTO;
import com.typinggame.entity.config.GameCharTypeConfig;
import com.typinggame.entity.config.SystemGameModeConfig;
import com.typinggame.entity.game.GameRecord;
import com.typinggame.entity.game.UserGameModeConfig;
import com.typinggame.entity.model.ModelConstant;
import com.typinggame.entity.model.UserModelParams;
import com.typinggame.enums.GameMode;
import com.typinggame.repository.GameCharTypeConfigRepository;
import com.typinggame.repository.GameModeConfigRepository;
import com.typinggame.repository.UserGameModeConfigRepository;
import com.typinggame.service.DifficultyModelService;
import com.typinggame.service.EvolutionService;
import com.typinggame.service.MathModelService;
import com.typinggame.service.StatsService;
import com.typinggame.service.WordSourceService;
import com.typinggame.util.GameCoreUtil;
import com.typinggame.vo.ResultMessage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@Tag(name = "游戏配置 API")
@RequestMapping("/api/v1/game/config")
public class GameConfigController {

    private static final String DEFAULT_CHAR_TYPES = "lower,upper,number,punct,operator,chinese,special";

    @Autowired
    private EvolutionService evolutionService;
    @Autowired
    private StatsService statsService;
    @Autowired
    private DifficultyModelService difficultyModelService;
    @Autowired
    private MathModelService mathModelService;
    @Autowired
    private WordSourceService wordSourceService;
    @Autowired
    private GameModeConfigRepository gameModeConfigRepository;
    @Autowired
    private GameCharTypeConfigRepository gameCharTypeConfigRepository;
    @Autowired
    private UserGameModeConfigRepository userGameModeConfigRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @GetMapping("/char-types")
    @Operation(summary = "获取当前用户在某模式下的字符类型配置")
    public ResponseEntity<ResultMessage<GameCharTypeConfigDTO>> getCharTypes(
            @RequestParam(value = "gameMode", required = false, defaultValue = "endless") String gameMode) {
        Integer userId = getCurrentUserId();
        GameMode modeEnum = GameMode.fromClientMode(gameMode);
        Optional<SystemGameModeConfig> sysOpt = gameModeConfigRepository.findByModeName(modeEnum.name());
        if (!sysOpt.isPresent()) {
            GameCharTypeConfigDTO dto = GameCharTypeConfigDTO.builder()
                    .gameMode(modeEnum.toClientMode())
                    .charType(DEFAULT_CHAR_TYPES)
                    .charTypes(parseCharTypes(DEFAULT_CHAR_TYPES))
                    .build();
            return ResponseEntity.ok(ResultMessage.success(dto));
        }
        Integer gameModeId = sysOpt.get().getId();
        GameCharTypeConfig cfg = gameCharTypeConfigRepository
                .findByUserIdAndGameModeId(userId, gameModeId)
                .orElse(null);
        String charType = (cfg != null && cfg.getCharType() != null && !cfg.getCharType().trim().isEmpty())
                ? cfg.getCharType()
                : DEFAULT_CHAR_TYPES;
        GameCharTypeConfigDTO dto = GameCharTypeConfigDTO.builder()
                .gameMode(modeEnum.toClientMode())
                .charType(normalizeCharTypeString(charType))
                .charTypes(parseCharTypes(charType))
                .build();
        return ResponseEntity.ok(ResultMessage.success(dto));
    }

    @PutMapping("/char-types")
    @Operation(summary = "更新当前用户在某模式下的字符类型配置")
    public ResponseEntity<ResultMessage<Boolean>> updateCharTypes(@RequestBody GameCharTypeConfigUpdateDTO req) {
        Integer userId = getCurrentUserId();
        String gameMode = (req != null && req.getGameMode() != null) ? req.getGameMode() : "endless";
        GameMode modeEnum = GameMode.fromClientMode(gameMode);
        Optional<SystemGameModeConfig> sysOpt = gameModeConfigRepository.findByModeName(modeEnum.name());
        if (!sysOpt.isPresent()) {
            return ResponseEntity.ok(ResultMessage.error(400, "unknown gameMode"));
        }
        Integer gameModeId = sysOpt.get().getId();

        String newCharType;
        if (req != null && req.getCharTypes() != null && !req.getCharTypes().isEmpty()) {
            newCharType = normalizeCharTypeString(String.join(",", req.getCharTypes()));
        } else if (req != null && req.getCharType() != null) {
            newCharType = normalizeCharTypeString(req.getCharType());
        } else {
            newCharType = DEFAULT_CHAR_TYPES;
        }
        if (parseCharTypes(newCharType).isEmpty()) {
            return ResponseEntity.ok(ResultMessage.error(400, "charTypes cannot be empty"));
        }

        Date now = new Date();
        GameCharTypeConfig cfg = gameCharTypeConfigRepository
                .findByUserIdAndGameModeId(userId, gameModeId)
                .orElseGet(() -> {
                    GameCharTypeConfig c = new GameCharTypeConfig();
                    c.setUserId(userId);
                    c.setGameModeId(gameModeId);
                    c.setCreateTime(now);
                    return c;
                });
        cfg.setCharType(newCharType);
        cfg.setUpdateTime(now);
        gameCharTypeConfigRepository.save(cfg);
        return ResponseEntity.ok(ResultMessage.success(true));
    }

    @GetMapping("/recommend")
    @Operation(summary = "获取推荐配置")
    public ResponseEntity<ResultMessage<GameRecommendDTO>> getRecommend(
            @RequestParam(value = "gameMode", required = false) String gameMode) {
        Integer userId = getCurrentUserId();
        String mode = (gameMode != null && !gameMode.isEmpty()) ? GameMode.fromClientMode(gameMode).name() : GameMode.ENDLESS_ADAPT.name();
        List<String> focusChars = java.util.Collections.emptyList();
        List<String> previewWords = evolutionService.generateNextRoundWords(userId, 1, 8, gameMode);
        String summary = "根据你的易错字符生成推荐练习内容";
        Double suggestedBaseSpeed = 1.0;
        Integer suggestedMonsterCount = null;
        Integer suggestedAvgStringLength = null;
        Integer suggestedNormalRatio = null;
        Integer suggestedAttackRatio = null;
        Integer suggestedEvasiveRatio = null;
        Integer suggestedSmartRatio = null;
        BigDecimal currentDifficulty = null;
        try {
            TrainingSuggestionDTO suggestion = statsService.getTrainingSuggestions(userId);
            if (suggestion != null && suggestion.getFocusChars() != null) {
                focusChars = suggestion.getFocusChars().stream()
                        .map(TrainingSuggestionDTO.CharSuggestionItem::getCharacter)
                        .collect(Collectors.toList());
                if (suggestion.getSummary() != null) summary = suggestion.getSummary();
            }
            if (suggestion != null) {
                GamePlayerPerformanceDTO aggregated =
                        difficultyModelService.buildAggregatedPerformance(userId, mode);

                GamePlayerPerformanceDTO perf = new GamePlayerPerformanceDTO();
                if (suggestion.getRecentAccuracy() != null) {
                    perf.setRealAccuracy(BigDecimal.valueOf(suggestion.getRecentAccuracy() / 100.0));
                }
                if (suggestion.getRecentSpeed() != null) {
                    perf.setRealSpeed(BigDecimal.valueOf(suggestion.getRecentSpeed()));
                }
                perf.setCurrentRound(1);
                if (aggregated != null) {
                    perf.setHistoryAvgAccuracy(aggregated.getHistoryAvgAccuracy());
                    perf.setHistoryPeakSpeed(aggregated.getHistoryPeakSpeed());
                    perf.setAvgCharErrorRate(aggregated.getAvgCharErrorRate());
                    perf.setErrorCharRatio(aggregated.getErrorCharRatio());
                }

                UserModelParams params = mathModelService.getUserModelParams(userId, mode);
                if (params == null) params = mathModelService.getDefaultParamsForMode(mode);
                if (params != null) {
                    ModelConstant constant = mathModelService.getModelConstant();

                    GameRecord record = new GameRecord();
                    record.setAvgAccuracy(perf.getRealAccuracy());
                    record.setAvgSpeed(perf.getRealSpeed());
                    record.setPeakSpeed(
                            perf.getHistoryPeakSpeed() != null ? perf.getHistoryPeakSpeed() : perf.getRealSpeed());
                    record.setRoundNum(perf.getCurrentRound());

                    BigDecimal errorRatio = perf.getErrorCharRatio();
                    GameAdjustParam adjustParam = GameCoreUtil.updateGameParam(
                            null, record, constant, params, errorRatio);
                    if (adjustParam != null) {
                        currentDifficulty = adjustParam.getCurrentDifficulty();
                        if (adjustParam.getMonsterSpeed() != null) {
                            double modelSpeed = adjustParam.getMonsterSpeed().doubleValue();
                            suggestedBaseSpeed = Math.max(0.8, Math.min(2.0, modelSpeed));
                        }
                        suggestedMonsterCount = adjustParam.getMonsterCount();
                        suggestedAvgStringLength = adjustParam.getAvgStringLength();
                        suggestedNormalRatio = adjustParam.getNormalRatio();
                        suggestedAttackRatio = adjustParam.getAttackRatio();
                        suggestedEvasiveRatio = adjustParam.getEvasiveRatio();
                        suggestedSmartRatio = adjustParam.getSmartRatio();
                    }
                }
            }
        } catch (Exception e) {
        }
        String suggestedModeClient = GameMode.valueOf(mode).toClientMode();
        GameRecommendDTO dto = GameRecommendDTO.builder()
                .focusChars(focusChars)
                .suggestedBaseSpeed(suggestedBaseSpeed)
                .suggestedMode(suggestedModeClient != null ? suggestedModeClient : "endless")
                .previewWords(previewWords)
                .summary(summary)
                .suggestedMonsterCount(suggestedMonsterCount)
                .suggestedAvgStringLength(suggestedAvgStringLength)
                .suggestedNormalRatio(suggestedNormalRatio)
                .suggestedAttackRatio(suggestedAttackRatio)
                .suggestedEvasiveRatio(suggestedEvasiveRatio)
                .suggestedSmartRatio(suggestedSmartRatio)
                .currentDifficulty(currentDifficulty)
                .build();
        return ResponseEntity.ok(ResultMessage.success(dto));
    }

    @GetMapping("/next-round")
    @Operation(summary = "获取下一轮参数与怪物列表")
    public ResponseEntity<ResultMessage<NextRoundResponseDTO>> getNextRound(
            @RequestParam(value = "roundNum", defaultValue = "1") int roundNum,
            @RequestParam(value = "gameMode", required = false, defaultValue = "endless") String gameMode,
            @RequestParam(value = "customDictIds", required = false) List<Integer> customDictIds,
            @RequestParam(value = "systemDictIds", required = false) List<Integer> systemDictIds) {
        Integer userId = getCurrentUserId();
        GameMode modeEnum = GameMode.fromClientMode(gameMode);
        GameAdjustParam adjustParam = buildAdjustParamForUser(userId, roundNum, modeEnum.name());
        int n = adjustParam.getMonsterCount() != null ? Math.max(3, Math.min(25, adjustParam.getMonsterCount())) : 8;
        List<String> externalWords = null;
        if (modeEnum == GameMode.WORD_SOURCE) {
            externalWords = wordSourceService.getCandidateWords(
                    userId, GameMode.WORD_SOURCE.name(), n * 2,
                    customDictIds != null ? customDictIds : Collections.emptyList(),
                    systemDictIds != null ? systemDictIds : Collections.emptyList());
        } else if (modeEnum == GameMode.LIMIT_TIME) {
            boolean hasDictIds = (customDictIds != null && !customDictIds.isEmpty())
                    || (systemDictIds != null && !systemDictIds.isEmpty());
            if (hasDictIds) {
                externalWords = wordSourceService.getCandidateWords(
                        userId, GameMode.LIMIT_TIME.name(), n * 2,
                        customDictIds != null ? customDictIds : Collections.emptyList(),
                        systemDictIds != null ? systemDictIds : Collections.emptyList());
            }
        }
        List<com.typinggame.entity.game.MonsterData> monsterList =
                evolutionService.generateNextRoundMonsters(userId, roundNum, gameMode, adjustParam, externalWords);
        List<MonsterDataDTO> monsterDTOs = new ArrayList<>();
        for (com.typinggame.entity.game.MonsterData md : monsterList) {
            MonsterDataDTO dto = new MonsterDataDTO();
            dto.setMonsterType(md.getMonsterType());
            dto.setTargetStr(md.getTargetStr());
            dto.setSpeed(md.getSpeed());
            dto.setSurvivalTime(md.getSurvivalTime());
            dto.setDestroyCostTime(md.getDestroyCostTime());
            dto.setTotalCount(md.getTotalCount());
            dto.setInputErrorCount(md.getInputErrorCount());
            dto.setIsTrueMean(md.getIsTrueMean());
            dto.setScore(md.getScore());
            dto.setDifficultyLevel(md.getDifficultyLevel());
            dto.setSpawnWave(md.getSpawnWave());
            dto.setIsParent(md.getIsParent());
            monsterDTOs.add(dto);
        }
        NextRoundResponseDTO response = NextRoundResponseDTO.builder()
                .adjustParam(adjustParam)
                .monsters(monsterDTOs)
                .build();
        return ResponseEntity.ok(ResultMessage.success(response));
    }

    private GameAdjustParam buildAdjustParamForUser(Integer userId, int roundNum, String gameMode) {
        String mode = gameMode != null && !gameMode.isEmpty() ? gameMode : GameMode.ENDLESS_ADAPT.name();
        try {
            GamePlayerPerformanceDTO aggregated = difficultyModelService.buildAggregatedPerformance(userId, mode);
            GamePlayerPerformanceDTO perf = new GamePlayerPerformanceDTO();
            perf.setCurrentRound(roundNum);
            TrainingSuggestionDTO suggestion = statsService.getTrainingSuggestions(userId);
            if (suggestion != null) {
                if (suggestion.getRecentAccuracy() != null) {
                    perf.setRealAccuracy(BigDecimal.valueOf(suggestion.getRecentAccuracy() / 100.0));
                }
                if (suggestion.getRecentSpeed() != null) {
                    perf.setRealSpeed(BigDecimal.valueOf(suggestion.getRecentSpeed()));
                }
            }
            if (aggregated != null) {
                perf.setHistoryAvgAccuracy(aggregated.getHistoryAvgAccuracy());
                perf.setHistoryPeakSpeed(aggregated.getHistoryPeakSpeed());
                perf.setAvgCharErrorRate(aggregated.getAvgCharErrorRate());
                perf.setErrorCharRatio(aggregated.getErrorCharRatio());
            }
            UserModelParams params = mathModelService.getUserModelParams(userId, mode);
            if (params == null) params = mathModelService.getDefaultParamsForMode(mode);
            if (params != null) {
                ModelConstant constant = mathModelService.getModelConstant();
                GameRecord record = new GameRecord();
                record.setAvgAccuracy(perf.getRealAccuracy());
                record.setAvgSpeed(perf.getRealSpeed());
                record.setPeakSpeed(perf.getHistoryPeakSpeed() != null ? perf.getHistoryPeakSpeed() : perf.getRealSpeed());
                record.setRoundNum(roundNum);
                GameAdjustParam computed = GameCoreUtil.updateGameParam(
                        null, record, constant, params, perf.getErrorCharRatio());
                if (computed != null) return computed;
            }
        } catch (Exception ignored) {
        }
        GameAdjustParam defaultParam = new GameAdjustParam();
        ModelConstant constant = mathModelService.getModelConstant();
        defaultParam.setCurrentDifficulty(constant.getInitialDifficulty() != null ? constant.getInitialDifficulty() : new BigDecimal("0.3"));
        defaultParam.setMonsterSpeed(new BigDecimal("1.0"));
        defaultParam.setMonsterCount(8);
        defaultParam.setAvgStringLength(3);
        defaultParam.setNormalRatio(60);
        defaultParam.setAttackRatio(10);
        defaultParam.setEvasiveRatio(15);
        defaultParam.setSmartRatio(15);
        boolean appliedUserConfig = userGameModeConfigRepository.findByUser_UserIdAndModeName(userId, mode).map(uc -> {
            if (uc.getMonsterInitNum() != null) defaultParam.setMonsterCount(Math.max(3, Math.min(25, uc.getMonsterInitNum())));
            parseTypeProToRatios(uc.getMonsterTypePro()).ifPresent(ratios -> {
                defaultParam.setNormalRatio(ratios[0]);
                defaultParam.setAttackRatio(ratios[1]);
                defaultParam.setEvasiveRatio(ratios[2]);
                defaultParam.setSmartRatio(ratios[3]);
            });
            return true;
        }).orElse(false);
        if (!appliedUserConfig) {
            gameModeConfigRepository.findByModeName(mode).ifPresent(config -> {
                if (config.getMonster_init_num() != null) defaultParam.setMonsterCount(Math.max(3, Math.min(25, config.getMonster_init_num())));
                parseTypeProToRatios(config.getMonster_type_pro()).ifPresent(ratios -> {
                    defaultParam.setNormalRatio(ratios[0]);
                    defaultParam.setAttackRatio(ratios[1]);
                    defaultParam.setEvasiveRatio(ratios[2]);
                    defaultParam.setSmartRatio(ratios[3]);
                });
            });
        }
        return defaultParam;
    }

    private List<String> parseCharTypes(String charType) {
        String normalized = normalizeCharTypeString(charType);
        if (normalized.isEmpty()) return java.util.Collections.emptyList();
        return java.util.Arrays.asList(normalized.split(","));
    }

    private String normalizeCharTypeString(String charType) {
        if (charType == null) return "";
        String raw = charType.trim();
        if (raw.isEmpty()) return "";
        String[] parts = raw.split("[,，\\s]+");
        Set<String> allowed = Set.of("lower", "upper", "number", "punct", "operator", "special", "chinese");
        LinkedHashSet<String> out = new LinkedHashSet<>();
        for (String p : parts) {
            if (p == null) continue;
            String s = p.trim().toLowerCase();
            if (s.equals("numbers")) s = "number";
            if (s.equals("punctuation")) s = "punct";
            if (s.equals("operators")) s = "operator";
            if (s.equals("specialchars") || s.equals("special_char") || s.equals("special-char")) s = "special";
            if (allowed.contains(s)) out.add(s);
        }
        return String.join(",", out);
    }

    private java.util.Optional<int[]> parseTypeProToRatios(String monsterTypePro) {
        if (monsterTypePro == null || monsterTypePro.trim().isEmpty()) return java.util.Optional.empty();
        try {
            @SuppressWarnings("unchecked")
            Map<String, Number> map = objectMapper.readValue(monsterTypePro, Map.class);
            int normal = (int) Math.round(map.getOrDefault("NORMAL", 0.5).doubleValue() * 100);
            int attack = (int) Math.round(map.getOrDefault("ATTACKER", 0.2).doubleValue() * 100);
            int evasive = (int) Math.round(map.getOrDefault("EVASIVE", 0.15).doubleValue() * 100);
            int smart = (int) Math.round(map.getOrDefault("SMART", 0.15).doubleValue() * 100);
            normal = Math.max(30, Math.min(80, normal));
            attack = Math.max(0, Math.min(20, attack));
            evasive = Math.max(5, Math.min(25, evasive));
            smart = Math.max(5, Math.min(25, smart));
            smart = 100 - normal - attack - evasive;
            smart = Math.max(5, Math.min(25, smart));
            normal = 100 - attack - evasive - smart;
            normal = Math.max(30, Math.min(80, normal));
            return java.util.Optional.of(new int[]{ normal, attack, evasive, 100 - normal - attack - evasive });
        } catch (Exception e) {
            return java.util.Optional.empty();
        }
    }

    private Integer getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return Integer.parseInt(authentication.getName());
    }
}