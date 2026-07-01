package com.typinggame.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

import com.typinggame.dto.game.GamePlayerPerformanceDTO;
import com.typinggame.entity.game.GameRecord;
import com.typinggame.entity.model.ModelConstant;
import com.typinggame.entity.model.UserModelParams;
import lombok.extern.slf4j.Slf4j;

/**
 * 综合难度系数计算工具（核心算法入口）
 * 对应文档：核心调节算法模型-综合难度系数（Dt）计算
 */
@Slf4j
public class DifficultyCalculationUtil {
    private static final int SCALE = 4; // 精度保留4位小数
    private static final RoundingMode ROUND_MODE = RoundingMode.HALF_UP;

    /**
     * 基于聚合后的玩家表现指标计算当前轮次综合难度系数 Dt。
     * Dt = α·Dt-1 + (1-α)·[WA·At + WS·(St/Smax) + WE·Eit + WAh·Ah + WSh·(Shmax/Sref) + WRe·Re + WK·log2(K)]
     */
    public static BigDecimal calculateCurrentDifficulty(BigDecimal lastDifficulty,
                                                        GamePlayerPerformanceDTO perf,
                                                        ModelConstant constant,
                                                        UserModelParams params) {
        if (perf == null || constant == null || params == null) {
            BigDecimal fallback = lastDifficulty != null
                    ? lastDifficulty
                    : (constant != null ? constant.getInitialDifficulty() : BigDecimal.ZERO);
            log.info("DifficultyCalc: skip (perf/constant/params null), lastDifficulty={}, fallbackDt={}",
                    lastDifficulty, fallback);
            return fallback;
        }

        BigDecimal zero = BigDecimal.ZERO;

        BigDecimal At = perf.getRealAccuracy() != null ? perf.getRealAccuracy() : zero;
        BigDecimal St = perf.getRealSpeed() != null ? perf.getRealSpeed() : zero;
        BigDecimal Eit = perf.getAvgCharErrorRate() != null ? perf.getAvgCharErrorRate() : zero;
        BigDecimal Ah = perf.getHistoryAvgAccuracy() != null ? perf.getHistoryAvgAccuracy() : At;
        BigDecimal Shmax = perf.getHistoryPeakSpeed() != null ? perf.getHistoryPeakSpeed() : St;
        BigDecimal Re = perf.getErrorCharRatio() != null ? perf.getErrorCharRatio() : zero;
        int K = perf.getCurrentRound() != null && perf.getCurrentRound() > 0 ? perf.getCurrentRound() : 1;

        // 标准化实时速度 St / Smax
        BigDecimal standardRealSpeed = constant.getMaxSpeed() != null && constant.getMaxSpeed().compareTo(zero) > 0
                ? St.divide(constant.getMaxSpeed(), SCALE, ROUND_MODE).min(BigDecimal.ONE).max(zero)
                : zero;

        // 标准化历史峰值速度 Shmax / Sref
        BigDecimal standardHistoryPeakSpeed = constant.getRefPeakSpeed() != null && constant.getRefPeakSpeed().compareTo(zero) > 0
                ? Shmax.divide(constant.getRefPeakSpeed(), SCALE, ROUND_MODE).min(BigDecimal.ONE).max(zero)
                : zero;

        // 轮次权重项 WK·log2(K)
        BigDecimal roundWeightItem = params.getWeightCurrentRound()
                .multiply(new BigDecimal(Math.log(K) / Math.log(2)))
                .max(zero);

        // 加权求和项
        BigDecimal weightedSum = params.getWeightRealAccuracy().multiply(At)
                .add(params.getWeightRealSpeed().multiply(standardRealSpeed))
                .add(params.getWeightCharErrorRate().multiply(Eit))
                .add(params.getWeightHistoryAccuracy().multiply(Ah))
                .add(params.getWeightHistoryPeakSpeed().multiply(standardHistoryPeakSpeed))
                .add(params.getWeightErrorCharRatio().multiply(Re))
                .add(roundWeightItem);

        BigDecimal alpha = params.getSmoothAdjustCoeff();
        BigDecimal base = (lastDifficulty != null ? lastDifficulty : constant.getInitialDifficulty());
        BigDecimal result = alpha.multiply(base)
                .add(BigDecimal.ONE.subtract(alpha).multiply(weightedSum))
                .setScale(SCALE, ROUND_MODE);

        log.info("DifficultyCalc: userId=?, mode=?, lastD={}, At={}, St={}, Eit={}, Ah={}, Shmax={}, Re={}, K={}, "
                        + "stdSt={}, stdShmax={}, weightedSum={}, alpha={}, Dt={}",
                null,
                params.getGameMode(),
                lastDifficulty,
                At, St, Eit, Ah, Shmax, Re, K,
                standardRealSpeed, standardHistoryPeakSpeed,
                weightedSum, alpha, result);
        return result;
    }

    /**
     * 兼容旧版：基于 GameRecord 的 Dt 计算，内部将 GameRecord 映射为 GamePlayerPerformanceDTO。
     */
    public static BigDecimal calculateCurrentDifficulty(BigDecimal lastDifficulty,
                                                        GameRecord performance,
                                                        ModelConstant constant,
                                                        UserModelParams params) {
        if (performance == null) {
            return calculateCurrentDifficulty(lastDifficulty, (GamePlayerPerformanceDTO) null, constant, params);
        }
        GamePlayerPerformanceDTO perf = new GamePlayerPerformanceDTO();
        // GameRecord 中 avgAccuracy/peakAccuracy 按“百分数”存储（如 85 表示 85%），
        // 此处需统一换算为 0-1 的小数，才能正确参与难度计算。
        BigDecimal hundred = new BigDecimal("100");
        if (performance.getAvgAccuracy() != null) {
            BigDecimal acc = performance.getAvgAccuracy().divide(hundred, SCALE, ROUND_MODE);
            perf.setRealAccuracy(acc);
            perf.setHistoryAvgAccuracy(acc);
        }
        perf.setRealSpeed(performance.getAvgSpeed());
        perf.setHistoryPeakSpeed(performance.getPeakSpeed());
        perf.setCurrentRound(performance.getRoundNum());
        return calculateCurrentDifficulty(lastDifficulty, perf, constant, params);
    }
}