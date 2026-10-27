package com.typinggame.game.rule;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GeneticEngineTest {

    private static final List<String> MODES = List.of(
            GeneticEngine.CROSSOVER_SINGLE_POINT,
            GeneticEngine.CROSSOVER_TWO_POINT,
            GeneticEngine.CROSSOVER_UNIFORM);

    @Test
    void crossoverAlwaysReturnsTargetLength() {
        Random random = new Random(7);
        for (String mode : MODES) {
            for (int length = 1; length <= 12; length++) {
                String child = GeneticEngine.crossover("abcd", "wxyz", length, mode, random);
                assertEquals(length, child.length(), mode + " 长度应为目标长度");
            }
        }
        // 两个父本都为空时没有可继承的基因，保持为空（调用方负责过滤空父本）
        assertEquals("", GeneticEngine.crossover(null, "", 6, GeneticEngine.CROSSOVER_TWO_POINT, random));
        assertEquals(6, GeneticEngine.crossover("abcd", null, 6, GeneticEngine.CROSSOVER_TWO_POINT, random).length());
    }

    @Test
    void twoPointCrossoverCanTakeFromBothParents() {
        boolean sawMixedChild = false;
        for (int seed = 0; seed < 40 && !sawMixedChild; seed++) {
            String child = GeneticEngine.crossover("abcd", "wxyz", 4, GeneticEngine.CROSSOVER_TWO_POINT,
                    new Random(seed));
            boolean hasFirstParentChar = child.chars().anyMatch(ch -> ch >= 'a' && ch <= 'd');
            boolean hasSecondParentChar = child.chars().anyMatch(ch -> ch >= 'w' && ch <= 'z');
            sawMixedChild = hasFirstParentChar && hasSecondParentChar;
        }
        assertTrue(sawMixedChild, "双点交叉应该能同时用上两个父本");
    }

    @Test
    void mutationUsesTheConfusedCharacterWhenKnown() {
        GeneticEngine.Mutation mutation = GeneticEngine.mutate("iii", List.of("z"), Map.of("i", List.of("x")),
                1.0, new Random(3));
        assertEquals(1, mutation.positions().size());
        assertEquals(List.of("x"), mutation.chars());
        assertEquals(1, mutation.text().chars().filter(ch -> ch == 'x').count());
    }

    @Test
    void mutationFallsBackToTheAlphabetInsteadOfTheNextPosition() {
        // 没有弱项字符、没有混淆对时也应分散替换，而不是固定取「下一个字符」
        Set<String> replacements = new HashSet<>();
        for (int seed = 0; seed < 20; seed++) {
            replacements.add(GeneticEngine.mutate("aaaa", List.of(), Map.of(), 1.0, new Random(seed)).text());
        }
        assertTrue(replacements.size() > 1, "替换结果应该分散，实际只有 " + replacements.size() + " 种");
    }

    @Test
    void evolutionIsDeterministicForTheSameSeed() {
        assertEquals(evolve(new Random(42)), evolve(new Random(42)));
    }

    @Test
    void evolutionKeepsElitesAndStaysDeduped() {
        List<GeneticEngine.Parent> parents = new ArrayList<>();
        parents.add(new GeneticEngine.Parent("alpha", 10));
        parents.add(new GeneticEngine.Parent("bravo", 5));
        parents.add(new GeneticEngine.Parent("delta", 1));
        List<String> result = GeneticEngine.evolve(new GeneticEngine.Request(
                parents,
                List.of("e"),
                Map.of(),
                6,
                4,
                0.3,
                0.34,
                3,
                GeneticEngine.CROSSOVER_TWO_POINT,
                new Random(11)));
        assertEquals(6, result.size());
        // 精英保留：round(0.34 * 6) = 2，恰好是最优的两个父本被原样截断保留
        assertTrue(result.contains("alph"), "应保留最优父本，实际 " + result);
        assertTrue(result.contains("brav"), "应保留次优父本，实际 " + result);
        assertEquals(result.size(), new HashSet<>(result).size(), "子代不应重复");
        result.forEach(word -> assertEquals(4, word.length()));
    }

    @Test
    void mutationRateZeroLeavesTextUntouched() {
        GeneticEngine.Mutation mutation = GeneticEngine.mutate("abcd", List.of("x"), Map.of(), 0.0, new Random(1));
        assertEquals("abcd", mutation.text());
        assertTrue(mutation.positions().isEmpty());
    }

    private List<String> evolve(Random random) {
        List<GeneticEngine.Parent> parents = new ArrayList<>();
        parents.add(new GeneticEngine.Parent("alpha", 10));
        parents.add(new GeneticEngine.Parent("bravo", 5));
        parents.add(new GeneticEngine.Parent("delta", 1));
        return GeneticEngine.evolve(new GeneticEngine.Request(
                parents,
                List.of("e"),
                Map.of(),
                6,
                4,
                0.3,
                0.0,
                3,
                GeneticEngine.CROSSOVER_TWO_POINT,
                random));
    }

    @Test
    void legacyEvolveWrapperStillWorks() {
        List<String> result = GeneticEngine.evolve(List.of("abcd", "wxyz"), List.of("e"), 4, 4, 0.2, new Random(5));
        assertFalse(result.isEmpty());
        assertEquals(4, result.size());
        result.forEach(word -> assertEquals(4, word.length()));
    }
}
