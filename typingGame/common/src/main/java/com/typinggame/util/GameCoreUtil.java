package com.typinggame.util;

import com.typinggame.dto.game.GameAdjustParam;
import com.typinggame.entity.game.GameRecord;
import com.typinggame.entity.game.MonsterData;
import com.typinggame.entity.game.UserGameModeConfig;
import com.typinggame.entity.model.ModelConstant;
import com.typinggame.entity.model.UserModelParams;
import com.typinggame.enums.CharType;
import com.typinggame.enums.MonsterType;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * 游戏核心整合工具（串联所有计算逻辑）
 */
@Slf4j
public class GameCoreUtil {

    /**
     * 初始化游戏参数（首轮游戏），返回一个简单的 MonsterData 模板。
     * 详细首轮配置（数量、长度、类型占比等）由上层基于 UserGameModeConfig 决定。
     */
    public static MonsterData initGameParam(ModelConstant constant, UserGameModeConfig modeConfig) {
        MonsterData initParam = new MonsterData();
        initParam.setSpeed(new BigDecimal("1.0"));
        return initParam;
    }

    /**
     * 迭代更新游戏全局调节参数（非首轮），输出 GameAdjustParam：
     * - currentDifficulty (D_t)
     * - monsterSpeed (V_t)
     * - monsterCount (N_t)
     * - avgStringLength (L_t)
     * - normal/attack/evasive/smart 占比
     *
     * @param lastAdjust      上一轮的调节结果（可为空，表示使用默认初始值）
     * @param performance     本轮 GameRecord 表现数据（A_t、S_t、轮次等）
     * @param constant        模型常量（Smax、Sref、初始难度等）
     * @param params          用户个性化模型参数（权重、平滑系数等）
     * @param errorCharRatio  易错字符占比 R_e（0-1），可由 DifficultyModelService 聚合得到
     * @return 新一轮的调节参数 DTO
     */
    public static GameAdjustParam updateGameParam(GameAdjustParam lastAdjust,
                                                  GameRecord performance,
                                                  ModelConstant constant,
                                                  UserModelParams params,
                                                  BigDecimal errorCharRatio) {
        if (performance == null || constant == null || params == null) {
            return null;
        }

        BigDecimal lastDifficulty = params.getLastDifficulty() != null
                ? params.getLastDifficulty() : constant.getInitialDifficulty();
        BigDecimal currentDifficulty = DifficultyCalculationUtil.calculateCurrentDifficulty(
                lastDifficulty, performance, constant, params);

        BigDecimal lastSpeed = (lastAdjust != null && lastAdjust.getMonsterSpeed() != null)
                ? lastAdjust.getMonsterSpeed()
                : new BigDecimal("1.0");
        Integer lastCount = (lastAdjust != null && lastAdjust.getMonsterCount() != null)
                ? lastAdjust.getMonsterCount()
                : 8;
        Integer lastLength = (lastAdjust != null && lastAdjust.getAvgStringLength() != null)
                ? lastAdjust.getAvgStringLength()
                : 3;
        Integer lastNormal = lastAdjust != null ? lastAdjust.getNormalRatio() : null;
        Integer lastAttack = lastAdjust != null ? lastAdjust.getAttackRatio() : null;
        Integer lastEvasive = lastAdjust != null ? lastAdjust.getEvasiveRatio() : null;
        Integer lastSmart = lastAdjust != null ? lastAdjust.getSmartRatio() : null;

        BigDecimal newSpeed = ParamAdjustUtil.adjustMonsterSpeed(
                lastSpeed, performance, currentDifficulty, lastDifficulty, constant, params);
        int newCount = ParamAdjustUtil.adjustMonsterCount(lastCount, currentDifficulty, performance, params);
        int newLength = ParamAdjustUtil.adjustAvgStringLength(lastLength, currentDifficulty, params, performance);
        int[] typeRatios = ParamAdjustUtil.adjustMonsterTypeRatio(
                currentDifficulty, params, errorCharRatio, lastNormal, lastAttack, lastEvasive, lastSmart);

        GameAdjustParam result = new GameAdjustParam();
        result.setCurrentDifficulty(currentDifficulty);
        result.setMonsterSpeed(newSpeed);
        result.setMonsterCount(newCount);
        result.setAvgStringLength(newLength);
        result.setNormalRatio(typeRatios[0]);
        result.setAttackRatio(typeRatios[1]);
        result.setEvasiveRatio(typeRatios[2]);
        result.setSmartRatio(typeRatios[3]);

        log.info("GameCore.updateGameParam: userId={}, mode={}, lastD={}, Dt={}, Vt={}, Nt={}, Lt={}, ratios=[{},{},{},{}], "
                        + "At={}, St={}, round={}, Re={}",
                performance.getUserId(),
                params.getGameMode(),
                lastDifficulty,
                currentDifficulty,
                newSpeed,
                newCount,
                newLength,
                typeRatios[0], typeRatios[1], typeRatios[2], typeRatios[3],
                performance.getAvgAccuracy(),
                performance.getAvgSpeed(),
                performance.getRoundNum(),
                errorCharRatio);
        return result;
    }

    /**
     * 生成首轮怪物（基于选中字符类型+易错字符）
     */
    public static List<MonsterData> generateFirstRoundMonsters(UserGameModeConfig modeConfig,
                                                               List<Character> errorChars,
                                                               BigDecimal initSpeed,
                                                               Integer initCount,
                                                               Integer avgLength) {
        List<CharType> types = new ArrayList<>();
        List<Character> charPool = buildCharPool(types);

        if (errorChars != null && !errorChars.isEmpty() && !charPool.isEmpty()) {
            int limit = Math.max(1, charPool.size() * 3 / 10);
            charPool.addAll(errorChars.stream()
                    .limit(limit)
                    .toList());
        }

        List<MonsterData> monsters = new ArrayList<>();
        Random random = new Random();
        int count = initCount != null ? initCount : 5;
        int length = avgLength != null ? avgLength : 3;

        for (int i = 0; i < count; i++) {
            StringBuilder sb = new StringBuilder();
            for (int j = 0; j < length; j++) {
                sb.append(charPool.get(random.nextInt(charPool.size())));
            }

            MonsterData monster = new MonsterData();
            monster.setTargetStr(sb.toString());
            monster.setSpeed(initSpeed != null ? initSpeed : new BigDecimal("1.0"));
            monster.setSurvivalTime(0);
            monster.setInputErrorCount(0);
            monsters.add(monster);
        }
        return monsters;
    }

    /**
     * 辅助方法：构建字符池（基于选中的字符类型）
     */
    private static List<Character> buildCharPool(List<CharType> selectedTypes) {
        List<Character> charPool = new ArrayList<>();
        for (CharType type : selectedTypes) {
            switch (type) {
                case lower:
                    for (char c = 'a'; c <= 'z'; c++) charPool.add(c);
                    break;
                case upper:
                    for (char c = 'A'; c <= 'Z'; c++) charPool.add(c);
                    break;
                case number:
                    for (char c = '0'; c <= '9'; c++) charPool.add(c);
                    break;
                case punct:
                    char[] punctuations = {',', '.', '!', '?', '"', '\'', '(', ')', ';', ':'};
                    for (char c : punctuations) charPool.add(c);
                    break;
                case chinese:
                    char[] chineseChars = {'我', '你', '他', '人', '工', '程', '技', '术', '游', '戏'};
                    for (char c : chineseChars) charPool.add(c);
                    break;
                case operator:
                    char[] operators = {'+', '-', '×', '÷', '=', '>', '<', '≥', '≤', '%', '*', '/'};
                    for (char c : operators) charPool.add(c);
                    break;
                case special:
                    char[] specialChars = {'@', '#', '$', '%', '^', '&', '_', '￥', '￡', '☆'};
                    for (char c : specialChars) charPool.add(c);
                    break;
            }
        }
        return charPool;
    }

    /**
     * 辅助方法：按占比分配怪物类型
     */
    private static MonsterType assignMonsterType(int normal, int attack, int evasive, int smart) {
        int random = new Random().nextInt(100);
        if (random < normal) {
            return MonsterType.NORMAL;
        } else if (random < normal + attack) {
            return MonsterType.ATTACKER;
        } else if (random < normal + attack + evasive) {
            return MonsterType.EVASIVE;
        } else {
            return MonsterType.SMART;
        }
    }
}
