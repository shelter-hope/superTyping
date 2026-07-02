package com.typinggame.game.controller;

import com.typinggame.dto.game.GameAdjustParam;
import com.typinggame.dto.game.GameCharTypeConfigDTO;
import com.typinggame.dto.game.GameCharTypeConfigUpdateDTO;
import com.typinggame.dto.game.GameRecommendDTO;
import com.typinggame.dto.game.MonsterDataDTO;
import com.typinggame.dto.game.NextRoundResponseDTO;
import com.typinggame.dto.stats.TrainingSuggestionDTO;
import com.typinggame.entity.user.CharacterStats;
import com.typinggame.enums.GameMode;
import com.typinggame.game.service.CharacterStatsService;
import com.typinggame.game.service.StatsService;
import com.typinggame.vo.ResultMessage;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/game/config")
public class GameConfigController {

    private static final String DEFAULT_CHAR_TYPES = "lower,upper,number,punct";
    private static final List<String> DEFAULT_POOL = Arrays.asList(
            "a", "s", "d", "f", "j", "k", "l", "e", "r", "t", "i", "o", "n", "m", "1", "2", "3", ".", ","
    );

    private final StatsService statsService;
    private final CharacterStatsService characterStatsService;

    public GameConfigController(StatsService statsService, CharacterStatsService characterStatsService) {
        this.statsService = statsService;
        this.characterStatsService = characterStatsService;
    }

    @GetMapping("/char-types")
    public ResponseEntity<ResultMessage<GameCharTypeConfigDTO>> getCharTypes(
            @RequestParam(value = "gameMode", required = false, defaultValue = "endless") String gameMode) {
        GameMode mode = GameMode.fromClientMode(gameMode);
        GameCharTypeConfigDTO dto = GameCharTypeConfigDTO.builder()
                .gameMode(mode.toClientMode())
                .charType(DEFAULT_CHAR_TYPES)
                .charTypes(parseCharTypes(DEFAULT_CHAR_TYPES))
                .build();
        return ResponseEntity.ok(ResultMessage.success(dto));
    }

    @PutMapping("/char-types")
    public ResponseEntity<ResultMessage<Boolean>> updateCharTypes(@RequestBody GameCharTypeConfigUpdateDTO request) {
        // 当前阶段先保持接口可用；后续阶段迁移用户级配置表后再持久化。
        return ResponseEntity.ok(ResultMessage.success(true));
    }

    @GetMapping("/recommend")
    public ResponseEntity<ResultMessage<GameRecommendDTO>> getRecommend(
            @RequestParam(value = "gameMode", required = false, defaultValue = "endless") String gameMode) {
        Integer userId = getCurrentUserId();
        GameMode mode = GameMode.fromClientMode(gameMode);
        TrainingSuggestionDTO suggestion = statsService.getTrainingSuggestions(userId);
        List<String> focusChars = suggestion.getFocusChars() == null ? Collections.emptyList() : suggestion.getFocusChars().stream()
                .map(TrainingSuggestionDTO.CharSuggestionItem::getCharacter)
                .filter(s -> s != null && !s.isBlank())
                .limit(10)
                .collect(Collectors.toList());
        List<String> previewWords = buildWords(focusChars, 8, 3);
        GameAdjustParam adjustParam = buildAdjustParam(1, suggestion.getRecentAccuracy(), suggestion.getRecentSpeed());

        GameRecommendDTO dto = GameRecommendDTO.builder()
                .focusChars(focusChars)
                .suggestedBaseSpeed(adjustParam.getMonsterSpeed().doubleValue())
                .suggestedMode(mode.toClientMode())
                .previewWords(previewWords)
                .summary(suggestion.getSummary())
                .suggestedMonsterCount(adjustParam.getMonsterCount())
                .suggestedAvgStringLength(adjustParam.getAvgStringLength())
                .suggestedNormalRatio(adjustParam.getNormalRatio())
                .suggestedAttackRatio(adjustParam.getAttackRatio())
                .suggestedEvasiveRatio(adjustParam.getEvasiveRatio())
                .suggestedSmartRatio(adjustParam.getSmartRatio())
                .currentDifficulty(adjustParam.getCurrentDifficulty())
                .build();
        return ResponseEntity.ok(ResultMessage.success(dto));
    }

    @GetMapping("/next-round")
    public ResponseEntity<ResultMessage<NextRoundResponseDTO>> getNextRound(
            @RequestParam(value = "roundNum", defaultValue = "1") int roundNum,
            @RequestParam(value = "gameMode", required = false, defaultValue = "endless") String gameMode,
            @RequestParam(value = "customDictIds", required = false) List<Integer> customDictIds,
            @RequestParam(value = "systemDictIds", required = false) List<Integer> systemDictIds) {
        Integer userId = getCurrentUserId();
        TrainingSuggestionDTO suggestion = statsService.getTrainingSuggestions(userId);
        GameAdjustParam adjustParam = buildAdjustParam(roundNum, suggestion.getRecentAccuracy(), suggestion.getRecentSpeed());
        List<CharacterStats> weakChars = characterStatsService.getWeakChars(userId, 10);
        List<String> focusChars = weakChars.stream()
                .map(CharacterStats::getCharValue)
                .filter(s -> s != null && !s.isBlank())
                .collect(Collectors.toList());
        List<String> words = buildWords(focusChars, adjustParam.getMonsterCount(), adjustParam.getAvgStringLength());
        List<MonsterDataDTO> monsters = buildMonsters(words, adjustParam, roundNum);
        return ResponseEntity.ok(ResultMessage.success(NextRoundResponseDTO.builder()
                .adjustParam(adjustParam)
                .monsters(monsters)
                .build()));
    }

    private GameAdjustParam buildAdjustParam(int roundNum, Double recentAccuracy, Double recentSpeed) {
        double accuracy = recentAccuracy == null || recentAccuracy <= 0 ? 90.0 : recentAccuracy;
        double speed = recentSpeed == null || recentSpeed <= 0 ? 1.0 : recentSpeed;
        double pressure = accuracy >= 95 ? 0.1 : accuracy >= 85 ? 0.2 : accuracy >= 70 ? 0.35 : 0.5;
        double difficulty = Math.min(0.95, 0.25 + roundNum * 0.03 + pressure);

        GameAdjustParam param = new GameAdjustParam();
        param.setCurrentDifficulty(BigDecimal.valueOf(difficulty).setScale(2, RoundingMode.HALF_UP));
        param.setMonsterSpeed(BigDecimal.valueOf(Math.max(0.8, Math.min(3.0, 0.8 + difficulty * 1.6 + Math.min(speed, 3.0) * 0.05))).setScale(2, RoundingMode.HALF_UP));
        param.setMonsterCount(Math.max(3, Math.min(25, 5 + roundNum + (int) Math.round(difficulty * 8))));
        param.setAvgStringLength(Math.max(1, Math.min(15, 2 + (int) Math.round(difficulty * 6))));
        param.setNormalRatio(Math.max(45, 75 - (int) Math.round(difficulty * 25)));
        param.setAttackRatio(Math.min(20, 5 + (int) Math.round(difficulty * 10)));
        param.setEvasiveRatio(Math.min(25, 10 + (int) Math.round(difficulty * 8)));
        param.setSmartRatio(Math.max(5, 100 - param.getNormalRatio() - param.getAttackRatio() - param.getEvasiveRatio()));
        return param;
    }

    private List<MonsterDataDTO> buildMonsters(List<String> words, GameAdjustParam adjustParam, int roundNum) {
        List<MonsterDataDTO> monsters = new ArrayList<>();
        for (int i = 0; i < words.size(); i++) {
            MonsterDataDTO monster = new MonsterDataDTO();
            monster.setMonsterType(pickMonsterType(i, adjustParam));
            monster.setTargetStr(words.get(i));
            monster.setSpeed(adjustParam.getMonsterSpeed());
            monster.setSurvivalTime(0);
            monster.setDestroyCostTime(0);
            monster.setTotalCount(words.get(i).length());
            monster.setInputErrorCount(0);
            monster.setIsTrueMean(1);
            monster.setScore(10 + roundNum * 2);
            monster.setDifficultyLevel(roundNum);
            monster.setSpawnWave(roundNum);
            monster.setIsParent(false);
            monsters.add(monster);
        }
        return monsters;
    }

    private String pickMonsterType(int index, GameAdjustParam adjustParam) {
        int bucket = index % 100;
        if (bucket < adjustParam.getNormalRatio()) return "NORMAL";
        if (bucket < adjustParam.getNormalRatio() + adjustParam.getAttackRatio()) return "ATTACKER";
        if (bucket < adjustParam.getNormalRatio() + adjustParam.getAttackRatio() + adjustParam.getEvasiveRatio()) return "EVASIVE";
        return "SMART";
    }

    private List<String> buildWords(List<String> focusChars, int count, int length) {
        List<String> pool = new ArrayList<>(DEFAULT_POOL);
        if (focusChars != null && !focusChars.isEmpty()) {
            pool.addAll(0, focusChars);
        }
        List<String> words = new ArrayList<>();
        int safeCount = Math.max(3, Math.min(25, count));
        int safeLength = Math.max(1, Math.min(15, length));
        for (int i = 0; i < safeCount; i++) {
            StringBuilder word = new StringBuilder();
            for (int j = 0; j < safeLength; j++) {
                word.append(pool.get((i + j) % pool.size()));
            }
            words.add(word.toString());
        }
        return words;
    }

    private List<String> parseCharTypes(String value) {
        if (value == null || value.isBlank()) return Collections.emptyList();
        Set<String> set = new LinkedHashSet<>();
        for (String item : value.split(",")) {
            String trimmed = item.trim();
            if (!trimmed.isEmpty()) set.add(trimmed);
        }
        return new ArrayList<>(set);
    }

    private Integer getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return Integer.parseInt(authentication.getName());
    }
}
