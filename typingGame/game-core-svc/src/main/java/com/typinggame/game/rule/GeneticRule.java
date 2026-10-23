package com.typinggame.game.rule;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class GeneticRule {
    private int inRoundParentLimit = 10;
    private int crossRoundParentLimit = 8;
    private double inRoundMutationRate = 0.15;
    private double crossRoundMutationRate = 0.25;
    private String crossover = "SINGLE_POINT";
    private boolean preferWeakChars = true;
    private double fitnessScoreWeight = 1;
    private double fitnessAccuracyWeight = 100;
    private double fitnessErrorPenalty = 2;
    private List<String> charPool = new ArrayList<>();
}
