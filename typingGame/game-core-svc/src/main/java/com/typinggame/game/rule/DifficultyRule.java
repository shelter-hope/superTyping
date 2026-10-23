package com.typinggame.game.rule;

import lombok.Data;

@Data
public class DifficultyRule {
    /** 调节器权重，与速度、历史峰值权重之和为 1。默认只跟踪准确率。 */
    private double weightAccuracy = 1.0;
    /** 当前速度相对参考速度的偏差权重。默认 0，避免把速度误差换算成准确率设定点偏移。 */
    private double weightNormalizedSpeed = 0.0;
    /** 历史峰值相对同一参考速度的偏差权重。缺峰值时该项为 0。 */
    private double weightHistorySpeed = 0.0;
    /** 易错字符抽样强化系数，范围 [0,1]。倍率 = 1 + 系数 × 弱项比例，不进入难度积分。 */
    private double weightComposure = 1.0;
    /** 保留字段。轮次不再进入难度积分，数量公式使用 countRoundFactor。 */
    private double weightRound = 0.0;
    private double targetAccuracy = 0.85;
    private double gain = 0.35;
    private double step = 0.05;
    private double minDifficulty = 0.15;
    private double maxDifficulty = 0.95;
    private double initialDifficulty = 0.30;
    private double maxReferenceSpeed = 150;
    private double refPeakSpeed = 120;
    private double weakCharDivisor = 10;
    /** 怪物速度覆盖速度上下界的比例，大于 1 时按满覆盖处理。 */
    private double speedSpan = 1.0;
    private double countBase = 3;
    private double countRoundFactor = 6;
    private double countDifficultyFactor = 16;
    private int countMin = 3;
    private int countMax = 25;
    private int normalBase = 80;
    private double normalSlope = 0.45;
    private int normalMin = 35;
    private int normalMax = 80;
    private int attackBase = 5;
    private double attackSlope = 0.15;
    private int attackMin = 5;
    private int attackMax = 20;
    private int evasiveBase = 8;
    private double evasiveSlope = 0.12;
    private int evasiveMin = 5;
    private int evasiveMax = 25;
    private int roundLogBase = 32;
    /** 准确率百分比阈值：达到则判定为高准确率推进（与历史 trigger 语义一致） */
    private double highAccuracyAdvanceThreshold = 85;
    /** 准确率百分比阈值：不高于则判定为低准确率回退 */
    private double lowAccuracyRollbackThreshold = 50;
}
