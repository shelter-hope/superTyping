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
        double partial = DifficultyEngine.nextDifficulty(rule, 0.30, 1.0, 150.0, 8, 0);
        assertEquals(0.3333, partial, 0.001);
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
    void rejectsMutationRateOutsideUnitInterval() {
        GeneticRule rule = new GeneticRule();
        rule.setInRoundMutationRate(1.5);
        assertThrows(IllegalArgumentException.class, () -> RuleValidator.validateGenetic(rule));
    }
}
