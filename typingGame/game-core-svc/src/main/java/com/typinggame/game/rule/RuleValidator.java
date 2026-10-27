package com.typinggame.game.rule;

public final class RuleValidator {
    private RuleValidator() {
    }

    public static void validateDifficulty(DifficultyRule rule) {
        if (rule == null) throw new IllegalArgumentException("难度规则不能为空");
        double regulator = rule.getWeightAccuracy() + rule.getWeightNormalizedSpeed() + rule.getWeightHistorySpeed();
        if (Math.abs(regulator - 1.0) > 0.001) {
            throw new IllegalArgumentException("准确率、速度、历史峰值权重之和必须为 1，当前为 " + regulator);
        }
        requireRange("易错强化系数", rule.getWeightComposure(), 0, 1);
        requireRange("弱项抽样强化", rule.getWeakExposureGain(), 0, 1);
        requireRange("轮次保留权重", rule.getWeightRound(), 0, 1);
        requireRange("目标准确率", rule.getTargetAccuracy(), 0, 1);
        requireRange("增益（v1 保留字段）", rule.getGain(), 0, 2);
        requireRange("步长", rule.getStep(), 0, 0.5);
        requirePositive("步长", rule.getStep());
        if (rule.getLogitSlope() < 1 || rule.getLogitSlope() > 20) {
            throw new IllegalArgumentException("logit 斜率 k 必须落在 [1, 20]");
        }
        requireRange("准确率死区", rule.getDeadband(), 0, 0.2);
        requireRange("上调阻尼", rule.getLambdaUp(), 0, 1);
        requireRange("下调阻尼", rule.getLambdaDown(), 0, 1);
        requireRange("速度偏差折算", rule.getSpeedErrorScale(), 0, 10);
        if (rule.getMinDifficulty() < 0 || rule.getMaxDifficulty() > 1 || rule.getMinDifficulty() >= rule.getMaxDifficulty()) {
            throw new IllegalArgumentException("难度区间不合法");
        }
        if (rule.getInitialDifficulty() < rule.getMinDifficulty() || rule.getInitialDifficulty() > rule.getMaxDifficulty()) {
            throw new IllegalArgumentException("初始难度超出区间");
        }
        if (rule.getCountMin() < 1 || rule.getCountMax() < rule.getCountMin()) {
            throw new IllegalArgumentException("怪物数量区间不合法");
        }
        requirePositive("最大参考速度", rule.getMaxReferenceSpeed());
        requirePositive("参考峰值速度", rule.getRefPeakSpeed());
        requirePositive("弱项字符除数", rule.getWeakCharDivisor());
        requirePositive("速度跨度", rule.getSpeedSpan());
        requirePositive("轮次对数底", rule.getRoundLogBase());
        requireMinMax("普通型占比", rule.getNormalMin(), rule.getNormalMax(), 0, 100);
        requireMinMax("攻击型占比", rule.getAttackMin(), rule.getAttackMax(), 0, 100);
        requireMinMax("回避型占比", rule.getEvasiveMin(), rule.getEvasiveMax(), 0, 100);
        requireRange("高准确率推进阈值", rule.getHighAccuracyAdvanceThreshold(), 0, 100);
        requireRange("低准确率回退阈值", rule.getLowAccuracyRollbackThreshold(), 0, 100);
        if (rule.getLowAccuracyRollbackThreshold() >= rule.getHighAccuracyAdvanceThreshold()) {
            throw new IllegalArgumentException("低准确率回退阈值必须小于高准确率推进阈值");
        }
    }

    public static void validateGenetic(GeneticRule rule) {
        if (rule == null) throw new IllegalArgumentException("遗传规则不能为空");
        if (rule.getInRoundParentLimit() < 1 || rule.getCrossRoundParentLimit() < 1) {
            throw new IllegalArgumentException("父本上限必须大于 0");
        }
        requireRange("局内变异率", rule.getInRoundMutationRate(), 0, 1);
        requireRange("跨局变异率", rule.getCrossRoundMutationRate(), 0, 1);
        if (rule.getCrossover() == null || rule.getCrossover().isBlank()) {
            throw new IllegalArgumentException("交叉方式不能为空");
        }
    }

    private static void requireRange(String name, double value, double min, double max) {
        if (value < min || value > max) {
            throw new IllegalArgumentException(name + "超出区间 [" + min + ", " + max + "]");
        }
    }

    private static void requirePositive(String name, double value) {
        if (value <= 0) {
            throw new IllegalArgumentException(name + "必须大于 0");
        }
    }

    private static void requireMinMax(String name, int min, int max, int floor, int ceiling) {
        if (min < floor || max > ceiling || min > max) {
            throw new IllegalArgumentException(name + "区间不合法");
        }
    }
}
