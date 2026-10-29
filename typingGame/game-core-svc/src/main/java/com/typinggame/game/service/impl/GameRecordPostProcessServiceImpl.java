package com.typinggame.game.service.impl;

import com.typinggame.dto.dictionary.DictionaryPracticeUpdateDTO;
import com.typinggame.dto.game.GameRecordUploadDTO;
import com.typinggame.game.entity.GameRecord;
import com.typinggame.game.entity.GameRound;
import com.typinggame.game.entity.MonsterData;
import com.typinggame.game.entity.StringParent;
import com.typinggame.entity.model.ModelInitParams;
import com.typinggame.game.entity.UserModelAdaptLog;
import com.typinggame.game.entity.UserModelParams;
import com.typinggame.entity.user.UserDailyStats;
import com.typinggame.enums.GameMode;
import com.typinggame.game.repository.GameRoundRepository;
import com.typinggame.game.repository.GameUserDailyStatsRepository;
import com.typinggame.game.repository.ModelInitParamsRepository;
import com.typinggame.game.repository.MonsterDataRepository;
import com.typinggame.game.repository.StringParentRepository;
import com.typinggame.game.repository.UserModelAdaptLogRepository;
import com.typinggame.game.repository.UserModelParamsRepository;
import com.typinggame.game.rule.DifficultyEngine;
import com.typinggame.game.rule.DifficultyRule;
import com.typinggame.game.rule.GeneticRule;
import com.typinggame.game.rule.RuleConfigProvider;
import com.typinggame.game.service.GameRecordPostProcessService;
import com.typinggame.game.word.DictionaryPracticeClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class GameRecordPostProcessServiceImpl implements GameRecordPostProcessService {

    private final GameUserDailyStatsRepository dailyStatsRepository;
    private final UserModelParamsRepository userModelParamsRepository;
    private final ModelInitParamsRepository modelInitParamsRepository;
    private final UserModelAdaptLogRepository adaptLogRepository;
    private final StringParentRepository stringParentRepository;
    private final MonsterDataRepository monsterDataRepository;
    private final DictionaryPracticeClient dictionaryPracticeClient;
    private final GameRoundRepository gameRoundRepository;
    private final RuleConfigProvider ruleConfigProvider;

    public GameRecordPostProcessServiceImpl(GameUserDailyStatsRepository dailyStatsRepository,
                                             UserModelParamsRepository userModelParamsRepository,
                                             ModelInitParamsRepository modelInitParamsRepository,
                                             UserModelAdaptLogRepository adaptLogRepository,
                                             StringParentRepository stringParentRepository,
                                             MonsterDataRepository monsterDataRepository,
                                             DictionaryPracticeClient dictionaryPracticeClient,
                                             GameRoundRepository gameRoundRepository,
                                             RuleConfigProvider ruleConfigProvider) {
        this.dailyStatsRepository = dailyStatsRepository;
        this.userModelParamsRepository = userModelParamsRepository;
        this.modelInitParamsRepository = modelInitParamsRepository;
        this.adaptLogRepository = adaptLogRepository;
        this.stringParentRepository = stringParentRepository;
        this.monsterDataRepository = monsterDataRepository;
        this.dictionaryPracticeClient = dictionaryPracticeClient;
        this.gameRoundRepository = gameRoundRepository;
        this.ruleConfigProvider = ruleConfigProvider;
    }

    @Override
    @Transactional
    public BigDecimal process(Integer userId, GameRecord record, GameRecordUploadDTO uploadDTO, List<MonsterData> monsters) {
        upsertDailyStats(userId, record);
        BigDecimal currentDifficulty = updateUserModelAndLog(userId, record);
        updateStringParents(userId, monsters);
        updateDictionaryWordStats(uploadDTO, monsters);
        return currentDifficulty;
    }

    private void upsertDailyStats(Integer userId, GameRecord record) {
        Date statDate = atStartOfDay(record.getCreateTime() == null ? new Date() : record.getCreateTime());
        UserDailyStats stats = dailyStatsRepository.findByUserIdAndStatDate(userId, statDate).orElseGet(() -> {
            UserDailyStats created = new UserDailyStats();
            created.setUserId(userId);
            created.setStatDate(statDate);
            created.setGameCount(0);
            created.setTotalScore(0);
            created.setTotalChars(0);
            created.setCorrectChars(0);
            created.setAvgSpeed(BigDecimal.ZERO);
            created.setAvgAccuracy(BigDecimal.ZERO);
            created.setPeakSpeed(BigDecimal.ZERO);
            created.setPeakAccuracy(BigDecimal.ZERO);
            created.setCreateTime(new Date());
            return created;
        });
        int oldCount = safeInt(stats.getGameCount());
        int newCount = oldCount + 1;
        stats.setGameCount(newCount);
        stats.setTotalScore(safeInt(stats.getTotalScore()) + safeInt(record.getTotalScore()));
        stats.setTotalChars(safeInt(stats.getTotalChars()) + safeInt(record.getTotalChars()));
        stats.setCorrectChars(safeInt(stats.getCorrectChars()) + safeInt(record.getCorrectChars()));
        stats.setAvgSpeed(weightedAverage(stats.getAvgSpeed(), oldCount, record.getAvgSpeed(), newCount));
        stats.setAvgAccuracy(weightedAverage(stats.getAvgAccuracy(), oldCount, record.getAvgAccuracy(), newCount));
        stats.setPeakSpeed(max(stats.getPeakSpeed(), record.getPeakSpeed()));
        stats.setPeakAccuracy(max(stats.getPeakAccuracy(), record.getPeakAccuracy()));
        stats.setGameMode(record.getGameMode());
        stats.setUpdateTime(new Date());
        dailyStatsRepository.save(stats);
    }

    private BigDecimal updateUserModelAndLog(Integer userId, GameRecord record) {
        UserModelParams params = resolveUserModelParams(userId, record.getGameMode());
        BigDecimal before = params.getLastDifficulty();
        BigDecimal after = calculatePostGameDifficulty(before, record);
        params.setLatestRound(record.getRoundNum());
        params.setLastDifficulty(after);
        params.setConsecutiveMismatchRound(resolveMismatchRound(params, before, after));
        params.setLastAdjustTime(new Date());
        params.setUpdateTime(new Date());
        userModelParamsRepository.save(params);
        writeAdaptLog(userId, record, before, after, params);
        return after;
    }

    private UserModelParams resolveUserModelParams(Integer userId, String gameMode) {
        return userModelParamsRepository.findByUserIdAndGameMode(userId, gameMode)
                .orElseGet(() -> createUserModelParams(userId, gameMode));
    }

    private UserModelParams createUserModelParams(Integer userId, String gameMode) {
        ModelInitParams init = modelInitParamsRepository.findByGameMode(gameMode).stream().findFirst().orElseGet(ModelInitParams::new);
        Date now = new Date();
        UserModelParams params = new UserModelParams();
        params.setUserId(userId);
        params.setGameMode(gameMode);
        params.setLatestRound(0);
        params.setLastDifficulty(defaultDecimal(init.getInitDifficulty(), "0.30"));
        params.setWeightRealAccuracy(defaultDecimal(init.getWeightRealAccuracy(), "0.35"));
        params.setWeightRealSpeed(defaultDecimal(init.getWeightRealSpeed(), "0.25"));
        params.setWeightCharErrorRate(defaultDecimal(init.getWeightCharErrorRate(), "0.20"));
        params.setWeightHistoryAccuracy(defaultDecimal(init.getWeightHistoryAccuracy(), "0.10"));
        params.setWeightHistoryPeakSpeed(defaultDecimal(init.getWeightHistoryPeakSpeed(), "0.05"));
        params.setWeightErrorCharRatio(defaultDecimal(init.getWeightErrorCharRatio(), "0.05"));
        params.setWeightCurrentRound(defaultDecimal(init.getWeightCurrentRound(), "0.10"));
        params.setFatigueBufferFlag(Boolean.FALSE);
        params.setProgressSpeedFlag(Boolean.FALSE);
        params.setRollbackFlag(Boolean.FALSE);
        params.setMaxAdjustRatio(defaultDecimal(init.getMaxAdjustRatio(), "0.30"));
        params.setSmoothAdjustCoeff(defaultDecimal(init.getSmoothAdjustCoeff(), "0.80"));
        GeneticRule genetic = ruleConfigProvider.activeGenetic();
        params.setCrossoverRate(BigDecimal.ONE);
        params.setMutationRate(BigDecimal.valueOf(genetic.getCrossRoundMutationRate()));
        params.setSpeedAdjustCoeff(BigDecimal.ONE);
        params.setLengthAdjustCoeff(BigDecimal.ONE);
        params.setConsecutiveMismatchRound(0);
        params.setCreateTime(now);
        params.setUpdateTime(now);
        return userModelParamsRepository.save(params);
    }

    private BigDecimal calculatePostGameDifficulty(BigDecimal before, GameRecord record) {
        if (record.getLastRoundId() != null) {
            GameRound settled = gameRoundRepository.findById(record.getLastRoundId()).orElse(null);
            if (settled != null && settled.getDifficultyAfter() != null) {
                return settled.getDifficultyAfter().setScale(4, RoundingMode.HALF_UP);
            }
        }
        DifficultyRule rule = ruleConfigProvider.activeDifficulty();
        double base = before == null ? rule.getInitialDifficulty() : before.doubleValue();
        Double accuracy = record.getAccuracyRatio() != null
                ? record.getAccuracyRatio().doubleValue()
                : (record.getAvgAccuracy() == null ? null : record.getAvgAccuracy().doubleValue());
        double next = DifficultyEngine.nextDifficulty(rule, base, accuracy, null, Math.max(1, safeInt(record.getRoundNum())), 0);
        return BigDecimal.valueOf(next).setScale(4, RoundingMode.HALF_UP);
    }

    private int resolveMismatchRound(UserModelParams params, BigDecimal before, BigDecimal after) {
        if (before == null || after == null || before.compareTo(after) == 0) {
            return 0;
        }
        return safeInt(params.getConsecutiveMismatchRound()) + 1;
    }

    private void writeAdaptLog(Integer userId, GameRecord record, BigDecimal before, BigDecimal after, UserModelParams params) {
        DifficultyRule rule = ruleConfigProvider.activeDifficulty();
        int monsterCount = DifficultyEngine.monsterCount(rule, after == null ? rule.getInitialDifficulty() : after.doubleValue(),
                Math.max(1, safeInt(record.getRoundNum())));
        GameRecord recordRef = new GameRecord();
        recordRef.setRecordId(record.getRecordId());
        UserModelAdaptLog log = new UserModelAdaptLog();
        log.setUserId(userId);
        log.setGameRecord(recordRef);
        log.setGameMode(record.getGameMode());
        log.setRoundNum(record.getRoundNum());
        log.setBeforeDifficulty(scale4(before));
        log.setAfterDifficulty(scale4(after));
        log.setTriggerRule("SESSION_SUMMARY");
        log.setAdjustParams("{"
                + "\"accuracy\":" + decimal(record.getAvgAccuracy())
                + ",\"speed\":" + decimal(record.getAvgSpeed())
                + ",\"weakCharCount\":0"
                + ",\"ruleVersion\":" + ruleConfigProvider.difficultyVersion()
                + ",\"monsterCount\":" + monsterCount
                + ",\"consecutiveMismatchRound\":" + safeInt(params.getConsecutiveMismatchRound())
                + ",\"triggerHint\":\"" + resolveTriggerHint(record.getAvgAccuracy(), rule) + "\""
                + "}");
        log.setLogTime(LocalDateTime.now());
        adaptLogRepository.save(log);
    }

    static String resolveTriggerHint(BigDecimal accuracy, DifficultyRule rule) {
        if (accuracy == null) return "SMOOTH_ROUND_PROGRESS";
        double percent = accuracy.doubleValue() > 1 ? accuracy.doubleValue() : accuracy.doubleValue() * 100.0;
        if (percent >= rule.getHighAccuracyAdvanceThreshold()) return "HIGH_ACCURACY_ADVANCE";
        if (percent <= rule.getLowAccuracyRollbackThreshold()) return "LOW_ACCURACY_ROLLBACK";
        return "SMOOTH_ROUND_PROGRESS";
    }

    private BigDecimal scale4(BigDecimal value) {
        return value == null ? null : value.setScale(4, RoundingMode.HALF_UP);
    }

    private void updateStringParents(Integer userId, List<MonsterData> monsters) {
        if (monsters == null || monsters.isEmpty()) return;
        List<MonsterData> parents = monsters.stream()
                .filter(m -> m.getMonsterId() != null && m.getTargetStr() != null && !m.getTargetStr().isBlank())
                .sorted(Comparator.comparing((MonsterData m) -> safeInt(m.getScore())).reversed()
                        .thenComparing(Comparator.comparing((MonsterData m) -> safeInt(m.getSurvivalTime())).reversed())
                        .thenComparing(MonsterData::getMonsterId))
                .limit(ruleConfigProvider.activeGenetic().getCrossRoundParentLimit())
                .collect(Collectors.toList());
        if (parents.isEmpty()) return;
        parents.forEach(m -> m.setIsParent(true));
        monsterDataRepository.saveAll(parents);
        String ids = parents.stream().map(MonsterData::getMonsterId).map(String::valueOf).collect(Collectors.joining(","));
        StringParent parent = stringParentRepository.findByUserId(userId).orElseGet(() -> {
            StringParent created = new StringParent();
            created.setUserId(userId);
            created.setCreateTime(LocalDateTime.now());
            return created;
        });
        parent.setStrParents(ids);
        parent.setUpdateTime(LocalDateTime.now());
        stringParentRepository.save(parent);
    }

    private void updateDictionaryWordStats(GameRecordUploadDTO uploadDTO, List<MonsterData> monsters) {
        if (uploadDTO == null || uploadDTO.getCustomDictIds() == null || uploadDTO.getCustomDictIds().isEmpty() || monsters == null || monsters.isEmpty()) {
            return;
        }
        Map<String, List<MonsterData>> monstersByWord = monsters.stream()
                .filter(monster -> monster.getTargetStr() != null && !monster.getTargetStr().isBlank())
                .collect(Collectors.groupingBy(MonsterData::getTargetStr));
        if (monstersByWord.isEmpty()) {
            return;
        }
        List<DictionaryPracticeUpdateDTO.PracticeItem> items = new ArrayList<>(monstersByWord.size());
        monstersByWord.forEach((word, matched) -> {
            DictionaryPracticeUpdateDTO.PracticeItem item = new DictionaryPracticeUpdateDTO.PracticeItem();
            item.setWord(word);
            item.setPracticeCount(matched.size());
            item.setErrorCount(matched.stream().mapToInt(monster -> safeInt(monster.getInputErrorCount())).sum());
            items.add(item);
        });
        dictionaryPracticeClient.recordPractice(uploadDTO.getCustomDictIds(), items);
    }

    private Date atStartOfDay(Date date) {
        return Date.from(date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate().atStartOfDay(ZoneId.systemDefault()).toInstant());
    }

    private BigDecimal weightedAverage(BigDecimal oldValue, int oldCount, BigDecimal newValue, int newCount) {
        BigDecimal safeOld = oldValue == null ? BigDecimal.ZERO : oldValue;
        BigDecimal safeNew = newValue == null ? BigDecimal.ZERO : newValue;
        return safeOld.multiply(BigDecimal.valueOf(oldCount)).add(safeNew)
                .divide(BigDecimal.valueOf(Math.max(1, newCount)), 2, RoundingMode.HALF_UP);
    }

    private BigDecimal max(BigDecimal left, BigDecimal right) {
        if (left == null) return right == null ? BigDecimal.ZERO : right;
        if (right == null) return left;
        return left.max(right);
    }

    private BigDecimal defaultDecimal(BigDecimal value, String fallback) {
        return value == null ? new BigDecimal(fallback) : value;
    }

    private String decimal(BigDecimal value) {
        return value == null ? "0" : value.toPlainString();
    }

    private int safeInt(Integer value) {
        return value == null ? 0 : value;
    }

    private double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}
