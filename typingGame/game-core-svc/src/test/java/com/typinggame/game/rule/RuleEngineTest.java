package com.typinggame.game.rule;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RuleEngineTest {
    @Test
    void difficultyMovesTowardTargetAndStaysInsideBounds() {
        DifficultyRule rule = new DifficultyRule();
        double high = DifficultyEngine.nextDifficulty(rule, 0.30, 1.0, 150.0, 31, 0);
        double low = DifficultyEngine.nextDifficulty(rule, 0.30, 0.2, 10.0, 1, 10);
        assertEquals(0.35, high, 0.0001);
        assertTrue(low < 0.30);
        assertTrue(low >= rule.getMinDifficulty());
        double partial = DifficultyEngine.nextDifficulty(rule, 0.30, 0.95, 40.0, 1, 10);
        assertEquals(0.335, partial, 0.0001);
        assertEquals(0.30, DifficultyEngine.nextDifficulty(rule, 0.30, 0.85, 200.0, 31, 10), 0.0001);
        assertEquals(0.30, DifficultyEngine.nextDifficulty(rule, 0.30, null, 0.0, 1, 0), 0.0001);
        assertEquals(0.25, low, 0.0001);
    }

    @Test
    void percentAccuracyIsNormalizedBeforeTheStep() {
        DifficultyRule rule = new DifficultyRule();
        double fromPercent = DifficultyEngine.nextDifficulty(rule, 0.30, 100.0, 0.0, 1, 0);
        double fromRatio = DifficultyEngine.nextDifficulty(rule, 0.30, 1.0, 0.0, 1, 0);
        assertEquals(fromRatio, fromPercent, 0.0001);
    }

    @Test
    void geneticCrossoverAndMutationAreStableForASeed() {
        Random random = new Random(7);
        String child = GeneticEngine.crossover("abcd", "wxyz", 4, random);
        GeneticEngine.Mutation mutation = GeneticEngine.mutate(child, List.of("q"), 1.0, true, new Random(7));
        assertEquals(4, child.length());
        assertTrue(mutation.text().contains("q"));
        List<String> evolved = GeneticEngine.evolve(List.of("abcd", "wxyz"), List.of("q"), 3, 4, 0.0, new Random(7));
        List<String> again = GeneticEngine.evolve(List.of("abcd", "wxyz"), List.of("q"), 3, 4, 0.0, new Random(7));
        assertEquals(again, evolved);
    }

    @Test
    void rejectsWeightsThatDoNotSumToOne() {
        DifficultyRule rule = new DifficultyRule();
        rule.setWeightAccuracy(0.9);
        assertThrows(IllegalArgumentException.class, () -> RuleValidator.validateDifficulty(rule));
    }

    @Test
    void rejectsInvalidTriggerThresholdOrder() {
        DifficultyRule rule = new DifficultyRule();
        rule.setHighAccuracyAdvanceThreshold(40);
        rule.setLowAccuracyRollbackThreshold(80);
        assertThrows(IllegalArgumentException.class, () -> RuleValidator.validateDifficulty(rule));
    }

    @Test
    void acceptsFullDefaultDifficultyRule() {
        RuleValidator.validateDifficulty(new DifficultyRule());
    }

    @Test
    void defaultMapsTrackAccuracyAndCoverTheOutputRange() {
        DifficultyRule rule = new DifficultyRule();
        assertEquals(3, DifficultyEngine.monsterCount(rule, 0.15, 31));
        assertEquals(6, DifficultyEngine.monsterCount(rule, 0.30, 1));
        assertEquals(25, DifficultyEngine.monsterCount(rule, 0.95, 31));
        DifficultyEngine.Outcome easy = DifficultyEngine.outcome(rule, 0.15, 0.8, 3.0, 1, 15);
        DifficultyEngine.Outcome hard = DifficultyEngine.outcome(rule, 0.95, 0.8, 3.0, 1, 15);
        assertEquals(0.8, easy.speed(), 0.0001);
        assertEquals(1, easy.length());
        assertEquals(3.0, hard.speed(), 0.0001);
        assertEquals(15, hard.length());
        assertEquals(100, hard.normalRatio() + hard.attackRatio() + hard.evasiveRatio() + hard.smartRatio());
        assertEquals(1.0, DifficultyEngine.weakExposureMultiplier(rule, 0), 0.0001);
        assertEquals(1.5, DifficultyEngine.weakExposureMultiplier(rule, 5), 0.0001);
        assertEquals(2.0, DifficultyEngine.weakExposureMultiplier(rule, 10), 0.0001);
        assertEquals(0, DifficultyEngine.pickIndex(new double[]{2, 1}, 0.66));
        assertEquals(1, DifficultyEngine.pickIndex(new double[]{2, 1}, 0.67));
        assertEquals(2, DifficultyEngine.exposureWeight("alpha", java.util.List.of("a"), 1, 2.0));
        assertEquals(1, DifficultyEngine.exposureWeight("xyz", java.util.List.of("a"), 1, 2.0));
    }

    @Test
    void speedWeightIsCenteredOnTheReferenceSpeed() {
        DifficultyRule rule = new DifficultyRule();
        rule.setWeightAccuracy(0.0);
        rule.setWeightNormalizedSpeed(1.0);
        double atReference = DifficultyEngine.nextDifficulty(rule, 0.30, 0.20, 120.0, 1, 0);
        double faster = DifficultyEngine.nextDifficulty(rule, 0.30, 0.20, 150.0, 1, 0);
        assertEquals(0.30, atReference, 0.0001);
        assertTrue(faster > 0.30);
    }

    @Test
    void monsterCountRespectsConfiguredBounds() {
        DifficultyRule rule = new DifficultyRule();
        rule.setCountMin(5);
        rule.setCountMax(8);
        rule.setCountBase(100);
        assertEquals(8, DifficultyEngine.monsterCount(rule, 1.0, 50));
    }

    @Test
    void rejectsMutationRateOutsideUnitInterval() {
        GeneticRule rule = new GeneticRule();
        rule.setInRoundMutationRate(1.5);
        assertThrows(IllegalArgumentException.class, () -> RuleValidator.validateGenetic(rule));
    }
}
