package com.typinggame.game.service.word;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WordDrawTest {
    @Test
    void dropsInactiveDictionariesAndPrefersHeavierWords() {
        assertEquals(List.of(2), WordDraw.activeOnly(List.of(1, 2, 3), List.of(2, 9)));
        List<String> first = WordDraw.draw(List.of(new WordDraw.Entry("easy", 1), new WordDraw.Entry("hard", 5)), 3, new Random(4));
        List<String> again = WordDraw.draw(List.of(new WordDraw.Entry("easy", 1), new WordDraw.Entry("hard", 5)), 3, new Random(4));
        assertEquals(again, first);
        assertTrue(first.contains("hard"));
        assertEquals(3, first.size());
    }
}
