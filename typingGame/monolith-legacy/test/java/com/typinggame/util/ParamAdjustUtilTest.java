package com.typinggame.util;

import com.typinggame.entity.game.GameRecord;
import com.typinggame.entity.model.ModelConstant;
import com.typinggame.entity.model.UserModelParams;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 参数调节工具单测（Vₜ、Nₜ、类型占比等）
 */
class ParamAdjustUtilTest {

    private static UserModelParams defaultParams() {
        UserModelParams p = new UserModelParams();
        p.setSmoothAdjustCoeff(new BigDecimal("0.80"));
        p.setWeightErrorCharRatio(new BigDecimal("0.05"));
        return p;
    }

    private static GameRecord record(BigDecimal accuracy, BigDecimal speed, int roundNum) {
        GameRecord r = new GameRecord();
        r.setAvgAccuracy(accuracy);
        r.setAvgSpeed(speed);
        r.setPeakSpeed(speed != null ? speed.multiply(new BigDecimal("1.1")) : null);
        r.setRoundNum(roundNum);
        return r;
    }

    private static ModelConstant constant() {
        ModelConstant c = new ModelConstant();
        c.setMaxSpeed(new BigDecimal("150"));
        c.setRefPeakSpeed(new BigDecimal("120"));
        c.setInitialDifficulty(new BigDecimal("0.30"));
        return c;
    }

    @Test
    @DisplayName("高准确率且高速度时走加速分支，结果在 [0.8, 3.0]")
    void whenHighAccuracyAndSpeed_speedIncreases() {
        GameRecord perf = record(new BigDecimal("0.90"), new BigDecimal("100"), 5);
        ModelConstant constant = constant();
        UserModelParams params = defaultParams();
        // 使用较小 lastSpeed 使 alpha*last+0.1 > last，便于断言上升
        BigDecimal lastSpeed = new BigDecimal("0.4");
        BigDecimal v = ParamAdjustUtil.adjustMonsterSpeed(lastSpeed, perf, new BigDecimal("0.5"), new BigDecimal("0.4"), constant, params);
        assertNotNull(v);
        assertTrue(v.compareTo(lastSpeed) >= 0, "speed should not decrease when At>=0.85 and St>=Shmax*0.8");
        assertTrue(v.compareTo(new BigDecimal("0.8")) >= 0);
        assertTrue(v.compareTo(new BigDecimal("3.0")) <= 0);
    }

    @Test
    @DisplayName("低准确率或低速度时速度下调")
    void whenLowAccuracyOrSpeed_speedDecreases() {
        GameRecord perf = record(new BigDecimal("0.35"), new BigDecimal("15"), 3);
        UserModelParams params = defaultParams();
        ModelConstant constant = constant();
        BigDecimal lastSpeed = new BigDecimal("1.5");
        BigDecimal v = ParamAdjustUtil.adjustMonsterSpeed(lastSpeed, perf, new BigDecimal("0.4"), new BigDecimal("0.4"), constant, params);
        assertNotNull(v);
        assertTrue(v.compareTo(lastSpeed) <= 0);
        assertTrue(v.compareTo(new BigDecimal("3.0")) <= 0);
        assertTrue(v.compareTo(new BigDecimal("0.8")) >= 0);
    }

    @Test
    @DisplayName("怪物数量约束在 [3, 25]")
    void monsterCountBoundedBetween3And25() {
        UserModelParams params = defaultParams();
        GameRecord perf = record(new BigDecimal("0.60"), new BigDecimal("50"), 2);
        int n = ParamAdjustUtil.adjustMonsterCount(2, new BigDecimal("0.30"), perf, params);
        assertTrue(n >= 3 && n <= 25, "Nₜ in [3, 25]");
    }

    @Test
    @DisplayName("类型占比四类之和为 100")
    void typeRatioSumIs100() {
        UserModelParams params = defaultParams();
        int[] ratio = ParamAdjustUtil.adjustMonsterTypeRatio(
                new BigDecimal("0.40"), params, new BigDecimal("0.10"),
                60, 10, 15, 15);
        assertNotNull(ratio);
        assertEquals(4, ratio.length);
        int sum = ratio[0] + ratio[1] + ratio[2] + ratio[3];
        assertEquals(100, sum, "NORMAL+ATTACKER+EVASIVE+SMART = 100");
    }
}
