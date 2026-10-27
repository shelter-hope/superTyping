package com.typinggame.game.rule;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Set;

public final class DifficultyEngine {
    private DifficultyEngine() {
    }

    /**
     * 能力感知的对数几率调节器（v2）。
     *
     * <p>把准确率偏差换算到对数几率空间：e = ln(p ÷ (1-p)) − ln(p* ÷ (1 − p*))。
     * 在 logistic 响应模型 p = σ(k(θ − D)) 下，玩家达到目标准确率所需的难度是
     * D* = D + e / k；每轮只朝 D* 走 λ（上调慢、下调快），再用 step 封顶，最后夹在 [min,max]。</p>
     *
     * <p>准确率落在 deadband 内时准确率项计 0，避免单轮噪声来回抖。
     * 速度、峰值速度是可选的副信号，按 weight×speedErrorScale 折算到同一量级；
     * 权重为 0 时它们完全不参与，因此「只看准确率」和「只看速度」的配置都能工作。</p>
     *
     * <p>与 v1 的区别：v1 是线性的有界积分器，均衡点由权重和目标隐式决定，
     * 且任何非零误差都会推动难度，长期在目标附近抖动；v2 在目标附近有死区，
     * 并让调整幅度随误差呈对数增长，离目标越远一步走得越多、越近越稳。</p>
     */
    public static double nextDifficulty(DifficultyRule rule, double previous, Double accuracy, Double speed, int roundNum, int weakCharCount) {
        return nextDifficulty(rule, previous, accuracy, speed, null, roundNum, weakCharCount);
    }

    public static double nextDifficulty(DifficultyRule rule, double previous, Double accuracy, Double speed, Double peakSpeed, int roundNum, int weakCharCount) {
        double safePrevious = clamp(previous, rule.getMinDifficulty(), rule.getMaxDifficulty());
        if (accuracy == null) {
            return safePrevious;
        }
        double target = accuracyBound(rule.getTargetAccuracy());
        double observed = accuracyBound(normalizeAccuracy(accuracy, target));
        double accuracyError = 0.0;
        if (rule.getWeightAccuracy() != 0 && Math.abs(observed - target) > Math.max(0.0, rule.getDeadband())) {
            accuracyError = rule.getWeightAccuracy() * (logit(observed) - logit(target));
        }
        double error = accuracyError
                + speedError(rule, speed, rule.getWeightNormalizedSpeed())
                + speedError(rule, peakSpeed, rule.getWeightHistorySpeed());
        if (Math.abs(error) < 1e-9) {
            return safePrevious;
        }
        double lambda = error > 0 ? clamp(rule.getLambdaUp(), 0, 1) : clamp(rule.getLambdaDown(), 0, 1);
        double move = lambda * error / Math.max(0.5, rule.getLogitSlope());
        double step = Math.max(0.0, rule.getStep());
        return clamp(safePrevious + clamp(move, -step, step), rule.getMinDifficulty(), rule.getMaxDifficulty());
    }

    /**
     * 从当前难度反推玩家能力 θ：p* = σ(k(θ − D*))，故 θ = D + ln(p* ÷ (1 − p*)) / k。
     * 只用于观测与后台展示，不参与积分。
     */
    public static double thetaAccuracy(DifficultyRule rule, double difficulty) {
        return difficulty + logit(accuracyBound(rule.getTargetAccuracy())) / Math.max(0.5, rule.getLogitSlope());
    }

    /** 本轮观测反推的目标难度 D* = D + logit 误差 / k；准确率缺失时返回原难度。 */
    public static double targetDifficulty(DifficultyRule rule, double difficulty, Double accuracy) {
        double safe = clamp(difficulty, rule.getMinDifficulty(), rule.getMaxDifficulty());
        if (accuracy == null) {
            return safe;
        }
        double target = accuracyBound(rule.getTargetAccuracy());
        double observed = accuracyBound(normalizeAccuracy(accuracy, target));
        return safe + (logit(observed) - logit(target)) / Math.max(0.5, rule.getLogitSlope());
    }

    /** logistic 响应模型：给定能力 θ 与难度 D 预测准确率，供管理台模拟器使用。 */
    public static double predictedAccuracy(DifficultyRule rule, double theta, double difficulty) {
        return logistic(Math.max(0.5, rule.getLogitSlope()) * (theta - difficulty));
    }

    public static double logit(double accuracy) {
        double safe = clamp(accuracy, 1e-4, 1 - 1e-4);
        return Math.log(safe / (1 - safe));
    }

    public static double logistic(double value) {
        return 1.0 / (1.0 + Math.exp(-value));
    }

    public static Outcome outcome(DifficultyRule rule, double difficulty, double minSpeed, double maxSpeed, int minLength, int maxLength) {
        double safe = clamp(difficulty, rule.getMinDifficulty(), rule.getMaxDifficulty());
        double unit = unitDifficulty(rule, safe);
        double pressure = unit * 100.0;
        int normal = (int) Math.round(clamp(rule.getNormalBase() - pressure * rule.getNormalSlope(), rule.getNormalMin(), rule.getNormalMax()));
        int attack = (int) Math.round(clamp(rule.getAttackBase() + pressure * rule.getAttackSlope(), rule.getAttackMin(), rule.getAttackMax()));
        int evasive = (int) Math.round(clamp(rule.getEvasiveBase() + pressure * rule.getEvasiveSlope(), rule.getEvasiveMin(), rule.getEvasiveMax()));
        int smart = 100 - normal - attack - evasive;
        if (smart < 0) {
            int overflow = -smart;
            int cut = Math.min(overflow, Math.max(0, normal - rule.getNormalMin()));
            normal -= cut;
            overflow -= cut;
            cut = Math.min(overflow, Math.max(0, attack - rule.getAttackMin()));
            attack -= cut;
            overflow -= cut;
            cut = Math.min(overflow, Math.max(0, evasive - rule.getEvasiveMin()));
            evasive -= cut;
            smart = 100 - normal - attack - evasive;
            if (smart < 0) {
                smart = 0;
            }
        }
        int length = (int) clamp(Math.round(minLength + unit * (maxLength - minLength)), minLength, maxLength);
        double cover = speedCover(rule);
        double speed = minSpeed + cover * unit * Math.max(0, maxSpeed - minSpeed);
        return new Outcome(safe, clamp(speed, minSpeed, maxSpeed), length, normal, attack, evasive, smart);
    }

    public static int monsterCount(DifficultyRule rule, double difficulty, int roundNum) {
        double unit = unitDifficulty(rule, difficulty);
        double roundTerm = roundProgress(rule, roundNum);
        double raw = rule.getCountBase() + rule.getCountDifficultyFactor() * unit + rule.getCountRoundFactor() * unit * roundTerm;
        return (int) clamp(Math.round(raw), rule.getCountMin(), rule.getCountMax());
    }

    /**
     * 易错字符抽样倍率。无易错字符时为 1；弱项比例为 1 且强化系数为 1 时为 2。
     * 强化系数取自 weakExposureGain，与难度积分完全解耦（v1 曾借用 weightComposure）。
     */
    public static double weakExposureMultiplier(DifficultyRule rule, int weakCharCount) {
        double ratio = Math.min(1.0, Math.max(0.0, weakCharCount / Math.max(1.0, rule.getWeakCharDivisor())));
        double gain = clamp(rule.getWeakExposureGain(), 0, 1);
        return 1.0 + gain * ratio;
    }

    public static int exposureWeight(String text, List<String> focusChars, int baseWeight, double multiplier) {
        int safe = Math.max(1, baseWeight);
        if (multiplier <= 1.0 || text == null || focusChars == null) {
            return safe;
        }
        for (String focus : focusChars) {
            if (focus != null && !focus.isBlank() && text.contains(focus)) {
                return Math.max(1, (int) Math.round(safe * multiplier));
            }
        }
        return safe;
    }

    public static String pickWeighted(List<String> pool, Set<String> boosted, double multiplier, Random random) {
        if (pool == null || pool.isEmpty()) {
            return "";
        }
        double boost = Math.max(1.0, multiplier);
        List<String> focus = boosted == null ? List.of() : List.copyOf(boosted);
        double[] weights = new double[pool.size()];
        for (int i = 0; i < pool.size(); i++) {
            weights[i] = containsFocus(pool.get(i), focus) ? boost : 1.0;
        }
        return pool.get(pickIndex(weights, random.nextDouble()));
    }

    private static boolean containsFocus(String text, List<String> focusChars) {
        if (text == null || focusChars == null) {
            return false;
        }
        for (String focus : focusChars) {
            if (focus != null && !focus.isBlank() && text.contains(focus)) {
                return true;
            }
        }
        return false;
    }

    public static int pickIndex(double[] weights, double unit) {
        double total = 0;
        for (double weight : weights) {
            total += Math.max(0, weight);
        }
        if (total <= 0) {
            return 0;
        }
        double aim = clamp(unit, 0, 0.999999) * total;
        double cursor = 0;
        for (int i = 0; i < weights.length; i++) {
            cursor += Math.max(0, weights[i]);
            if (aim < cursor) {
                return i;
            }
        }
        return weights.length - 1;
    }

    public static double roundProgress(DifficultyRule rule, int roundNum) {
        int safeRound = Math.max(1, roundNum);
        return Math.min(1.0, Math.log(safeRound + 1) / Math.log(Math.max(2, rule.getRoundLogBase())));
    }

    public static double normalizeAccuracy(Double accuracy, double fallback) {
        if (accuracy == null) return fallback;
        double value = accuracy > 1 ? accuracy / 100.0 : accuracy;
        return clamp(value, 0, 1);
    }

    public static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private static double speedSetpoint(DifficultyRule rule) {
        return Math.min(1.0, Math.max(0, rule.getRefPeakSpeed()) / Math.max(1.0, rule.getMaxReferenceSpeed()));
    }

    /** 把速度偏差折算成对数几率误差：权重要么为 0（不参与），要么按 speedErrorScale 放大。 */
    private static double speedError(DifficultyRule rule, Double speed, double weight) {
        if (speed == null || weight == 0) {
            return 0.0;
        }
        double scale = Math.max(0.0, rule.getSpeedErrorScale());
        return weight * scale * (normalizeSpeed(speed, rule.getMaxReferenceSpeed()) - speedSetpoint(rule));
    }

    /** logistic 里 p 取对数前先夹到 (0,1) 内部，避免 0%/100% 把误差推到无穷。 */
    private static double accuracyBound(double accuracy) {
        return clamp(accuracy, 0.02, 0.98);
    }

    private static double normalizeSpeed(double speed, double reference) {
        return Math.min(1.0, Math.max(0, speed) / Math.max(1.0, reference));
    }

    private static double unitDifficulty(DifficultyRule rule, double difficulty) {
        double span = rule.getMaxDifficulty() - rule.getMinDifficulty();
        if (span <= 1e-9) {
            return 0;
        }
        return clamp((difficulty - rule.getMinDifficulty()) / span, 0, 1);
    }

    private static double speedCover(DifficultyRule rule) {
        double span = rule.getSpeedSpan();
        if (span > 1.0) {
            return 1.0;
        }
        return clamp(span, 0, 1);
    }

    /**
     * 离线仿真：给定每轮的准确率/速度序列，用同一套规则推演难度与输出配置。
     *
     * <p>只服务管理端的「仿真预览」：不读写数据库、不落库、不影响真实对局。
     * 序列比轮数短时沿用最后一项，为空时用规则自身的默认值（目标准确率、速度 0）。</p>
     */
    public static List<SimulationPoint> simulate(DifficultyRule rule, List<Double> accuracies,
                                                 List<Double> speeds, int rounds) {
        int count = Math.max(1, Math.min(200, rounds));
        double previous = rule.getInitialDifficulty();
        List<SimulationPoint> points = new ArrayList<>(count);
        for (int round = 1; round <= count; round++) {
            Double accuracy = valueAt(accuracies, round - 1, rule.getTargetAccuracy());
            Double speed = valueAt(speeds, round - 1, 0.0);
            previous = nextDifficulty(rule, previous, accuracy, speed, round, 0);
            Outcome outcome = outcome(rule, previous, 0.8, 3.0, 1, 15);
            points.add(new SimulationPoint(round, accuracy, previous, monsterCount(rule, previous, round),
                    outcome.normalRatio(), outcome.attackRatio(), outcome.evasiveRatio(), outcome.smartRatio()));
        }
        return points;
    }

    private static Double valueAt(List<Double> values, int index, double fallback) {
        if (values == null || values.isEmpty()) {
            return fallback;
        }
        Double value = values.get(Math.min(index, values.size() - 1));
        return value != null ? value : fallback;
    }

    public record Outcome(double difficulty, double speed, int length, int normalRatio, int attackRatio, int evasiveRatio, int smartRatio) {
    }

    public record SimulationPoint(int round, double accuracy, double difficulty, int monsterCount,
                                  int normalRatio, int attackRatio, int evasiveRatio, int smartRatio) {
    }
}
