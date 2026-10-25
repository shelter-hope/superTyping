package com.typinggame.game.word;

import com.typinggame.entity.dictionaries.CustomDictionary;
import com.typinggame.entity.dictionaries.SystemDictionary;
import com.typinggame.game.repository.GameCustomDictionaryRepository;
import com.typinggame.game.repository.GameDictionaryWordRepository;
import com.typinggame.game.repository.GameSystemDictionaryRepository;
import com.typinggame.game.repository.GameSystemDictionaryWordRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class JpaWordSource implements WordSource {
    private final GameCustomDictionaryRepository customDictionaryRepository;
    private final GameDictionaryWordRepository dictionaryWordRepository;
    private final GameSystemDictionaryWordRepository systemDictionaryWordRepository;
    private final GameSystemDictionaryRepository systemDictionaryRepository;

    public JpaWordSource(GameCustomDictionaryRepository customDictionaryRepository,
                         GameDictionaryWordRepository dictionaryWordRepository,
                         GameSystemDictionaryWordRepository systemDictionaryWordRepository,
                         GameSystemDictionaryRepository systemDictionaryRepository) {
        this.customDictionaryRepository = customDictionaryRepository;
        this.dictionaryWordRepository = dictionaryWordRepository;
        this.systemDictionaryWordRepository = systemDictionaryWordRepository;
        this.systemDictionaryRepository = systemDictionaryRepository;
    }

    @Override
    public List<Integer> allowedCustomDictIds(Collection<Integer> dictIds, Integer userId) {
        if (dictIds == null || dictIds.isEmpty()) {
            return List.of();
        }
        return customDictionaryRepository.findByDictIdInAndUserId(dictIds, userId).stream()
                .map(CustomDictionary::getDictId)
                .collect(Collectors.toList());
    }

    @Override
    public List<Integer> activeSystemDictIds(Collection<Integer> dictIds) {
        if (dictIds == null || dictIds.isEmpty()) {
            return List.of();
        }
        return systemDictionaryRepository.findByDictIdInAndIsActiveTrue(dictIds).stream()
                .map(SystemDictionary::getDictId)
                .collect(Collectors.toList());
    }

    @Override
    public List<WordEntry> customWords(Collection<Integer> dictIds) {
        if (dictIds == null || dictIds.isEmpty()) {
            return List.of();
        }
        return dictionaryWordRepository.findByDictIdIn(dictIds, page()).stream()
                .map(word -> new WordEntry(word.getWord(), word.getDifficultyLevel()))
                .collect(Collectors.toList());
    }

    @Override
    public List<WordEntry> systemWords(Collection<Integer> dictIds) {
        if (dictIds == null || dictIds.isEmpty()) {
            return List.of();
        }
        return systemDictionaryWordRepository.findByDictIdIn(dictIds, page()).stream()
                .map(word -> new WordEntry(word.getWord(), word.getDifficultyLevel()))
                .collect(Collectors.toList());
    }

    private PageRequest page() {
        return PageRequest.of(0, 500);
    }
}
