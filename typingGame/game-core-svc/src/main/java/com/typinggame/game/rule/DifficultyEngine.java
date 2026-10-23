package com.typinggame.game.rule;

import java.util.List;
import java.util.Random;
import java.util.Set;

public final class DifficultyEngine {
    private DifficultyEngine() {
    }

    /**
     * 饱和积分器。误差只含相对设定点的偏差，默认权重下就是准确率偏差。
     * 轮次和易错字符不进入误差。准确率缺失时保持难度。
     * roundNum、weakCharCount 保留给调用方，不参与本步更新。
     */
    public static double nextDifficulty(DifficultyRule rule, double previous, Double accuracy, Double speed, int roundNum, int weakCharCount) {
        return nextDifficulty(rule, previous, accuracy, speed, null, roundNum, weakCharCount);
    }

    public static double nextDifficulty(DifficultyRule rule, double previous, Double accuracy, Double speed, Double peakSpeed, int roundNum, int weakCharCount) {
        double safePrevious = clamp(previous, rule.getMinDifficulty(), rule.getMaxDifficulty());
        if (accuracy == null) {
            return safePrevious;
        }
        double normalizedAccuracy = normalizeAccuracy(accuracy, 0);
        double error = rule.getWeightAccuracy() * (normalizedAccuracy - rule.getTargetAccuracy());
        double speedSetpoint = speedSetpoint(rule);
        if (speed != null && rule.getWeightNormalizedSpeed() != 0) {
            error += rule.getWeightNormalizedSpeed() * (normalizeSpeed(speed, rule.getMaxReferenceSpeed()) - speedSetpoint);
        }
        if (peakSpeed != null && rule.getWeightHistorySpeed() != 0) {
            error += rule.getWeightHistorySpeed() * (normalizeSpeed(peakSpeed, rule.getMaxReferenceSpeed()) - speedSetpoint);
        }
        double raw = safePrevious + rule.getGain() * error;
        double stepped = safePrevious + clamp(raw - safePrevious, -rule.getStep(), rule.getStep());
        return clamp(stepped, rule.getMinDifficulty(), rule.getMaxDifficulty());
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
     */
    public static double weakExposureMultiplier(DifficultyRule rule, int weakCharCount) {
        double ratio = Math.min(1.0, Math.max(0.0, weakCharCount / Math.max(1.0, rule.getWeakCharDivisor())));
        double gain = clamp(rule.getWeightComposure(), 0, 1);
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

    public record Outcome(double difficulty, double speed, int length, int normalRatio, int attackRatio, int evasiveRatio, int smartRatio) {
    }
}
