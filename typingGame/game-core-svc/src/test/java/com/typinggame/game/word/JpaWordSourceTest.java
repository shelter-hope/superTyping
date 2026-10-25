package com.typinggame.game.word;

import com.typinggame.entity.dictionaries.CustomDictionary;
import com.typinggame.entity.dictionaries.DictionaryWord;
import com.typinggame.entity.dictionaries.SystemDictionary;
import com.typinggame.entity.dictionaries.SystemDictionaryWord;
import com.typinggame.game.repository.GameCustomDictionaryRepository;
import com.typinggame.game.repository.GameDictionaryWordRepository;
import com.typinggame.game.repository.GameSystemDictionaryRepository;
import com.typinggame.game.repository.GameSystemDictionaryWordRepository;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class JpaWordSourceTest {

    @Test
    void loadsAllowedIdsAndWords() {
        GameCustomDictionaryRepository customDicts = mock(GameCustomDictionaryRepository.class);
        GameDictionaryWordRepository customWords = mock(GameDictionaryWordRepository.class);
        GameSystemDictionaryWordRepository systemWords = mock(GameSystemDictionaryWordRepository.class);
        GameSystemDictionaryRepository systemDicts = mock(GameSystemDictionaryRepository.class);
        CustomDictionary owned = new CustomDictionary();
        owned.setDictId(2);
        when(customDicts.findByDictIdInAndUserId(List.of(1, 2), 7)).thenReturn(List.of(owned));
        SystemDictionary active = new SystemDictionary();
        active.setDictId(9);
        when(systemDicts.findByDictIdInAndIsActiveTrue(List.of(8, 9))).thenReturn(List.of(active));
        DictionaryWord cw = new DictionaryWord();
        cw.setWord("hi");
        cw.setDifficultyLevel(2);
        when(customWords.findByDictIdIn(eq(List.of(2)), any(Pageable.class))).thenReturn(List.of(cw));
        SystemDictionaryWord sw = new SystemDictionaryWord();
        sw.setWord("ok");
        sw.setDifficultyLevel(1);
        when(systemWords.findByDictIdIn(eq(List.of(9)), any(Pageable.class))).thenReturn(List.of(sw));

        WordSource source = new JpaWordSource(customDicts, customWords, systemWords, systemDicts);

        assertEquals(List.of(2), source.allowedCustomDictIds(List.of(1, 2), 7));
        assertEquals(List.of(9), source.activeSystemDictIds(List.of(8, 9)));
        assertEquals("hi", source.customWords(List.of(2)).get(0).word());
        assertEquals("ok", source.systemWords(List.of(9)).get(0).word());
    }
}
