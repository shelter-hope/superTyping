package com.typinggame.util;

import com.typinggame.dto.game.GamePlayerPerformanceDTO;
import com.typinggame.entity.model.ModelConstant;
import com.typinggame.entity.model.UserModelParams;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 综合难度系数 Dₜ 计算工具单测
 */
class DifficultyCalculationUtilTest {

    private static UserModelParams defaultParams() {
        UserModelParams p = new UserModelParams();
        p.setWeightRealAccuracy(new BigDecimal("0.35"));
        p.setWeightRealSpeed(new BigDecimal("0.25"));
        p.setWeightCharErrorRate(new BigDecimal("0.20"));
        p.setWeightHistoryAccuracy(new BigDecimal("0.10"));
        p.setWeightHistoryPeakSpeed(new BigDecimal("0.05"));
        p.setWeightErrorCharRatio(new BigDecimal("0.05"));
        p.setWeightCurrentRound(new BigDecimal("0.10"));
        p.setSmoothAdjustCoeff(new BigDecimal("0.80"));
        return p;
    }

    private static ModelConstant defaultConstant() {
        ModelConstant c = new ModelConstant();
        c.setMaxSpeed(new BigDecimal("150"));
        c.setRefPeakSpeed(new BigDecimal("120"));
        c.setInitialDifficulty(new BigDecimal("0.30"));
        return c;
    }

    @Test
    @DisplayName("null 表现时返回 lastDifficulty 或 initialDifficulty")
    void whenPerfOrParamsNull_returnsLastOrInitial() {
        ModelConstant constant = defaultConstant();
        UserModelParams params = defaultParams();
        GamePlayerPerformanceDTO perf = new GamePlayerPerformanceDTO();
        perf.setRealAccuracy(BigDecimal.ONE);
        perf.setRealSpeed(new BigDecimal("60"));
        perf.setCurrentRound(1);

        BigDecimal last = new BigDecimal("0.40");
        BigDecimal got = DifficultyCalculationUtil.calculateCurrentDifficulty(last, (GamePlayerPerformanceDTO) null, constant, params);
        assertEquals(last, got);

        got = DifficultyCalculationUtil.calculateCurrentDifficulty(null, perf, constant, params);
        assertNotNull(got);
        assertTrue(got.compareTo(BigDecimal.ZERO) >= 0);
    }

    @Test
    @DisplayName("高准确率与速度时 Dₜ 偏向较高")
    void whenHighAccuracyAndSpeed_difficultyTendsHigher() {
        ModelConstant constant = defaultConstant();
        UserModelParams params = defaultParams();
        GamePlayerPerformanceDTO perf = new GamePlayerPerformanceDTO();
        perf.setRealAccuracy(new BigDecimal("0.95"));
        perf.setRealSpeed(new BigDecimal("100"));
        perf.setHistoryAvgAccuracy(new BigDecimal("0.90"));
        perf.setHistoryPeakSpeed(new BigDecimal("110"));
        perf.setAvgCharErrorRate(new BigDecimal("0.05"));
        perf.setErrorCharRatio(new BigDecimal("0.10"));
        perf.setCurrentRound(5);

        BigDecimal last = new BigDecimal("0.35");
        BigDecimal dt = DifficultyCalculationUtil.calculateCurrentDifficulty(last, perf, constant, params);
        assertNotNull(dt);
        assertTrue(dt.compareTo(BigDecimal.ZERO) >= 0, "Dₜ >= 0");
        assertTrue(dt.compareTo(BigDecimal.ONE) <= 1, "Dₜ <= 1");
        // 平滑后应比 0.35 有所上升（表现好 -> 难度略升）
        assertTrue(dt.compareTo(last) >= 0, "Dₜ >= lastDifficulty when performance good");
    }

    @Test
    @DisplayName("轮次 K 增大时 roundWeightItem 增大")
    void whenRoundIncreases_roundWeightIncreases() {
        ModelConstant constant = defaultConstant();
        UserModelParams params = defaultParams();
        GamePlayerPerformanceDTO perf = new GamePlayerPerformanceDTO();
        perf.setRealAccuracy(new BigDecimal("0.70"));
        perf.setRealSpeed(new BigDecimal("60"));
        perf.setCurrentRound(1);

        BigDecimal last = new BigDecimal("0.30");
        BigDecimal dt1 = DifficultyCalculationUtil.calculateCurrentDifficulty(last, perf, constant, params);
        perf.setCurrentRound(8);
        BigDecimal dt8 = DifficultyCalculationUtil.calculateCurrentDifficulty(last, perf, constant, params);
        assertTrue(dt8.compareTo(dt1) >= 0, "higher round should not decrease Dₜ when other same");
    }
}
