package com.typinggame.game.service.rule;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class GeneticRule {
    /** 局内（本轮怪物之间）参与交叉的父本上限。 */
    private int inRoundParentLimit = 10;
    /** 跨局参与交叉的历史字符串上限。 */
    private int crossRoundParentLimit = 8;
    private double inRoundMutationRate = 0.15;
    private double crossRoundMutationRate = 0.25;
    /** SINGLE_POINT / TWO_POINT / UNIFORM，见 GeneticEngine。 */
    private String crossover = "TWO_POINT";
    private boolean preferWeakChars = true;
    /** 精英保留比例：每代按适应度原样保留的比例，避免拆掉最优解。 */
    private double eliteRatio = 0.1;
    /** 锦标赛规模：从父本池里随机抽几个取最好的那个。 */
    private int tournamentSize = 3;
    private double fitnessScoreWeight = 1;
    private double fitnessAccuracyWeight = 100;
    private double fitnessErrorPenalty = 2;
    private List<String> charPool = new ArrayList<>();
}
