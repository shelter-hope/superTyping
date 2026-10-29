package com.typinggame.game.service.rule;

import lombok.Data;

@Data
public class DifficultyRule {
    /** 调节器权重，与速度、历史峰值权重之和为 1。默认只跟踪准确率。 */
    private double weightAccuracy = 1.0;
    /** 当前速度相对参考速度的偏差权重。默认 0，避免把速度误差换算成准确率设定点偏移。 */
    private double weightNormalizedSpeed = 0.0;
    /** 历史峰值相对同一参考速度的偏差权重。缺峰值时该项为 0。 */
    private double weightHistorySpeed = 0.0;
    /** 旧字段：v2 起不再参与难度积分，弱项抽样改用 weakExposureGain。保留以便旧配置反序列化。 */
    private double weightComposure = 1.0;
    /** 保留字段。轮次不再进入难度积分，数量公式使用 countRoundFactor。 */
    private double weightRound = 0.0;
    private double targetAccuracy = 0.85;
    /** 旧字段：v1 积分器增益。v2 用 lambdaUp / lambdaDown，保留以便旧配置反序列化。 */
    private double gain = 0.35;
    private double step = 0.05;
    /** logistic 斜率 k：难度每变化 1，准确率的对数几率变化 k。越大越"陡"。 */
    private double logitSlope = 6.0;
    /** 准确率死区：|准确率 − 目标| 不超过该值时不动难度，避免单轮噪声来回抖。 */
    private double deadband = 0.03;
    /** 上调阻尼：准确率高于目标时，每轮走完误差的多少（慢）。 */
    private double lambdaUp = 0.25;
    /** 下调阻尼：准确率低于目标时，每轮走完误差的多少（快）。 */
    private double lambdaDown = 0.5;
    /** 速度偏差折算系数：把归一化速度偏差放大到 logit 误差同一量级。 */
    private double speedErrorScale = 2.0;
    /** 易错字符抽样强化系数，范围 [0,1]。倍率 = 1 + 系数 × 弱项比例，不进入难度积分。 */
    private double weakExposureGain = 1.0;
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
