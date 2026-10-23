package com.typinggame.game.rule;

import lombok.Data;

@Data
public class DifficultyRule {
    private double weightAccuracy = 0.45;
    private double weightNormalizedSpeed = 0.20;
    private double weightHistorySpeed = 0.10;
    private double weightComposure = 0.10;
    private double weightRound = 0.15;
    private double targetAccuracy = 0.85;
    private double gain = 0.35;
    private double step = 0.05;
    private double minDifficulty = 0.15;
    private double maxDifficulty = 0.95;
    private double initialDifficulty = 0.30;
    private double maxReferenceSpeed = 150;
    private double refPeakSpeed = 120;
    private double weakCharDivisor = 10;
    private double speedSpan = 1.8;
    private double countBase = 4;
    private double countRoundFactor = 0.8;
    private double countDifficultyFactor = 12;
    private int countMin = 3;
    private int countMax = 25;
    private int normalBase = 80;
    private double normalSlope = 0.30;
    private int normalMin = 50;
    private int normalMax = 80;
    private int attackBase = 5;
    private double attackSlope = 0.10;
    private int attackMin = 5;
    private int attackMax = 15;
    private int evasiveBase = 10;
    private double evasiveSlope = 0.10;
    private int evasiveMin = 10;
    private int evasiveMax = 20;
    private int roundLogBase = 32;
}
