package com.typinggame.util;

import com.typinggame.entity.game.GameRecord;
import com.typinggame.entity.model.ModelConstant;
import com.typinggame.entity.model.UserModelParams;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 游戏参数调节工具（实现各维度调节公式）
 * 对应文档：核心调节算法模型-参数调节公式
 */
@Slf4j
public class ParamAdjustUtil {
    private static final int SCALE = 4;
    private static final RoundingMode ROUND_MODE = RoundingMode.HALF_UP;

    /**
     * 调节怪物速度
     * Vt = α·Vt-1 + 0.1             At >= 0.85 且 St >= Shmax * 0.8
     *      α·Vt-1 - 0.1             At <= 0.4 或 St <= 20
     *      α·Vt-1 + (Dt - 0.5) * 0.2  其他情况
     * 约束: Vt ∈ [0.8, 3.0]
     */
    public static BigDecimal adjustMonsterSpeed(BigDecimal lastSpeed,
                                                GameRecord performance,
                                                BigDecimal currentDifficulty,
                                                BigDecimal lastDifficulty, // 保留参数但本逻辑暂未使用
                                                ModelConstant constant,
                                                UserModelParams params) {
        BigDecimal alpha = params.getSmoothAdjustCoeff(); // 使用平滑系数alpha
        BigDecimal newSpeed;

        // 阈值常量（可配置化）
        BigDecimal AT_HIGH = new BigDecimal("0.85");
        BigDecimal AT_LOW = new BigDecimal("0.4");
        BigDecimal ST_LOW = new BigDecimal("20");

        // 历史峰值速度 * 0.8
        BigDecimal peakSpeedThreshold = performance.getPeakSpeed().multiply(new BigDecimal("0.8"));

        // 规则1：At >= 0.85 且 St >= Shmax * 0.8 -> 加速
        if (performance.getAvgAccuracy().compareTo(AT_HIGH) >= 0 &&
            performance.getAvgSpeed().compareTo(peakSpeedThreshold) >= 0) {
            newSpeed = alpha.multiply(lastSpeed).add(new BigDecimal("0.1"));
        }
        // 规则2：At <= 0.4 或 St <= 20 -> 减速
        else if (performance.getAvgAccuracy().compareTo(AT_LOW) <= 0 ||
                 performance.getAvgSpeed().compareTo(ST_LOW) <= 0) {
            newSpeed = alpha.multiply(lastSpeed).subtract(new BigDecimal("0.1"));
        }
        // 规则3：其他情况
        else {
            newSpeed = alpha.multiply(lastSpeed).add(
                    currentDifficulty.subtract(new BigDecimal("0.5")).multiply(new BigDecimal("0.2"))
            );
        }

        // 边界约束 [0.8, 3.0]
        BigDecimal bounded = newSpeed.setScale(SCALE, ROUND_MODE)
                .max(new BigDecimal("0.8"))
                .min(new BigDecimal("3.0"));
        log.info("ParamAdjust.speed: userId={}, mode={}, lastV={}, Dt={}, At={}, St={}, peakSpeed={}, Vt={}",
                performance.getUserId(),
                params.getGameMode(),
                lastSpeed,
                currentDifficulty,
                performance.getAvgAccuracy(),
                performance.getAvgSpeed(),
                performance.getPeakSpeed(),
                bounded);
        return bounded;
    }

    /**
     * 调节怪物生成数量（基于难度系数分段）
     * Nt = min(25, α·Nt-1 + 1)   K <= 10 且 St >= 40
     *      min(25, α·Nt-1 + 2)   St >= 120 且 At >= 0.7
     *      max(3, α·Nt-1)        At <= 0.5 或 St <= 30
     *      α·Nt-1 + floor(Dt * 2) 其他情况
     */
    public static int adjustMonsterCount(int lastCount, 
                                         BigDecimal currentDifficulty,
                                         GameRecord performance,
                                         UserModelParams params) {
        BigDecimal alpha = params.getSmoothAdjustCoeff();
        int newCount;
        BigDecimal lastCountBD = new BigDecimal(lastCount);
        
        // 规则1：K <= 10 且 St >= 40
        if (performance.getRoundNum() <= 10 && performance.getAvgSpeed().compareTo(new BigDecimal("40")) >= 0) {
             newCount = alpha.multiply(lastCountBD).add(BigDecimal.ONE).intValue();
        } 
        // 规则2：St >= 120 且 At >= 0.7
        else if (performance.getAvgSpeed().compareTo(new BigDecimal("120")) >= 0 && performance.getAvgAccuracy().compareTo(new BigDecimal("0.7")) >= 0) {
             newCount = alpha.multiply(lastCountBD).add(new BigDecimal("2")).intValue();
        }
        // 规则3：At <= 0.5 或 St <= 30
        else if (performance.getAvgAccuracy().compareTo(new BigDecimal("0.5")) <= 0 || performance.getAvgSpeed().compareTo(new BigDecimal("30")) <= 0) {
             newCount = alpha.multiply(lastCountBD).intValue();
        }
        // 规则4：其他情况
        else {
             newCount = alpha.multiply(lastCountBD).add(
                     new BigDecimal(Math.floor(currentDifficulty.multiply(new BigDecimal("2")).doubleValue()))
             ).intValue();
        }

        // 边界约束 [3, 25]
        int bounded = Math.max(3, Math.min(25, newCount));
        log.info("ParamAdjust.count: userId={}, mode={}, lastN={}, Dt={}, At={}, St={}, round={}, Nt={}",
                performance.getUserId(),
                params.getGameMode(),
                lastCount,
                currentDifficulty,
                performance.getAvgAccuracy(),
                performance.getAvgSpeed(),
                performance.getRoundNum(),
                bounded);
        return bounded;
    }

    /**
     * 调节字符串平均长度
     * Lt = max(1, α·Lt-1 - 1)      At <= 0.5 且 Eit >= 0.3
     *      min(15, α·Lt-1 + 1)     At >= 0.8 且 St >= 80
     *      α·Lt-1 + floor((Dt - 0.4) * 3) 其他情况
     */
    public static int adjustAvgStringLength(int lastLength,
                                            BigDecimal currentDifficulty,
                                            UserModelParams params,
                                            GameRecord performance) {
        BigDecimal alpha = params.getSmoothAdjustCoeff();
        int newLength;
        BigDecimal lastLengthBD = new BigDecimal(lastLength);

        // 规则1：At <= 0.5 且 Eit >= 0.3（此处暂不显式引入 Eit）
        if (performance.getAvgAccuracy().compareTo(new BigDecimal("0.5")) <= 0) {
            newLength = alpha.multiply(lastLengthBD).subtract(BigDecimal.ONE).intValue();
        }
        // 规则2：At >= 0.8 且 St >= 80
        else if (performance.getAvgAccuracy().compareTo(new BigDecimal("0.8")) >= 0
                && performance.getAvgSpeed().compareTo(new BigDecimal("80")) >= 0) {
            newLength = alpha.multiply(lastLengthBD).add(BigDecimal.ONE).intValue();
        }
        // 规则3：其他情况
        else {
            newLength = alpha.multiply(lastLengthBD).add(
                    new BigDecimal(Math.floor(
                            currentDifficulty.subtract(new BigDecimal("0.4"))
                                    .multiply(new BigDecimal("3"))
                                    .doubleValue()))
            ).intValue();
        }

        // 边界约束 [1, 15]
        int bounded = Math.max(1, Math.min(15, newLength));
        log.info("ParamAdjust.length: userId={}, mode={}, lastL={}, Dt={}, At={}, St={}, Lt={}",
                performance.getUserId(),
                params.getGameMode(),
                lastLength,
                currentDifficulty,
                performance.getAvgAccuracy(),
                performance.getAvgSpeed(),
                bounded);
        return bounded;
    }

    /**
     * 调节怪物类型占比，输出 NORMAL / ATTACKER / EVASIVE / SMART 四类占比（百分比，总和为 100）。
     * 使用 D_t 与错误字符占比 R_e 影响进阶类型占比，并在各自合法区间内调整。
     */
    public static int[] adjustMonsterTypeRatio(BigDecimal currentDifficulty,
                                               UserModelParams params,
                                               BigDecimal errorCharRatio,
                                               Integer lastNormal,
                                               Integer lastAttack,
                                               Integer lastEvasive,
                                               Integer lastSmart) {
        final int NORMAL_MIN = 30, NORMAL_MAX = 80;
        final int ATTACK_MIN = 0, ATTACK_MAX = 20;
        final int EVASIVE_MIN = 5, EVASIVE_MAX = 25;
        final int SMART_MIN = 5, SMART_MAX = 25;

        int normal = lastNormal != null && lastNormal > 0 ? lastNormal : 60;
        int attack = lastAttack != null && lastAttack >= 0 ? lastAttack : 10;
        int evasive = lastEvasive != null && lastEvasive >= 0 ? lastEvasive : 15;
        int smart = lastSmart != null && lastSmart >= 0 ? lastSmart : 15;

        BigDecimal weightErrorCharRatio = params != null && params.getWeightErrorCharRatio() != null
                ? params.getWeightErrorCharRatio() : BigDecimal.ONE;
        BigDecimal ratio = errorCharRatio != null ? errorCharRatio : BigDecimal.ZERO;
        BigDecimal intensity = currentDifficulty != null ? currentDifficulty : BigDecimal.ZERO;
        intensity = intensity.multiply(weightErrorCharRatio).multiply(ratio);
        if (intensity.compareTo(BigDecimal.ZERO) < 0) {
            intensity = BigDecimal.ZERO;
        }
        BigDecimal maxIntensity = new BigDecimal("0.6");
        if (intensity.compareTo(maxIntensity) > 0) {
            intensity = maxIntensity;
        }

        int extra = intensity.multiply(new BigDecimal("40"))
                .setScale(0, RoundingMode.HALF_UP)
                .intValue();

        normal = Math.max(NORMAL_MIN, normal - extra);
        if (extra > 0) {
            int toAttack = (int) Math.round(extra * 0.4);
            int toEvasive = (int) Math.round(extra * 0.3);
            int toSmart = extra - toAttack - toEvasive;
            attack += toAttack;
            evasive += toEvasive;
            smart += toSmart;
        }

        normal = Math.min(Math.max(normal, NORMAL_MIN), NORMAL_MAX);
        attack = Math.min(Math.max(attack, ATTACK_MIN), ATTACK_MAX);
        evasive = Math.min(Math.max(evasive, EVASIVE_MIN), EVASIVE_MAX);
        smart = Math.min(Math.max(smart, SMART_MIN), SMART_MAX);

        int sum = normal + attack + evasive + smart;
        if (sum != 100) {
            int diff = 100 - sum;
            int adjustedNormal = normal + diff;
            if (adjustedNormal < NORMAL_MIN) {
                adjustedNormal = NORMAL_MIN;
            } else if (adjustedNormal > NORMAL_MAX) {
                adjustedNormal = NORMAL_MAX;
            }
            normal = adjustedNormal;

            sum = normal + attack + evasive + smart;
            if (sum != 100) {
                int remainingDiff = 100 - sum;
                int step = remainingDiff / 3;
                attack = Math.min(Math.max(attack + step, ATTACK_MIN), ATTACK_MAX);
                evasive = Math.min(Math.max(evasive + step, EVASIVE_MIN), EVASIVE_MAX);
                smart = Math.min(Math.max(smart + (remainingDiff - 2 * step), SMART_MIN), SMART_MAX);
            }
        }

        int[] result = new int[]{normal, attack, evasive, smart};
        log.info("ParamAdjust.typeRatio: userId={}, mode={}, Dt={}, Re={}, last=[{},{},{},{}], result=[{},{},{},{}]",
                null,
                params != null ? params.getGameMode() : null,
                currentDifficulty,
                errorCharRatio,
                lastNormal, lastAttack, lastEvasive, lastSmart,
                result[0], result[1], result[2], result[3]);
        return result;
    }
}
