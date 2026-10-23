package com.typinggame.game.service.impl;

import com.typinggame.dto.game.GameAdjustParam;
import com.typinggame.entity.config.SystemMonsterCommoConfig;
import com.typinggame.entity.model.ModelConstant;
import com.typinggame.entity.model.ModelInitParams;
import com.typinggame.entity.model.UserModelParams;
import com.typinggame.enums.GameMode;
import com.typinggame.game.repository.ModelConstantRepository;
import com.typinggame.game.repository.ModelInitParamsRepository;
import com.typinggame.game.repository.SystemMonsterCommoConfigRepository;
import com.typinggame.game.repository.UserModelParamsRepository;
import com.typinggame.game.rule.DifficultyEngine;
import com.typinggame.game.rule.DifficultyRule;
import com.typinggame.game.rule.RuleConfigProvider;
import com.typinggame.game.service.DifficultyModelService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
public class DifficultyModelServiceImpl implements DifficultyModelService {

    private static final BigDecimal DEFAULT_MIN_SPEED = BigDecimal.valueOf(0.8);
    private static final BigDecimal DEFAULT_MAX_SPEED = BigDecimal.valueOf(3.0);

    private final ModelInitParamsRepository modelInitParamsRepository;
    private final ModelConstantRepository modelConstantRepository;
    private final SystemMonsterCommoConfigRepository monsterCommoConfigRepository;
    private final UserModelParamsRepository userModelParamsRepository;
    private final RuleConfigProvider ruleConfigProvider;

    public DifficultyModelServiceImpl(ModelInitParamsRepository modelInitParamsRepository,
                                      ModelConstantRepository modelConstantRepository,
                                      SystemMonsterCommoConfigRepository monsterCommoConfigRepository,
                                      UserModelParamsRepository userModelParamsRepository,
                                      RuleConfigProvider ruleConfigProvider) {
        this.modelInitParamsRepository = modelInitParamsRepository;
        this.modelConstantRepository = modelConstantRepository;
        this.monsterCommoConfigRepository = monsterCommoConfigRepository;
        this.userModelParamsRepository = userModelParamsRepository;
        this.ruleConfigProvider = ruleConfigProvider;
    }

    @Override
    public GameAdjustParam calculate(Integer userId, GameMode gameMode, int roundNum, Double recentAccuracy, Double recentSpeed, int weakCharCount) {
        return calculate(userId, gameMode, roundNum, recentAccuracy, recentSpeed, weakCharCount, null);
    }

    @Override
    public GameAdjustParam calculate(Integer userId, GameMode gameMode, int roundNum, Double recentAccuracy, Double recentSpeed, int weakCharCount, Double previousDifficulty) {
        DifficultyRule rule = copyRule(ruleConfigProvider.activeDifficulty());
        ModelInitParams weights = loadInitParams(gameMode);
        UserModelParams userParams = userModelParamsRepository.findByUserIdAndGameMode(userId, gameMode.name()).orElse(null);
        ModelConstant constants = modelConstantRepository.findAll().stream().findFirst().orElse(null);
        SystemMonsterCommoConfig bounds = monsterCommoConfigRepository.findAll().stream().findFirst().orElse(null);
        if (constants != null && constants.getMaxSpeed() != null) rule.setMaxReferenceSpeed(constants.getMaxSpeed().doubleValue());
        if (constants != null && constants.getRefPeakSpeed() != null) rule.setRefPeakSpeed(constants.getRefPeakSpeed().doubleValue());

        double previous = previousDifficulty != null
                ? previousDifficulty
                : storedDifficulty(userParams, weights, rule);
        double difficulty = DifficultyEngine.nextDifficulty(rule, previous, recentAccuracy, recentSpeed, roundNum, weakCharCount);
        return present(rule, difficulty, roundNum, bounds);
    }

    @Override
    public GameAdjustParam project(Integer userId, GameMode gameMode, int roundNum, Double difficulty) {
        DifficultyRule rule = copyRule(ruleConfigProvider.activeDifficulty());
        ModelInitParams weights = loadInitParams(gameMode);
        UserModelParams userParams = userModelParamsRepository.findByUserIdAndGameMode(userId, gameMode.name()).orElse(null);
        ModelConstant constants = modelConstantRepository.findAll().stream().findFirst().orElse(null);
        SystemMonsterCommoConfig bounds = monsterCommoConfigRepository.findAll().stream().findFirst().orElse(null);
        if (constants != null && constants.getMaxSpeed() != null) rule.setMaxReferenceSpeed(constants.getMaxSpeed().doubleValue());
        if (constants != null && constants.getRefPeakSpeed() != null) rule.setRefPeakSpeed(constants.getRefPeakSpeed().doubleValue());
        double resolved = difficulty != null ? difficulty : storedDifficulty(userParams, weights, rule);
        return present(rule, resolved, roundNum, bounds);
    }

    private GameAdjustParam present(DifficultyRule rule, double difficulty, int roundNum, SystemMonsterCommoConfig bounds) {
        BigDecimal minSpeed = bounds != null && bounds.getMinSpeed() != null ? bounds.getMinSpeed() : DEFAULT_MIN_SPEED;
        BigDecimal maxMonsterSpeed = bounds != null && bounds.getMaxSpeed() != null ? bounds.getMaxSpeed() : DEFAULT_MAX_SPEED;
        int minLength = bounds != null && bounds.getMinStrLength() != null ? bounds.getMinStrLength() : 1;
        int maxLength = bounds != null && bounds.getMaxStrLength() != null ? bounds.getMaxStrLength() : 15;
        DifficultyEngine.Outcome outcome = DifficultyEngine.outcome(rule, difficulty, minSpeed.doubleValue(), maxMonsterSpeed.doubleValue(), minLength, maxLength);

        GameAdjustParam param = new GameAdjustParam();
        param.setCurrentDifficulty(scale(outcome.difficulty()));
        param.setMonsterSpeed(scale(outcome.speed()));
        param.setMonsterCount(DifficultyEngine.monsterCount(rule, outcome.difficulty(), roundNum));
        param.setAvgStringLength(outcome.length());
        param.setNormalRatio(outcome.normalRatio());
        param.setAttackRatio(outcome.attackRatio());
        param.setEvasiveRatio(outcome.evasiveRatio());
        param.setSmartRatio(outcome.smartRatio());
        return param;
    }

    private DifficultyRule copyRule(DifficultyRule source) {
        DifficultyRule copy = new DifficultyRule();
        copy.setWeightAccuracy(source.getWeightAccuracy());
        copy.setWeightNormalizedSpeed(source.getWeightNormalizedSpeed());
        copy.setWeightHistorySpeed(source.getWeightHistorySpeed());
        copy.setWeightComposure(source.getWeightComposure());
        copy.setWeightRound(source.getWeightRound());
        copy.setTargetAccuracy(source.getTargetAccuracy());
        copy.setGain(source.getGain());
        copy.setStep(source.getStep());
        copy.setMinDifficulty(source.getMinDifficulty());
        copy.setMaxDifficulty(source.getMaxDifficulty());
        copy.setInitialDifficulty(source.getInitialDifficulty());
        copy.setMaxReferenceSpeed(source.getMaxReferenceSpeed());
        copy.setRefPeakSpeed(source.getRefPeakSpeed());
        copy.setWeakCharDivisor(source.getWeakCharDivisor());
        copy.setSpeedSpan(source.getSpeedSpan());
        copy.setCountBase(source.getCountBase());
        copy.setCountRoundFactor(source.getCountRoundFactor());
        copy.setCountDifficultyFactor(source.getCountDifficultyFactor());
        copy.setCountMin(source.getCountMin());
        copy.setCountMax(source.getCountMax());
        copy.setNormalBase(source.getNormalBase());
        copy.setNormalSlope(source.getNormalSlope());
        copy.setNormalMin(source.getNormalMin());
        copy.setNormalMax(source.getNormalMax());
        copy.setAttackBase(source.getAttackBase());
        copy.setAttackSlope(source.getAttackSlope());
        copy.setAttackMin(source.getAttackMin());
        copy.setAttackMax(source.getAttackMax());
        copy.setEvasiveBase(source.getEvasiveBase());
        copy.setEvasiveSlope(source.getEvasiveSlope());
        copy.setEvasiveMin(source.getEvasiveMin());
        copy.setEvasiveMax(source.getEvasiveMax());
        copy.setRoundLogBase(source.getRoundLogBase());
        copy.setHighAccuracyAdvanceThreshold(source.getHighAccuracyAdvanceThreshold());
        copy.setLowAccuracyRollbackThreshold(source.getLowAccuracyRollbackThreshold());
        return copy;
    }

    private ModelInitParams loadInitParams(GameMode gameMode) {
        return modelInitParamsRepository.findByGameMode(gameMode.name()).stream().findFirst().orElseGet(ModelInitParams::new);
    }

    private double storedDifficulty(UserModelParams userParams, ModelInitParams weights, DifficultyRule rule) {
        return weight(firstNonNull(userParams == null ? null : userParams.getLastDifficulty(), weights.getInitDifficulty()), rule.getInitialDifficulty());
    }

    private double weight(BigDecimal value, double fallback) {
        return value == null ? fallback : value.doubleValue();
    }

    private BigDecimal firstNonNull(BigDecimal first, BigDecimal second) {
        return first != null ? first : second;
    }

    private BigDecimal scale(double value) {
        return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP);
    }
}
