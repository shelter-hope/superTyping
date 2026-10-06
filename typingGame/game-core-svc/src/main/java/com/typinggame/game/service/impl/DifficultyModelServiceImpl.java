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

    public DifficultyModelServiceImpl(ModelInitParamsRepository modelInitParamsRepository,
                                      ModelConstantRepository modelConstantRepository,
                                      SystemMonsterCommoConfigRepository monsterCommoConfigRepository,
                                      UserModelParamsRepository userModelParamsRepository) {
        this.modelInitParamsRepository = modelInitParamsRepository;
        this.modelConstantRepository = modelConstantRepository;
        this.monsterCommoConfigRepository = monsterCommoConfigRepository;
        this.userModelParamsRepository = userModelParamsRepository;
    }

    @Override
    public GameAdjustParam calculate(Integer userId, GameMode gameMode, int roundNum, Double recentAccuracy, Double recentSpeed, int weakCharCount) {
        ModelInitParams weights = loadInitParams(gameMode);
        UserModelParams userParams = userModelParamsRepository.findByUserIdAndGameMode(userId, gameMode.name()).orElse(null);
        ModelConstant constants = modelConstantRepository.findAll().stream().findFirst().orElse(null);
        SystemMonsterCommoConfig bounds = monsterCommoConfigRepository.findAll().stream().findFirst().orElse(null);

        double accuracy = normalizeAccuracy(recentAccuracy);
        double speed = Math.max(0, recentSpeed == null ? 0 : recentSpeed);
        double maxSpeed = constants != null && constants.getMaxSpeed() != null ? constants.getMaxSpeed().doubleValue() : 150.0;
        double refPeakSpeed = constants != null && constants.getRefPeakSpeed() != null ? constants.getRefPeakSpeed().doubleValue() : 120.0;
        double weakRatio = Math.min(1.0, Math.max(0.0, weakCharCount / 10.0));
        int safeRound = Math.max(1, roundNum);

        double previous = weight(firstNonNull(userParams == null ? null : userParams.getLastDifficulty(), weights.getInitDifficulty()), 0.30);
        double normalizedSpeed = Math.min(1.0, speed / Math.max(1.0, maxSpeed));
        double historySpeed = Math.min(1.0, speed / Math.max(1.0, refPeakSpeed));
        double performance = 0.45 * accuracy
                + 0.20 * normalizedSpeed
                + 0.10 * historySpeed
                + 0.10 * (1.0 - weakRatio)
                + 0.15 * Math.min(1.0, Math.log(safeRound + 1) / Math.log(32));
        double rawDifficulty = previous + 0.35 * (performance - 0.85);
        double difficulty = clamp(previous + clamp(rawDifficulty - previous, -0.05, 0.05), 0.15, 0.95);

        BigDecimal minSpeed = bounds != null && bounds.getMinSpeed() != null ? bounds.getMinSpeed() : DEFAULT_MIN_SPEED;
        BigDecimal maxMonsterSpeed = bounds != null && bounds.getMaxSpeed() != null ? bounds.getMaxSpeed() : DEFAULT_MAX_SPEED;
        int minLength = bounds != null && bounds.getMinStrLength() != null ? bounds.getMinStrLength() : 1;
        int maxLength = bounds != null && bounds.getMaxStrLength() != null ? bounds.getMaxStrLength() : 15;

        GameAdjustParam param = new GameAdjustParam();
        param.setCurrentDifficulty(scale(difficulty));
        param.setMonsterSpeed(scale(clamp(minSpeed.doubleValue() + difficulty * 1.8, minSpeed.doubleValue(), maxMonsterSpeed.doubleValue())));
        param.setMonsterCount((int) clamp(Math.round(4 + safeRound * 0.8 + difficulty * 12), 3, 25));
        param.setAvgStringLength((int) clamp(Math.round(minLength + difficulty * (maxLength - minLength)), minLength, maxLength));

        int pressure = (int) Math.round(difficulty * 100);
        param.setNormalRatio((int) clamp(80 - pressure * 0.30, 50, 80));
        param.setAttackRatio((int) clamp(5 + pressure * 0.10, 5, 15));
        param.setEvasiveRatio((int) clamp(10 + pressure * 0.10, 10, 20));
        param.setSmartRatio(100 - param.getNormalRatio() - param.getAttackRatio() - param.getEvasiveRatio());
        return param;
    }

    private ModelInitParams loadInitParams(GameMode gameMode) {
        return modelInitParamsRepository.findByGameMode(gameMode.name()).stream().findFirst().orElseGet(ModelInitParams::new);
    }

    private double normalizeAccuracy(Double accuracy) {
        if (accuracy == null) return 0.85;
        return accuracy > 1 ? clamp(accuracy / 100.0, 0, 1) : clamp(accuracy, 0, 1);
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

    private double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}
