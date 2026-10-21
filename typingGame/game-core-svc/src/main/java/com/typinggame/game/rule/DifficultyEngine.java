package com.typinggame.game.rule;

public final class DifficultyEngine {
    private DifficultyEngine() {
    }

    public static double nextDifficulty(DifficultyRule rule, double previous, Double accuracy, Double speed, int roundNum, int weakCharCount) {
        double safePrevious = clamp(previous, rule.getMinDifficulty(), rule.getMaxDifficulty());
        double normalizedAccuracy = normalizeAccuracy(accuracy, rule.getTargetAccuracy());
        double safeSpeed = Math.max(0, speed == null ? 0 : speed);
        double normalizedSpeed = Math.min(1.0, safeSpeed / Math.max(1.0, rule.getMaxReferenceSpeed()));
        double historySpeed = Math.min(1.0, safeSpeed / Math.max(1.0, rule.getRefPeakSpeed()));
        double weakRatio = Math.min(1.0, Math.max(0.0, weakCharCount / Math.max(1.0, rule.getWeakCharDivisor())));
        int safeRound = Math.max(1, roundNum);
        double roundTerm = Math.min(1.0, Math.log(safeRound + 1) / Math.log(Math.max(2, rule.getRoundLogBase())));
        double performance = rule.getWeightAccuracy() * normalizedAccuracy
                + rule.getWeightNormalizedSpeed() * normalizedSpeed
                + rule.getWeightHistorySpeed() * historySpeed
                + rule.getWeightComposure() * (1.0 - weakRatio)
                + rule.getWeightRound() * roundTerm;
        double raw = safePrevious + rule.getGain() * (performance - rule.getTargetAccuracy());
        double stepped = safePrevious + clamp(raw - safePrevious, -rule.getStep(), rule.getStep());
        return clamp(stepped, rule.getMinDifficulty(), rule.getMaxDifficulty());
    }

    public static Outcome outcome(DifficultyRule rule, double difficulty, double minSpeed, double maxSpeed, int minLength, int maxLength) {
        double safe = clamp(difficulty, rule.getMinDifficulty(), rule.getMaxDifficulty());
        int pressure = (int) Math.round(safe * 100);
        int normal = (int) clamp(rule.getNormalBase() - pressure * rule.getNormalSlope(), rule.getNormalMin(), rule.getNormalMax());
        int attack = (int) clamp(rule.getAttackBase() + pressure * rule.getAttackSlope(), rule.getAttackMin(), rule.getAttackMax());
        int evasive = (int) clamp(rule.getEvasiveBase() + pressure * rule.getEvasiveSlope(), rule.getEvasiveMin(), rule.getEvasiveMax());
        int smart = Math.max(0, 100 - normal - attack - evasive);
        int length = (int) clamp(Math.round(minLength + safe * (maxLength - minLength)), minLength, maxLength);
        return new Outcome(safe, clamp(minSpeed + safe * rule.getSpeedSpan(), minSpeed, maxSpeed), length, normal, attack, evasive, smart);
    }

    public static int monsterCount(DifficultyRule rule, double difficulty, int roundNum) {
        int safeRound = Math.max(1, roundNum);
        return (int) clamp(Math.round(rule.getCountBase() + safeRound * rule.getCountRoundFactor() + difficulty * rule.getCountDifficultyFactor()),
                rule.getCountMin(), rule.getCountMax());
    }

    public static double normalizeAccuracy(Double accuracy, double fallback) {
        if (accuracy == null) return fallback;
        double value = accuracy > 1 ? accuracy / 100.0 : accuracy;
        return clamp(value, 0, 1);
    }

    public static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    public record Outcome(double difficulty, double speed, int length, int normalRatio, int attackRatio, int evasiveRatio, int smartRatio) {
    }
}
