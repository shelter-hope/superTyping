package com.typinggame.chat.dictionary;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DictionaryTextParserTest {
    @Test
    void parsesCsvAndRejectsOtherTypes() {
        List<String> words = DictionaryTextParser.parse("words.csv", "word,difficulty\ncat,1\ntree,2\n");
        assertEquals(List.of("cat", "tree"), words);
        assertThrows(IllegalArgumentException.class, () -> DictionaryTextParser.parse("notes.md", "cat"));
    }

    @Test
    void splitsPlainTextOnWhitespace() {
        assertEquals(List.of("alpha", "beta"), DictionaryTextParser.parse("words.txt", "alpha\nbeta alpha"));
    }
}
